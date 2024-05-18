package client.teavm.wasm;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static libsidplay.common.SIDEmu.NONE;
import static libsidplay.sidtune.SidTune.RESET;
import static libsidutils.CBMCodeUtils.petsciiToScreenRam;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import javax.sound.sampled.LineUnavailableException;

import org.teavm.interop.Export;

import builder.resid.ReSIDBuilder;
import client.teavm.common.AudioDriverTeaVM;
import client.teavm.common.config.ConfigurationTeaVM;
import client.teavm.compiletime.RomsTeaVM;
import client.teavm.wasm.audio.PalEmulationABGRTeaVM;
import client.teavm.wasm.audio.WebAssemblyAudioDriver;
import client.teavm.wasm.config.WebAssemblyConfigResolver;
import libsidplay.C64;
import libsidplay.HardwareEnsemble;
import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.components.c1530.Datasette.Control;
import libsidplay.components.c1541.DiskImage;
import libsidplay.components.cart.CartridgeType;
import libsidplay.components.keyboard.KeyTableEntry;
import libsidplay.components.mos6510.MOS6510;
import libsidplay.components.mos656x.PALEmulation;
import libsidplay.config.IAudioSection;
import libsidplay.config.IC1541Section;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;
import libsidplay.config.ISidPlay2Section;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidplay.sidtune.SidTuneType;
import libsidutils.IOUtils;
import sidplay.player.PSid64DetectedTuneInfo;
import sidplay.player.PSid64Detection;

/**
 * TeaVM version of JSIDPlay2 to generate web assembly code.
 */
public class JSIDPlay2TeaVM {

	private static final Logger LOG = Logger.getLogger(JSIDPlay2TeaVM.class.getName());

	private static final int RAM_COMMAND = 0x277;
	private static final int RAM_COMMAND_LEN = 0xc6;
	private static final int MAX_COMMAND_LEN = 16;
	private static final int RAM_COMMAND_SCREEN_ADDRESS = 1024 + 6 * 40 + 1;
	private static final String RUN = "RUN\r", SYS = "SYS%d\r", LOAD = "LOAD\r";

	private static IConfig config;
	private static EventScheduler context;
	private static SidTune tune;
	private static HardwareEnsemble hardwareEnsemble;
	private static C64 c64;
	private static ReSIDBuilder sidBuilder;
	private static String command;
	private static int bufferSize;

	//
	// Exports to JavaScript
	//

