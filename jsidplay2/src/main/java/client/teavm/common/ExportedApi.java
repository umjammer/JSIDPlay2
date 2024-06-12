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
import libsidplay.common.Mixer;
import libsidplay.common.SidReads;
import libsidplay.common.StereoMode;
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
	private final IConfig config;

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
	public void open(byte[] sidContents, String sidContentsName, int song, int nthFrame, boolean addSidListener,
			byte[] cartContents, String cartContentsName, String command)
			throws IOException, SidTuneError, LineUnavailableException, InterruptedException {
		this.command = command;

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
		doLogFilterNames(emulationSection, tune);

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
		PALEmulationTeaVM palEmulation = nthFrame > 0 ? new PALEmulationTeaVM(nthFrame) : null;
		c64.getVIC().setPalEmulation(palEmulation);
		if (cartContents != null) {
			insertCart(cartContents, cartContentsName);
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
					this.command = loadAddr == 0x0801 ? RUN : String.format(SYS, loadAddr);
				}
			}
			if (this.command != null) {
				if (this.command.startsWith(LOAD)) {
					// Load from tape needs someone to press play
					hardwareEnsemble.getDatasette().control(Control.START);
				}
				// Enter basic command
				typeInCommand(this.command);
			}
			c64.getEventScheduler().schedule(Event.of("PSID64 Detection", event2 -> autodetectPSID64()),
					(long) (c64.getClock().getCpuFrequency()));
		}), SidTune.getInitDelay(tune));

		AudioDriverTeaVM audioDriver = new AudioDriverTeaVM(importedApi, palEmulation);
		audioDriver.open(audioSection, null, c64.getClock(), c64.getEventScheduler());
		sidBuilder.setAudioDriver(audioDriver);

		updateSids(emulationSection);
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
	public void typeInCommand(String multiLineCommand) {
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
	public void insertDisk(byte[] diskContents, String diskContentsName) {
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
	public void insertTape(byte[] tapeContents, String tapeContentsName) {
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
	public void typeKey(KeyTableEntry key) {
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
	public void pressKey(KeyTableEntry key) {
		if (isOpen()) {
			c64.getEventScheduler()
					.scheduleThreadSafeKeyEvent(Event.of("Wait Until Virtual Keyboard Pressed", event2 -> {
						c64.getKeyboard().keyPressed(key);
					}));
		}
	}

	@Override
	public void releaseKey(KeyTableEntry key) {
		if (isOpen()) {
			c64.getEventScheduler()
					.scheduleThreadSafeKeyEvent(Event.of("Wait Until Virtual Keyboard Released", event2 -> {
						c64.getKeyboard().keyReleased(key);
					}));
		}
	}

	@Override
	public void joystick(int number, int value) {
		if (isOpen()) {
			c64.setJoystick(number, () -> (byte) (0xff ^ value));

			c64.getEventScheduler().schedule(Event.of("Wait Until Virtual Joystick Released", event -> {

				c64.getEventScheduler().scheduleThreadSafeKeyEvent(
						Event.of("Virtual Joystick Released", event2 -> c64.setJoystick(number, null)));

			}), c64.getClock().getCyclesPerFrame() << 2);
		}
	}

	@Override
	public void volumeLevels(float mainVolume, float secondVolume, float thirdVolume, float mainBalance,
			float secondBalance, float thirdBalance, int mainDelay, int secondDelay, int thirdDelay) {
		final IAudioSection audioSection = config.getAudioSection();
		audioSection.setMainVolume(mainVolume);
		audioSection.setSecondVolume(secondVolume);
		audioSection.setThirdVolume(thirdVolume);
		audioSection.setMainBalance(mainBalance);
		audioSection.setSecondBalance(secondBalance);
		audioSection.setThirdBalance(thirdBalance);
		audioSection.setMainDelay(mainDelay);
		audioSection.setSecondDelay(secondDelay);
		audioSection.setThirdDelay(thirdDelay);

		if (isOpen()) {
			sidBuilder.setVolume(0, mainVolume);
			sidBuilder.setVolume(1, secondVolume);
			sidBuilder.setVolume(2, thirdVolume);
			sidBuilder.setBalance(0, mainBalance);
			sidBuilder.setBalance(1, secondBalance);
			sidBuilder.setBalance(2, thirdBalance);
			sidBuilder.setDelay(0, mainDelay);
			sidBuilder.setDelay(1, secondDelay);
			sidBuilder.setDelay(2, thirdDelay);
		}
		LOG.finest("volumeLevels, mainVolume=" + mainVolume + ", secondVolume:" + secondVolume + ", thirdVolume:"
				+ thirdVolume);
		LOG.finest("volumeLevels, mainBalance=" + mainBalance + ", secondBalance:" + secondBalance + ", thirdBalance:"
				+ thirdBalance);
		LOG.finest(
				"volumeLevels, mainDelay=" + mainDelay + ", secondDelay:" + secondDelay + ", thirdDelay:" + thirdDelay);
	}

	@Override
	public void stereo(StereoMode stereoMode, int dualSidBase, int thirdSIDBase, boolean fakeStereo,
			SidReads sidToRead) {
		final IEmulationSection emulationSection = config.getEmulationSection();
		emulationSection.setStereoMode(stereoMode);
		emulationSection.setDualSidBase(dualSidBase);
		emulationSection.setThirdSIDBase(thirdSIDBase);
		emulationSection.setFakeStereo(fakeStereo);
		emulationSection.setSidToRead(sidToRead);

		if (isOpen()) {
			updateSids(emulationSection);
		}
		LOG.finest("stereoMode: " + stereoMode + ", dualSidBase=" + dualSidBase + ", thirdSIDBase=" + thirdSIDBase
				+ ", fakeStereo:" + fakeStereo + ", sidToRead:" + sidToRead);
	}

	@Override
	public void defaultEmulation(Emulation emulation) {
		final IEmulationSection emulationSection = config.getEmulationSection();
		emulationSection.setDefaultEmulation(emulation);

		if (isOpen()) {
			updateSids(emulationSection);
		}
		LOG.finest("defaultEmulation: " + emulation);
	}

	@Override
	public void defaultChipModel(ChipModel chipModel) {
		final IEmulationSection emulationSection = config.getEmulationSection();
		emulationSection.setDefaultSidModel(chipModel);

		if (isOpen()) {
			updateSids(emulationSection);
		}
		LOG.finest("defaultChipModel: " + chipModel);
	}

	@Override
	public void filterName(Emulation emulation, ChipModel chipModel, int sidNum, String filterName) {
		final IEmulationSection emulationSection = config.getEmulationSection();
		emulationSection.setFilterName(sidNum, Engine.EMULATION, emulation, chipModel, filterName);

		if (isOpen()) {
			updateSids(emulationSection);
		}
		LOG.finest(getFilterName(sidNum) + ": " + filterName);
	}

	@Override
	public void mute(int sidNum, int voice, boolean value) {
		final IEmulationSection emulationSection = config.getEmulationSection();
		emulationSection.setMuteVoice(sidNum, voice, value);

		if (isOpen()) {
			c64.configureSID(sidNum, sid -> sid.setVoiceMute(voice, value));
		}
		LOG.finest("mute SID" + sidNum + ", voice" + voice + ": " + value);
	}

	@Override
	public void fastForward() {
		if (isOpen()) {
			if (sidBuilder instanceof Mixer) {
				((Mixer) sidBuilder).fastForward();
			}
		}
	}

	@Override
	public void normalSpeed() {
		if (isOpen()) {
			if (sidBuilder instanceof Mixer) {
				((Mixer) sidBuilder).normalSpeed();
			}
		}
	}

	@Override
	public void freezeCartridge() {
		if (isOpen()) {
			c64.getCartridge().freeze();
		}
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
		LOG.finest("bufferSize: " + audioSection.getBufferSize());
		LOG.finest("audioBufferSize: " + audioSection.getAudioBufferSize());
		LOG.finest("samplingRate: " + audioSection.getSamplingRate());
		LOG.finest("sampling: " + audioSection.getSampling());
		LOG.finest("reverbBypass: " + audioSection.getReverbBypass());
		LOG.finest("defaultClockSpeed: " + emulationSection.getDefaultClockSpeed());
		LOG.finest("isJiffyDosInstalled: " + c1541Section.isJiffyDosInstalled());
	}

	private void doLogFilterNames(final IEmulationSection emulationSection, SidTune tune) {
		for (int sidNum = 0; sidNum < PLA.MAX_SIDS; sidNum++) {
			if (SidTune.isSIDUsed(emulationSection, tune, sidNum)) {
				Engine engine = Engine.getEngine(emulationSection, tune);
				Emulation emulation = Emulation.getEmulation(emulationSection, sidNum);
				ChipModel chipModel = ChipModel.getChipModel(emulationSection, tune, sidNum);
				String filterName = emulationSection.getFilterName(sidNum, engine, emulation, chipModel);
				LOG.finest(getFilterName(sidNum) + ": " + filterName);
			}
		}
	}

	private void insertCart(byte[] cartContents, String cartContentsName) {
		try {
			File cartFile = createReadOnlyFile(cartContents, cartContentsName);
			c64.setCartridge(CartridgeType.CRT, cartFile);
			LOG.fine("Cartridge: image attached: " + cartContentsName);
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.err.println(String.format("Cannot insert media file '%s'.", cartContentsName));
		}
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
					updateSids(emulationSection);
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

	private void updateSids(final IEmulationSection emulationSection) {
		c64.insertSIDChips((sidNum, sidEmu) -> {
			if (SidTune.isSIDUsed(emulationSection, tune, sidNum)) {
				return sidBuilder.lock(sidEmu, sidNum, tune);
			} else if (sidEmu != NONE) {
				sidBuilder.unlock(sidEmu);
			}
			return NONE;
		}, sidNum -> SidTune.getSIDAddress(emulationSection, tune, sidNum));
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
