package client.teavm.common;

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

import builder.resid.ReSIDBuilder;
import client.teavm.common.audio.AudioDriverTeaVM;
import client.teavm.common.config.ConfigurationTeaVM;
import client.teavm.common.video.PALEmulationTeaVM;
import client.teavm.compiletime.RomsTeaVM;
import libsidplay.C64;
import libsidplay.HardwareEnsemble;
import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Engine;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.components.c1530.Datasette.Control;
import libsidplay.components.c1541.DiskImage;
import libsidplay.components.cart.CartridgeType;
import libsidplay.components.keyboard.KeyTableEntry;
import libsidplay.components.mos6510.MOS6510;
import libsidplay.components.pla.PLA;
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

public class ExportedApi implements IExportedApi {

	private static final Logger LOG = Logger.getLogger(ExportedApi.class.getName());

	private static final int RAM_COMMAND = 0x277;
	private static final int RAM_COMMAND_LEN = 0xc6;
	private static final int MAX_COMMAND_LEN = 16;
	private static final int RAM_COMMAND_SCREEN_ADDRESS = 1024 + 6 * 40 + 1;
	private static final String RUN = "RUN\r", SYS = "SYS%d\r", LOAD = "LOAD\r";

	private final IImportedApi importedApi;

	private IConfig config;
	private EventScheduler context;
	private SidTune tune;
	private HardwareEnsemble hardwareEnsemble;
	private C64 c64;
	private ReSIDBuilder sidBuilder;
	private String command;
	private int bufferSize;

	public ExportedApi(IImportedApi importedApi) {
		this.importedApi = importedApi;
		config = new ConfigurationTeaVM(importedApi);
	}