	@Export(name = "open")
	public static void open(byte[] sidContents, String sidContentsName, int song, int nthFrame, boolean addSidListener,
			byte[] cartContents, String cartContentsName)
			throws IOException, SidTuneError, LineUnavailableException, InterruptedException {
		config = new ConfigurationTeaVM(new WebAssemblyConfigResolver());
		final ISidPlay2Section sidplay2Section = config.getSidplay2Section();
		final IAudioSection audioSection = config.getAudioSection();
		final IEmulationSection emulationSection = config.getEmulationSection();
		final IC1541Section c1541Section = config.getC1541Section();

		doLog(sidplay2Section, audioSection, emulationSection, c1541Section);

		if (sidContents != null) {
			String url = jsStringToJavaString(sidContentsName);
			LOG.finest("Load Tune, length=" + sidContents.length);
			LOG.finest("Tune name: " + url);
			LOG.finest("Song: " + song);
			tune = SidTune.load(url, new ByteArrayInputStream(sidContents), SidTuneType.get(url));
			tune.getInfo().setSelectedSong(song == 0 ? null : song);
		} else {
			LOG.finest("RESET");
			tune = RESET;
		}
		LOG.finest("nthFrame: " + nthFrame);
		LOG.finest("addSidListener: " + addSidListener);
		String cartContentsUrl = jsStringToJavaString(cartContentsName);
		if (cartContentsUrl != null) {
			LOG.finest("Cart, length=: " + cartContents.length);
			LOG.finest("Cart name: : " + cartContentsUrl);
		}
		Map<String, String> allRoms = RomsTeaVM.getJavaScriptRoms(false);
		Decoder decoder = Base64.getDecoder();
		byte[] charRom = decoder.decode(allRoms.get(RomsTeaVM.CHAR_ROM));
		byte[] basicRom = decoder.decode(allRoms.get(RomsTeaVM.BASIC_ROM));
		byte[] kernalRom = decoder.decode(allRoms.get(RomsTeaVM.KERNAL_ROM));
		byte[] c1541Rom = decoder.decode(allRoms.get(RomsTeaVM.C1541_ROM));
		byte[] psidDriverBin = decoder.decode(allRoms.get(RomsTeaVM.PSID_DRIVER_ROM));
		byte[] jiffyDosC64Rom = decoder.decode(allRoms.get(RomsTeaVM.JIFFYDOS_C64_ROM));
		byte[] jiffyDosC1541Rom = decoder.decode(allRoms.get(RomsTeaVM.JIFFYDOS_C1541_ROM));

		hardwareEnsemble = new HardwareEnsemble(config, context -> new MOS6510(context), charRom, basicRom, kernalRom,
				jiffyDosC64Rom, jiffyDosC1541Rom, c1541Rom, new byte[0], new byte[0]);
		hardwareEnsemble.setClock(CPUClock.getCPUClock(emulationSection, tune));
		c64 = hardwareEnsemble.getC64();
		c64.getVIC().setPalEmulation(nthFrame > 0 ? new PalEmulationABGRTeaVM(nthFrame, decoder) : PALEmulation.NONE);
		if (cartContents != null) {
			try {
				File cartFile = createReadOnlyFile(cartContents, cartContentsUrl);
				c64.setCartridge(CartridgeType.CRT, cartFile);
				LOG.fine("Cartridge: image attached: " + cartContentsUrl);
			} catch (IOException e) {
				System.err.println(e.getMessage());
				System.err.println(String.format("Cannot insert media file '%s'.", cartContentsUrl));
			}
		}
		hardwareEnsemble.reset();
		emulationSection.getOverrideSection().reset();
		sidBuilder = new ReSIDBuilder(c64.getEventScheduler(), config, c64.getClock(), c64.getCartridge());
		c64.getEventScheduler().schedule(Event.of("Auto-start", event -> {
			if (tune != RESET) {
				// for tunes: Install player into RAM
				Integer driverAddress = tune.placeProgramInMemoryTeaVM(c64.getRAM(), psidDriverBin);
				if (driverAddress != null) {
					// Set play address to feedback call frames counter.
					c64.setPlayAddr(tune.getInfo().getPlayAddr());
					// Start SID player driver
					c64.getCPU().forcedJump(driverAddress);
				} else {
					// No player: Start basic program or assembler code
					final int loadAddr = tune.getInfo().getLoadAddr();
					command = loadAddr == 0x0801 ? RUN : String.format(SYS, loadAddr);
				}
			}
			if (command != null) {
				if (command.startsWith(LOAD)) {
					// Load from tape needs someone to press play
					hardwareEnsemble.getDatasette().control(Control.START);
				}
				// Enter basic command
				typeInCommand(command);
			}
			c64.getEventScheduler().schedule(Event.of("PSID64 Detection", event2 -> autodetectPSID64()),
					(long) (c64.getClock().getCpuFrequency()));
		}), SidTune.getInitDelay(tune));

		AudioDriverTeaVM audioDriver = new AudioDriverTeaVM(new WebAssemblyAudioDriver(), nthFrame);
		audioDriver.open(audioSection, null, c64.getClock(), c64.getEventScheduler());
		sidBuilder.setAudioDriver(audioDriver);

		c64.insertSIDChips((sidNum, sidEmu) -> {
			if (SidTune.isSIDUsed(emulationSection, tune, sidNum)) {
				return sidBuilder.lock(sidEmu, sidNum, tune);
			} else if (sidEmu != NONE) {
				sidBuilder.unlock(sidEmu);
			}
			return NONE;
		}, sidNum -> SidTune.getSIDAddress(emulationSection, tune, sidNum));
		if (nthFrame > 0) {
			c64.configureVICs(vic -> vic.setVideoDriver(audioDriver));
		}
		if (addSidListener) {
			c64.setSIDListener(audioDriver);
		}
		sidBuilder.start();
		bufferSize = audioSection.getBufferSize();
		context = c64.getEventScheduler();
	}

