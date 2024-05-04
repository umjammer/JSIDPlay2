package client.teavm;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static libsidplay.common.SIDEmu.NONE;
import static libsidplay.sidtune.SidTune.RESET;
import static libsidutils.CBMCodeUtils.petsciiToScreenRam;

import java.io.ByteArrayInputStream;
import java.io.File;
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
import client.teavm.config.JavaScriptConfig;
import libsidplay.C64;
import libsidplay.HardwareEnsemble;
import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.components.c1530.Datasette.Control;
import libsidplay.components.mos6510.MOS6510;
import libsidplay.components.mos656x.PALEmulation;
import libsidplay.config.IAudioSection;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidplay.sidtune.SidTuneType;
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
	public static void open(byte[] sidContents, String nameFromJS, int song, int nthFrame, boolean addSidListener)
			throws IOException, SidTuneError, LineUnavailableException, InterruptedException {
		String url = jsStringToJavaString(nameFromJS);
		config = new JavaScriptConfig();
		final IAudioSection audioSection = config.getAudioSection();
		final IEmulationSection emulationSection = config.getEmulationSection();

		doLog(audioSection, emulationSection);

		if (sidContents != null) {
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

		Map<String, String> allRoms = JavaScriptRoms.getJavaScriptRoms(false);
		Decoder decoder = Base64.getDecoder();
		byte[] charRom = decoder.decode(allRoms.get(JavaScriptRoms.CHAR_ROM));
		byte[] basicRom = decoder.decode(allRoms.get(JavaScriptRoms.BASIC_ROM));
		byte[] kernalRom = decoder.decode(allRoms.get(JavaScriptRoms.KERNAL_ROM));
		byte[] c1541Rom = decoder.decode(allRoms.get(JavaScriptRoms.C1541_ROM));
		byte[] psidDriverBin = decoder.decode(allRoms.get(JavaScriptRoms.PSID_DRIVER_ROM));

		hardwareEnsemble = new HardwareEnsemble(config, context -> new MOS6510(context), charRom, basicRom, kernalRom,
				new byte[0], new byte[0], c1541Rom, new byte[0], new byte[0]);
		hardwareEnsemble.setClock(CPUClock.getCPUClock(emulationSection, tune));
		c64 = hardwareEnsemble.getC64();
		c64.getVIC().setPalEmulation(nthFrame > 0 ? new JavaScriptPalEmulation(nthFrame) : PALEmulation.NONE);
		hardwareEnsemble.reset();
		emulationSection.getOverrideSection().reset();
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

		sidBuilder = new ReSIDBuilder(c64.getEventScheduler(), config, c64.getClock(), c64.getCartridge());
		JavaScriptAudioDriver audioDriver = new JavaScriptAudioDriver(nthFrame);
		audioDriver.open(audioSection, null, c64.getClock(), c64.getEventScheduler());
		sidBuilder.setAudioDriver(audioDriver);

		c64.insertSIDChips((sidNum, sidEmu) -> {
			if (SidTune.isSIDUsed(config.getEmulationSection(), tune, sidNum)) {
				return sidBuilder.lock(sidEmu, sidNum, tune);
			} else if (sidEmu != NONE) {
				sidBuilder.unlock(sidEmu);
			}
			return NONE;
		}, sidNum -> SidTune.getSIDAddress(config.getEmulationSection(), tune, sidNum));
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
	private static void insertDisk(byte[] diskContents, String nameFromJS) {
		File d64File = new File(jsStringToJavaString(nameFromJS));
		try {
			try (OutputStream os = new FileOutputStream(d64File)) {
				os.write(diskContents);
			}
			d64File.setWritable(false);
			config.getC1541Section().setDriveOn(true);
			hardwareEnsemble.enableFloppyDiskDrives(true);
			// attach selected disk into the first disk drive
			hardwareEnsemble.getFloppies()[0].getDiskController().insertDisk(d64File);

			installHack(d64File);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.err.println(String.format("Cannot insert media file '%s'.", d64File.getAbsolutePath()));
		}
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

	private static void doLog(IAudioSection audioSection, IEmulationSection emulationSection) {
		LOG.finest("bufferSize: " + audioSection.getBufferSize());
		LOG.finest("audioBufferSize: " + audioSection.getAudioBufferSize());
		LOG.finest("samplingRate: " + audioSection.getSamplingRate());
		LOG.finest("sampling: " + audioSection.getSampling());
		LOG.finest("reverbBypass: " + audioSection.getReverbBypass());
		LOG.finest("defaultClockSpeed: " + emulationSection.getDefaultClockSpeed());
		LOG.finest("defaultSidModel: " + emulationSection.getDefaultSidModel());
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

	//
	// main
	//

	public static void main(String[] args) throws Exception {
	}
}
