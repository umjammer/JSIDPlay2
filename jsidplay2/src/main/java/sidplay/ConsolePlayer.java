package sidplay;

import static sidplay.ini.IniConfig.getINIPath;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.sound.sampled.Mixer.Info;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

import builder.jexsid.JExSIDBuilder;
import builder.jhardsid.JHardSIDBuilder;
import builder.jsidblaster.JSIDBlasterBuilder;
import builder.jsidblaster.SIDType;
import libsidplay.components.mos6510.MOS6510;
import libsidplay.config.IWhatsSidSection;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import libsidutils.debug.MOS6510Debug;
import libsidutils.siddatabase.SidDatabase;
import sidplay.audio.JavaSound;
import sidplay.consoleplayer.ConsoleIO;
import sidplay.filefilter.AudioTuneFileFilter;
import sidplay.filefilter.VideoTuneFileFilter;
import sidplay.fingerprinting.FingerprintJsonClient;
import sidplay.ini.IniConfig;
import sidplay.ini.validator.VerboseValidator;
import sidplay.player.DebugUtil;
import sidplay.player.State;

/**
 * 
 * Main class of the console based JSIDPlay2.
 * 
 * @author ken
 *
 */
@Parameters(resourceBundle = "sidplay.ConsolePlayer")
final public class ConsolePlayer {

	static {
		DebugUtil.init();
	}

	private static final AudioTuneFileFilter AUDIO_TUNE_FILE_FILTER = new AudioTuneFileFilter();
	private static final VideoTuneFileFilter VIDEO_TUNE_FILE_FILTER = new VideoTuneFileFilter();

	@Parameter(names = { "--help", "-h" }, descriptionKey = "USAGE", help = true, order = 10000)
	private Boolean help = Boolean.FALSE;

	@Parameter(names = "--cpuDebug", hidden = true, descriptionKey = "DEBUG", order = 10001)
	private Boolean cpuDebug = Boolean.FALSE;

	@Parameter(names = { "--startSong", "-o" }, descriptionKey = "START_SONG", order = 10002)
	private Integer song = null;

	@Parameter(names = { "--verbose",
			"-v" }, descriptionKey = "VERBOSE", validateWith = VerboseValidator.class, order = 10003)
	private Integer verbose = 0;

	@Parameter(names = { "--quiet", "-q" }, descriptionKey = "QUIET", order = 10004)
	private Boolean quiet = Boolean.FALSE;

	@Parameter(descriptionKey = "FILES_AND_FOLDERS")
	private List<String> filenames = new ArrayList<>();

	@ParametersDelegate
	private IniConfig config = new IniConfig(true);

	private ConsolePlayer(final String[] args) {
		try {
			JCommander commander = JCommander.newBuilder().addObject(this).programName(getClass().getName()).build();
			commander.parse(args);
			Optional<String> optFilename = filenames.stream().findFirst();
			if (help || !optFilename.isPresent()) {
				commander.usage();
				printSoundcardDevices();
				printHardwareDevices();
				exit(1);
			}
			for (String filename : filenames) {
				File file = new File(filename);
				if (file.isDirectory()) {
					processDirectory(file);
				} else if (file.isFile()) {
					processFile(file);
				}
			}
		} catch (ParameterException | IOException | SidTuneError e) {
			System.err.println(e.getMessage());
			exit(1);
		}
	}

	private void processDirectory(File dir) throws IOException, SidTuneError {
		File[] listFiles = Optional
				.ofNullable(dir
						.listFiles(file -> AUDIO_TUNE_FILE_FILTER.accept(file) || VIDEO_TUNE_FILE_FILTER.accept(file)))
				.orElse(new File[0]);
		Arrays.sort(listFiles);
		for (File file : listFiles) {
			if (file.isDirectory()) {
				processDirectory(file);
			} else if (file.isFile()) {
				processFile(file);
			}
		}
	}

