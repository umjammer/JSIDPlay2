package server.restful.teavm;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static libsidplay.common.SIDEmu.NONE;
import static libsidplay.sidtune.SidTune.RESET;
import static libsidutils.CBMCodeUtils.petsciiToScreenRam;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Logger;

import javax.sound.sampled.LineUnavailableException;

import org.teavm.interop.Async;
import org.teavm.interop.Export;
import org.teavm.interop.Import;

import builder.resid.ReSIDBuilder;
import libsidplay.HardwareEnsemble;
import libsidplay.common.CPUClock;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.components.c1530.Datasette.Control;
import libsidplay.components.mos6510.MOS6510;
import libsidplay.components.mos656x.PALEmulation;
import libsidplay.config.IConfig;
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
	private static HardwareEnsemble hardwareEnsemble;
	private static String command;
	private static int bufferSize;

	@Export(name = "jsidplay2")
	public static void jsidplay2() throws Exception {
		config = new EmptyConfig();
		LOG.finest("bufferSize: " + getBufferSize());
		LOG.finest("audioBufferSize: " + getAudioBufferSize());
		config.getAudioSection().setBufferSize(getBufferSize());
		config.getAudioSection().setAudioBufferSize(getAudioBufferSize());
	}

	@Export(name = "open")
	public static void open(int sidTuneLength, int sidTuneTypeNum)
			throws IOException, SidTuneError, LineUnavailableException, InterruptedException {
		SidTuneType sidTuneType = SidTuneType.get(sidTuneTypeNum);
		String name = "example" + sidTuneType.getFileExtension();

		hardwareEnsemble = new HardwareEnsemble(config, context -> new MOS6510(context), CharsRom.CHARS, BasicRom.BASIC,
				KernalRom.KERNAL, new byte[0], new byte[0], new byte[0], new byte[0], new byte[0]);
		hardwareEnsemble.getC64().getVIC().setPalEmulation(PALEmulation.NONE);
		byte[] contents = new byte[sidTuneLength];
		getSid(contents);
		SidTune tune = SidTune.load(name, new ByteArrayInputStream(contents), sidTuneType);
		tune.getInfo().setSelectedSong(null);

		hardwareEnsemble.reset();
		hardwareEnsemble.getC64().getEventScheduler().schedule(Event.of("Auto-start", event -> {
			if (tune != RESET) {
				// for tunes: Install player into RAM
				Integer driverAddress = tune.placeProgramInMemoryTeaVM(hardwareEnsemble.getC64().getRAM(), PSid.PSID);
				if (driverAddress != null) {
					// Set play address to feedback call frames counter.
					hardwareEnsemble.getC64().setPlayAddr(tune.getInfo().getPlayAddr());
					// Start SID player driver
					hardwareEnsemble.getC64().getCPU().forcedJump(driverAddress);
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
		ReSIDBuilder sidBuilder = new ReSIDBuilder(hardwareEnsemble.getC64().getEventScheduler(), config, CPUClock.PAL,
				hardwareEnsemble.getC64().getCartridge());
		JavaScriptAudioDriver audioDriver = new JavaScriptAudioDriver();
		audioDriver.open(config.getAudioSection(), null, hardwareEnsemble.getC64().getClock(),
				hardwareEnsemble.getC64().getEventScheduler());
		sidBuilder.setAudioDriver(audioDriver);
		hardwareEnsemble.getC64().insertSIDChips((sidNum, sidEmu) -> {
			if (SidTune.isSIDUsed(config.getEmulationSection(), tune, sidNum)) {
				return sidBuilder.lock(sidEmu, sidNum, tune);
			} else if (sidEmu != NONE) {
				sidBuilder.unlock(sidEmu);
			}
			return NONE;
		}, sidNum -> SidTune.getSIDAddress(config.getEmulationSection(), tune, sidNum));
		sidBuilder.start();
		bufferSize = config.getAudioSection().getBufferSize();
		context = hardwareEnsemble.getC64().getEventScheduler();
	}

	@Async
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
		config = null;
		hardwareEnsemble = null;
		command = null;
		context = null;
	}

	@Import(module = "env", name = "getSid")
	public static native void getSid(byte[] contents);

	@Import(module = "env", name = "getBufferSize")
	public static native int getBufferSize();

	@Import(module = "env", name = "getAudioBufferSize")
	public static native int getAudioBufferSize();

	@Export(name = "delaySidBlaster")
	public static void delaySidBlaster(int cycles) {
		long delay = (long) (cycles / CPUClock.PAL.getCpuFrequency() * 1000000000L);
		long startTime = System.nanoTime();
		while (System.nanoTime() - startTime < delay)
			;
	}

	private static void typeInCommand(final String multiLineCommand) {
		String command;
		if (multiLineCommand.length() > MAX_COMMAND_LEN) {
			String[] lines = multiLineCommand.split("\r");
			for (String line : lines) {
				byte[] screenRam = petsciiToScreenRam(line);
				System.arraycopy(screenRam, 0, hardwareEnsemble.getC64().getRAM(), RAM_COMMAND_SCREEN_ADDRESS,
						screenRam.length);
				break;
			}
			int indexOf = multiLineCommand.indexOf('\r');
			command = indexOf != -1 ? multiLineCommand.substring(indexOf) : "\r";
		} else {
			command = multiLineCommand;
		}
		final int length = Math.min(command.length(), MAX_COMMAND_LEN);
		System.arraycopy(command.getBytes(US_ASCII), 0, hardwareEnsemble.getC64().getRAM(), RAM_COMMAND, length);
		hardwareEnsemble.getC64().getRAM()[RAM_COMMAND_LEN] = (byte) length;
	}

	public static void main(String[] args) throws Exception {
	}
}