	@Export(name = "setCommand")
	public static void typeInCommand(final String nameFromJS) {
		String multiLineCommand = jsStringToJavaString(nameFromJS);
		String command;
		if (multiLineCommand.length() > MAX_COMMAND_LEN) {
			String[] lines = multiLineCommand.split("\r");
			for (String line : lines) {
				byte[] screenRam = petsciiToScreenRam(line);
				System.arraycopy(screenRam, 0, c64.getRAM(), RAM_COMMAND_SCREEN_ADDRESS, screenRam.length);
				break;
			}
			int indexOf = multiLineCommand.indexOf('\r');
			command = indexOf != -1 ? multiLineCommand.substring(indexOf) : "\r";
		} else {
			command = multiLineCommand;
		}
		final int length = Math.min(command.length(), MAX_COMMAND_LEN);
		System.arraycopy(command.getBytes(US_ASCII), 0, c64.getRAM(), RAM_COMMAND, length);
		c64.getRAM()[RAM_COMMAND_LEN] = (byte) length;
	}

	@Export(name = "clock")
	public static void clock() throws InterruptedException {
		for (int i = 0; i < bufferSize; i++) {
			context.clock();
		}
	}

	@Export(name = "insertDisk")
	private static void insertDisk(byte[] diskContents, String diskContentsName) {
		String diskContentsUrl = jsStringToJavaString(diskContentsName);
		try {
			File d64File = createReadOnlyFile(diskContents, diskContentsUrl);
			config.getC1541Section().setDriveOn(true);
			hardwareEnsemble.enableFloppyDiskDrives(true);
			// attach selected disk into the first disk drive
			DiskImage disk = hardwareEnsemble.getFloppies()[0].getDiskController().insertDisk(d64File);
			disk.setExtendImagePolicy(() -> true);
			installHack(d64File);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.err.println(String.format("Cannot insert media file '%s'.", diskContentsUrl));
		}
	}

	@Export(name = "ejectDisk")
	private static void ejectDisk() {
		try {
			hardwareEnsemble.getFloppies()[0].getDiskController().ejectDisk();
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.err.println("Cannot eject disk.");
		}
	}

	@Export(name = "insertTape")
	public static void insertTape(byte[] tapeContents, String tapeContentsName) {
		String tapeContentsUrl = jsStringToJavaString(tapeContentsName);
		try {
			File tapeFile = createReadOnlyFile(tapeContents, tapeContentsUrl);
			hardwareEnsemble.getDatasette().insertTape(tapeFile);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.err.println(String.format("Cannot insert media file '%s'.", tapeContentsUrl));
		}
	}

	@Export(name = "ejectTape")
	private static void ejectTape() {
		try {
			hardwareEnsemble.getDatasette().ejectTape();
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.err.println("Cannot eject tape.");
		}
	}

	@Export(name = "pressPlayOnTape")
	public static void pressPlayOnTape() {
		hardwareEnsemble.getDatasette().control(Control.START);
	}

	@Export(name = "typeKey")
	public static void typeKey(final String keyCode) {
		String keyCodeStr = jsStringToJavaString(keyCode);
		LOG.fine("keyCodeStr: " + keyCodeStr);
		KeyTableEntry key = KeyTableEntry.valueOf(keyCodeStr);
		LOG.fine("typeKey: " + key);
		c64.getKeyboard().keyPressed(key);

		c64.getEventScheduler().schedule(Event.of("Wait Until Virtual Keyboard Released", event2 -> {

			c64.getEventScheduler().scheduleThreadSafeKeyEvent(
					Event.of("Virtual Keyboard Released", event3 -> c64.getKeyboard().keyReleased(key)));

		}), c64.getClock().getCyclesPerFrame() << 2);
	}