	private void processFile(File file) throws IOException, SidTuneError {
		IWhatsSidSection whatsSidSection = config.getWhatsSidSection();
		whatsSidSection.setEnable(false);
		String url = whatsSidSection.getUrl();
		String username = whatsSidSection.getUsername();
		String password = whatsSidSection.getPassword();
		int connectionTimeout = whatsSidSection.getConnectionTimeout();

		final SidTune tune = SidTune.load(file);
		tune.getInfo().setSelectedSong(song);
		final Player player = new Player(config, cpuDebug ? MOS6510Debug.class : MOS6510.class);
		player.setTune(tune);
		final ConsoleIO consoleIO = new ConsoleIO(config, file.getAbsolutePath());
		player.setMenuHook(obj -> consoleIO.menu(obj, verbose, quiet, System.out));
		player.setInteractivityHook(obj -> consoleIO.decodeKeys(obj, System.in));
		player.setWhatsSidHook(obj -> consoleIO.whatsSid(obj, quiet, System.out));
		player.setFingerPrintMatcher(new FingerprintJsonClient(url, username, password, connectionTimeout));

		if (config.getSidplay2Section().isEnableDatabase()) {
			setSIDDatabase(player);
		}
		player.setRecordingFilenameProvider(theTune -> {
			File tuneFile = new File(file.getAbsolutePath());
			String basename = new File(tuneFile.getParentFile(), PathUtils.getFilenameWithoutSuffix(tuneFile.getName()))
					.getAbsolutePath();
			if (theTune.getInfo().getSongs() > 1) {
				basename += String.format("-%02d", theTune.getInfo().getCurrentSong());
			}
			return basename;
		});
		player.startC64();
		player.stopC64(false);
		if (player.stateProperty().get() == State.QUIT) {
			throw new IOException("QUIT by user");
		}
	}

	private void setSIDDatabase(final Player player) {
		File hvscRoot = player.getConfig().getSidplay2Section().getHvsc();
		if (hvscRoot != null) {
			try {
				player.setSidDatabase(new SidDatabase(hvscRoot));
			} catch (IOException e) {
				System.err.println("WARNING: song length database can not be read: " + e.getMessage());
			}
		}
	}

	private void printSoundcardDevices() {
		int deviceIdx = 0;
		for (Info deviceInfo : JavaSound.getDeviceInfos()) {
			System.out.printf("    --deviceIndex %d -> %s (%s)\n", deviceIdx++, deviceInfo.getName(),
					deviceInfo.getDescription());
		}
	}

	private void printHardwareDevices() {
		try {
			triggerFetchDevices();
			String[] serialNumbers = JSIDBlasterBuilder.getSerialNumbers();
			if (serialNumbers.length > 0) {
				System.out.println("\nDetected SIDBlaster devices: (please add to INI file: " + getINIPath() + ")");
				System.out.printf("    SIDBlasterMapping_N=%d\n", serialNumbers.length);
				int deviceIdx = 0;
				for (String serialNumber : serialNumbers) {
					SIDType sidType = JSIDBlasterBuilder.getSidType(deviceIdx);
					System.out.printf("    SIDBlasterMapping_%d=%s=%s\n", deviceIdx++, serialNumber, sidType.name());
				}
			}
			String[] exSidDeviceNames = JExSIDBuilder.getDeviceNames();
			if (exSidDeviceNames.length > 0) {
				System.out.println("\nDetected ExSID devices:");
				for (String deviceName : exSidDeviceNames) {
					System.out.println("    " + deviceName);
				}
			}
			String[] hardSidDeviceNames = JHardSIDBuilder.getDeviceNames();
			if (hardSidDeviceNames.length > 0) {
				System.out.println("\nDetected HardSID devices:");
				for (String deviceName : hardSidDeviceNames) {
					System.out.println("    " + deviceName);
				}
			}

		} catch (UnsatisfiedLinkError e) {
			// ignore to not bother non SIDBlaster users
		}
	}

	private void triggerFetchDevices() {
		new JSIDBlasterBuilder(null, config, null);
		new JExSIDBuilder(null, config, null);
		new JHardSIDBuilder(null, config, null);
	}

	private void exit(int rc) {
		try {
			System.out.println("Press <enter> to exit the player!");
			System.in.read();
			System.exit(rc);
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(1);
		}
	}

	public static void main(final String[] args) {
		new ConsolePlayer(args);
	}

}