	@Override
	public void open(byte[] sidContents, String sidContentsNameFromJS, int song, int nthFrame, boolean addSidListener,
			byte[] cartContents, String cartContentsNameFromJS, String commandFromJS)
			throws IOException, SidTuneError, LineUnavailableException, InterruptedException {
		// JavaScript string cannot be used directly for some reason, therefore:
		String sidContentsName = sidContentsNameFromJS != null ? "" + sidContentsNameFromJS : null;
		String cartContentsName = cartContentsNameFromJS != null ? "" + cartContentsNameFromJS : null;
		command = commandFromJS != null ? "" + commandFromJS : null;

		final ISidPlay2Section sidplay2Section = config.getSidplay2Section();
		final IAudioSection audioSection = config.getAudioSection();
		final IEmulationSection emulationSection = config.getEmulationSection();
		final IC1541Section c1541Section = config.getC1541Section();

		doLog(sidplay2Section, audioSection, emulationSection, c1541Section);

		if (sidContents != null) {
			LOG.finest("Load Tune, length=" + sidContents.length);
			LOG.finest("Tune name: " + sidContentsName);
			LOG.finest("Song: " + song);
			tune = SidTune.load(sidContentsName, new ByteArrayInputStream(sidContents),
					SidTuneType.get(sidContentsName));
			tune.getInfo().setSelectedSong(song == 0 ? null : song);
		} else {
			LOG.finest("RESET");
			tune = RESET;
		}
		LOG.finest("nthFrame: " + nthFrame);
		LOG.finest("addSidListener: " + addSidListener);
		if (cartContentsName != null) {
			LOG.finest("Cart, length=: " + cartContents.length);
			LOG.finest("Cart name: : " + cartContentsName);
		}
		for (int sidNum = 0; sidNum < PLA.MAX_SIDS; sidNum++) {
			if (SidTune.isSIDUsed(emulationSection, tune, sidNum)) {
				Engine engine = Engine.getEngine(emulationSection, tune);
				Emulation emulation = Emulation.getEmulation(emulationSection, sidNum);
				ChipModel chipModel = ChipModel.getChipModel(emulationSection, tune, sidNum);
				String filterName = emulationSection.getFilterName(sidNum, engine, emulation, chipModel);
				LOG.finest(getFilterName(sidNum) + ": " + filterName);
			}
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
		PALEmulationTeaVM palEmulationTeaVM = nthFrame > 0 ? new PALEmulationTeaVM(nthFrame) : null;
		c64.getVIC().setPalEmulation(palEmulationTeaVM);
		if (cartContents != null) {
			try {
				File cartFile = createReadOnlyFile(cartContents, cartContentsName);
				c64.setCartridge(CartridgeType.CRT, cartFile);
				LOG.fine("Cartridge: image attached: " + cartContentsName);
			} catch (IOException e) {
				System.err.println(e.getMessage());
				System.err.println(String.format("Cannot insert media file '%s'.", cartContentsName));
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

		AudioDriverTeaVM audioDriver = new AudioDriverTeaVM(importedApi, palEmulationTeaVM);
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

	@Override
	public void typeInCommand(String multiLineCommandFromJS) {
		// JavaScript string cannot be used directly for some reason, therefore:
		String multiLineCommand = multiLineCommandFromJS != null ? "" + multiLineCommandFromJS : null;

		if (isOpen()) {
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
	}

	@Override
	public void clock() throws InterruptedException {
		for (int i = 0; i < bufferSize; i++) {
			context.clock();
		}
	}

	@Override
	public void insertDisk(byte[] diskContents, String diskContentsNameFromJS) {
		// JavaScript string cannot be used directly for some reason, therefore:
		String diskContentsName = diskContentsNameFromJS != null ? "" + diskContentsNameFromJS : null;

		try {
			if (isOpen()) {
				File d64File = createReadOnlyFile(diskContents, diskContentsName);
				config.getC1541Section().setDriveOn(true);
				hardwareEnsemble.enableFloppyDiskDrives(true);
				// attach selected disk into the first disk drive
				DiskImage disk = hardwareEnsemble.getFloppies()[0].getDiskController().insertDisk(d64File);
				disk.setExtendImagePolicy(() -> true);
				installHack(d64File);
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.err.println(String.format("Cannot insert media file '%s'.", diskContentsName));
		}
	}

	@Override
	public void ejectDisk() {
		try {
			if (isOpen()) {
				hardwareEnsemble.getFloppies()[0].getDiskController().ejectDisk();
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.err.println("Cannot eject disk.");
		}
	}

	@Override
	public void insertTape(byte[] tapeContents, String tapeContentsNameFromJS) {
		// JavaScript string cannot be used directly for some reason, therefore:
		String tapeContentsName = tapeContentsNameFromJS != null ? "" + tapeContentsNameFromJS : null;

		try {
			if (isOpen()) {
				File tapeFile = createReadOnlyFile(tapeContents, tapeContentsName);
				hardwareEnsemble.getDatasette().insertTape(tapeFile);
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.err.println(String.format("Cannot insert media file '%s'.", tapeContentsName));
		}
	}

	@Override
	public void ejectTape() {
		try {
			if (isOpen()) {
				hardwareEnsemble.getDatasette().ejectTape();
			}
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.err.println("Cannot eject tape.");
		}
	}

	@Override
	public void pressPlayOnTape() {
		if (isOpen()) {
			hardwareEnsemble.getDatasette().control(Control.START);
		}
	}

	@Override
	public void typeKey(String keyCodeFromJS) {
		// JavaScript string cannot be used directly for some reason, therefore:
		String keyCode = keyCodeFromJS != null ? "" + keyCodeFromJS : null;

		KeyTableEntry key = KeyTableEntry.valueOf(keyCode);

		if (isOpen()) {
			if (key == KeyTableEntry.RESTORE) {
				c64.getKeyboard().restore();
			} else {
				c64.getKeyboard().keyPressed(key);

				c64.getEventScheduler().schedule(Event.of("Wait Until Virtual Keyboard Released", event2 -> {

					c64.getEventScheduler().scheduleThreadSafeKeyEvent(
							Event.of("Virtual Keyboard Released", event3 -> c64.getKeyboard().keyReleased(key)));

				}), c64.getClock().getCyclesPerFrame() << 2);
			}
		}
	}

	@Override
	public void pressKey(String keyCodeFromJS) {
		// JavaScript string cannot be used directly for some reason, therefore:
		String keyCode = keyCodeFromJS != null ? "" + keyCodeFromJS : null;

		KeyTableEntry key = KeyTableEntry.valueOf(keyCode);

		if (isOpen()) {
			c64.getKeyboard().keyPressed(key);
		}
	}

	@Override
	public void releaseKey(String keyCodeFromJS) {
		// JavaScript string cannot be used directly for some reason, therefore:
		String keyCode = keyCodeFromJS != null ? "" + keyCodeFromJS : null;

		KeyTableEntry key = KeyTableEntry.valueOf(keyCode);

		if (isOpen()) {
			c64.getKeyboard().keyReleased(key);
		}
	}

	@Override
	public void joystick(int number, int value) {
		if (isOpen()) {
			c64.setJoystick(number, () -> (byte) (0xff ^ value));

			c64.getEventScheduler().schedule(Event.of("Wait Until Virtual Joystick Released", event -> {

				c64.setJoystick(number, () -> (byte) (0xff));

			}), c64.getClock().getCyclesPerFrame() << 2);
		}
	}

	@Override
	public void filterName(String emulationFromJS, String chipModelFromJS, int sidNumber, String filterNameFromJS) {
		// JavaScript string cannot be used directly for some reason, therefore:
		String emulationStr = emulationFromJS != null ? "" + emulationFromJS : null;
		String chipModelStr = chipModelFromJS != null ? "" + chipModelFromJS : null;
		String filterName = filterNameFromJS != null ? "" + filterNameFromJS : null;

		final IEmulationSection emulationSection = config.getEmulationSection();
		emulationSection.setFilterName(sidNumber, Engine.EMULATION, Emulation.valueOf(emulationStr),
				ChipModel.valueOf(chipModelStr), filterName);
		if (isOpen()) {
			c64.insertSIDChips((sidNum, sidEmu) -> {
				if (SidTune.isSIDUsed(emulationSection, tune, sidNum)) {
					return sidBuilder.lock(sidEmu, sidNum, tune);
				} else if (sidEmu != NONE) {
					sidBuilder.unlock(sidEmu);
				}
				return NONE;
			}, sidNum -> SidTune.getSIDAddress(emulationSection, tune, sidNum));

		}
		LOG.finest(getFilterName(sidNumber) + ": " + filterName);
	}

	@Override
	public void delaySidBlaster(int cycles) {
		// some hackery for SIDBlaster USB to support delayed writes
		long delay = (long) (cycles / CPUClock.PAL.getCpuFrequency() * 1000000000L);
		long startTime = System.nanoTime();
		while (System.nanoTime() - startTime < delay)
			;
	}

	//
	// Private methods
	//

	private void doLog(ISidPlay2Section sidplay2Section, IAudioSection audioSection, IEmulationSection emulationSection,
			IC1541Section c1541Section) {
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

	private void autodetectPSID64() {
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

	private boolean isOpen() {
		return c64 != null;
	}

	private String getFilterName(int sidNumber) {
		switch (sidNumber) {
		case 1:
			return "StereoFilter";
		case 2:
			return "3-SIDFilter";
		default:
			return "Filter";
		}
	}

	private void installHack(File d64File) {
		c64.getCPU().setEODHack(d64File.getName().toLowerCase(Locale.US).contains("disgrace"));
	}

	private File createReadOnlyFile(byte[] fileContents, String fileContentsUrl)
			throws IOException, FileNotFoundException {
		File tmp = File.createTempFile(IOUtils.getFilenameWithoutSuffix(fileContentsUrl),
				IOUtils.getFilenameSuffix(fileContentsUrl));
		try (OutputStream os = new FileOutputStream(tmp)) {
			os.write(fileContents);
		}
		tmp.setWritable(false);
		return tmp;
	}

}