	@Export(name = "delaySidBlaster")
	public static void delaySidBlaster(int cycles) {
		// some hackery for SIDBlaster USB to support delayed writes
		long delay = (long) (cycles / CPUClock.PAL.getCpuFrequency() * 1000000000L);
		long startTime = System.nanoTime();
		while (System.nanoTime() - startTime < delay)
			;
	}

	//
	// Private methods
	//

	/**
	 * JavaScript string cannot be used directly for some reason, therefore:
	 */
	private static String jsStringToJavaString(String stringFromJS) {
		if (stringFromJS != null) {
			return new StringBuilder(stringFromJS).toString();
		}
		return null;
	}

	private static void doLog(ISidPlay2Section sidplay2Section, IAudioSection audioSection,
			IEmulationSection emulationSection, IC1541Section c1541Section) {
		LOG.finest("palEmulation: " + sidplay2Section.isPalEmulation());
		LOG.finest("defaultClockSpeed: " + emulationSection.getDefaultClockSpeed());
		LOG.finest("defaultEmulation: " + emulationSection.getDefaultEmulation());
		LOG.finest("defaultSidModel: " + emulationSection.getDefaultSidModel());
		LOG.finest("sampling: " + audioSection.getSampling());
		LOG.finest("samplingRate: " + audioSection.getSamplingRate());
		LOG.finest("reverbBypass: " + audioSection.getReverbBypass());
		LOG.finest("bufferSize: " + audioSection.getBufferSize());
		LOG.finest("audioBufferSize: " + audioSection.getAudioBufferSize());
		LOG.finest("isJiffyDosInstalled: " + c1541Section.isJiffyDosInstalled());
	}

	private static void autodetectPSID64() {
		IEmulationSection emulationSection = config.getEmulationSection();

		if (emulationSection.isDetectPSID64ChipModel()) {
			PSid64DetectedTuneInfo psid64TuneInfo = PSid64Detection.detectPSid64TuneInfo(c64.getRAM(),
					c64.getVicMemBase() + c64.getVIC().getVideoMatrixBase());
			if (psid64TuneInfo.isDetected()) {

				boolean update = false;
				if (psid64TuneInfo.hasDifferentUserChipModel(ChipModel.getChipModel(emulationSection, tune, 0))) {
					emulationSection.getOverrideSection().getSidModel()[0] = psid64TuneInfo.getUserChipModel();
					update = true;
				}
				if (psid64TuneInfo.hasDifferentStereoChipModel(ChipModel.getChipModel(emulationSection, tune, 1))) {
					emulationSection.getOverrideSection().getSidModel()[1] = psid64TuneInfo.getStereoChipModel();
					update = true;
				}
				if (psid64TuneInfo.hasDifferentStereoAddress(SidTune.getSIDAddress(emulationSection, tune, 1))) {
					emulationSection.getOverrideSection().getSidBase()[1] = psid64TuneInfo.getStereoAddress();
					update = true;
				}
				if (update) {
					c64.insertSIDChips((sidNum, sidEmu) -> {
						if (SidTune.isSIDUsed(config.getEmulationSection(), tune, sidNum)) {
							return sidBuilder.lock(sidEmu, sidNum, tune);
						} else if (sidEmu != NONE) {
							sidBuilder.unlock(sidEmu);
						}
						return NONE;
					}, sidNum -> SidTune.getSIDAddress(config.getEmulationSection(), tune, sidNum));
				}
			}
		}
	}

	private static void installHack(File d64File) {
		c64.getCPU().setEODHack(d64File.getName().toLowerCase(Locale.US).contains("disgrace"));
	}

	private static File createReadOnlyFile(byte[] fileContents, String fileContentsUrl)
			throws IOException, FileNotFoundException {
		File tmp = File.createTempFile(IOUtils.getFilenameWithoutSuffix(fileContentsUrl),
				IOUtils.getFilenameSuffix(fileContentsUrl));
		try (OutputStream os = new FileOutputStream(tmp)) {
			os.write(fileContents);
		}
		tmp.setWritable(false);
		return tmp;
	}

	public static void main(String[] args) throws Exception {
	}
}
