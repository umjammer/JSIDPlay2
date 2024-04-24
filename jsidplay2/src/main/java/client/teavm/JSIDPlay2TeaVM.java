package client.teavm;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static libsidplay.common.SIDEmu.NONE;
import static libsidplay.sidtune.SidTune.RESET;
import static libsidutils.CBMCodeUtils.petsciiToScreenRam;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Base64.Decoder;
import java.util.Map;
import java.util.logging.Logger;

import javax.sound.sampled.LineUnavailableException;

import org.teavm.interop.Export;

import builder.resid.ReSIDBuilder;
import libsidplay.C64;
import libsidplay.HardwareEnsemble;
import libsidplay.common.CPUClock;
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
	private static C64 c64;
	private static String command;
	private static int bufferSize;

	//
	// Exports to JavaScript
	//

	@Export(name = "open")
	public static void open(byte[] sidContents, String nameFromJS, int nthFrame, boolean addSidListener)
			throws IOException, SidTuneError, LineUnavailableException, InterruptedException {
		SidTune tune;
		if (sidContents != null) {
			// JavaScript string cannot be used directly for some reason, therefore:
			String url = new StringBuilder(nameFromJS).toString();
			LOG.finest("Load Tune, length=" + sidContents.length);
			LOG.finest("Tune name: " + url);
			tune = SidTune.load(url, new ByteArrayInputStream(sidContents), SidTuneType.get(url));
			tune.getInfo().setSelectedSong(null);
		} else {
			LOG.finest("RESET");
			tune = RESET;
		}
		LOG.finest("nthFrame: " + nthFrame);

		config = new JavaScriptConfig();
		final IAudioSection audioSection = config.getAudioSection();
		final IEmulationSection emulationSection = config.getEmulationSection();

		doLog(audioSection, emulationSection);

		Map<String, String> allRoms = JavaScriptRoms.getJavaScriptRoms(false);
		Decoder decoder = Base64.getDecoder();
		byte[] charRom = decoder.decode(allRoms.get(JavaScriptRoms.CHAR_ROM));
		byte[] basicRom = decoder.decode(allRoms.get(JavaScriptRoms.BASIC_ROM));
		byte[] kernalRom = decoder.decode(allRoms.get(JavaScriptRoms.KERNAL_ROM));
		byte[] psidDriverBin = decoder.decode(allRoms.get(JavaScriptRoms.PSID_DRIVER_ROM));

		HardwareEnsemble hardwareEnsemble = new HardwareEnsemble(config, context -> new MOS6510(context), charRom,
				basicRom, kernalRom, new byte[0], new byte[0], new byte[0], new byte[0], new byte[0]);
		hardwareEnsemble.setClock(CPUClock.getCPUClock(emulationSection, tune));
		c64 = hardwareEnsemble.getC64();
		c64.getVIC().setPalEmulation(nthFrame > 0 ? new JavaScriptPalEmulation() : PALEmulation.NONE);
		hardwareEnsemble.reset();
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
		}), SidTune.getInitDelay(tune));

		ReSIDBuilder sidBuilder = new ReSIDBuilder(c64.getEventScheduler(), config, c64.getClock(), c64.getCartridge());
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

	@Export(name = "clock")
	public static int clock() throws InterruptedException {
		for (int i = 0; i < bufferSize; i++) {
			context.clock();
		}
		return bufferSize;
	}

	@Export(name = "close")
	public static void close() {
		bufferSize = 0;
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

	private static void typeInCommand(final String multiLineCommand) {
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

	private static void doLog(IAudioSection audioSection, IEmulationSection emulationSection) {
		LOG.finest("bufferSize: " + audioSection.getBufferSize());
		LOG.finest("audioBufferSize: " + audioSection.getAudioBufferSize());
		LOG.finest("samplingRate: " + audioSection.getSamplingRate());
		LOG.finest("sampling: " + audioSection.getSampling());
		LOG.finest("reverbBypass: " + audioSection.getReverbBypass());
		LOG.finest("defaultClockSpeed: " + emulationSection.getDefaultClockSpeed());
		LOG.finest("defaultSidModel: " + emulationSection.getDefaultSidModel());
	}

	//
	// main
	//

	public static void main(String[] args) throws Exception {
	}
}
