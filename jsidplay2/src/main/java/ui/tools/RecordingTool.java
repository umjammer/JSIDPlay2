package ui.tools;

import static libsidplay.config.IWhatsSidSystemProperties.AWAIT_TERMINATION;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

import libsidplay.common.SamplingRate;
import libsidplay.components.mos656x.PALEmulation;
import libsidplay.sidtune.MD5Method;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.IOUtils;
import libsidutils.fingerprinting.FingerPrinting;
import libsidutils.fingerprinting.ini.IniFingerprintConfig;
import libsidutils.siddatabase.SidDatabase;
import sidplay.Player;
import sidplay.audio.AudioDriver;
import sidplay.filefilter.AudioTuneFileFilter;
import sidplay.filefilter.VideoTuneFileFilter;
import sidplay.ini.IniConfig;
import sidplay.ini.converter.FileToStringConverter;
import sidplay.player.DebugUtil;
import ui.entities.PersistenceProperties;
import ui.entities.whatssid.service.WhatsSidService;
import ui.tools.audio.WhatsSidDriver;

/**
 * WhatsSID? is a Shazam like feature. It analyzes tunes to recognize a
 * currently played tune.
 *
 * This is the main class to create or Update a WhatsSID database.
 *
 * This is the program to create the fingerprintings for all tunes of a
 * collection.
 *
 * This program has been expanded to be a more general recording tool where
 * several tunes are recorded in parallel.
 *
 * @author ken
 *
 */
@Parameters(resourceBundle = "ui.tools.RecordingTool")
public class RecordingTool {

	static {
		DebugUtil.init();
	}

	private static final AudioTuneFileFilter AUDIO_TUNE_FILE_FILTER = new AudioTuneFileFilter();
	private static final VideoTuneFileFilter VIDEO_TUNE_FILE_FILTER = new VideoTuneFileFilter();

	@Parameter(names = { "--help", "-h" }, descriptionKey = "USAGE", help = true, order = 10000)
	private Boolean help = Boolean.FALSE;

	@Parameter(names = { "--maxThreads" }, descriptionKey = "MAX_THREADS", order = 10001)
	private Integer maxThreads = Runtime.getRuntime().availableProcessors();

	@Parameter(names = {
			"--destinationDirectory" }, descriptionKey = "DESTINATION_DIRECTORY", converter = FileToStringConverter.class, order = 10002)
	private File destinationDirectory;

	@Parameter(names = { "--fingerprinting" }, descriptionKey = "FINGERPRINTING", arity = 1, order = 10003)
	private Boolean fingerprinting = Boolean.FALSE;

	@Parameter(names = { "--whatsSIDDatabaseDriver" }, descriptionKey = "WHATSSID_DATABASE_DRIVER", order = 10004)
	private String whatsSidDatabaseDriver;

	@Parameter(names = { "--whatsSIDDatabaseUrl" }, descriptionKey = "WHATSSID_DATABASE_URL", order = 10005)
	private String whatsSidDatabaseUrl;

	@Parameter(names = { "--whatsSIDDatabaseUsername" }, descriptionKey = "WHATSSID_DATABASE_USERNAME", order = 10006)
	private String whatsSidDatabaseUsername;

	@Parameter(names = { "--whatsSIDDatabasePassword" }, descriptionKey = "WHATSSID_DATABASE_PASSWORD", order = 10007)
	private String whatsSidDatabasePassword;

	@Parameter(names = { "--whatsSIDDatabaseDialect" }, descriptionKey = "WHATSSID_DATABASE_DIALECT", order = 10008)
	private String whatsSidDatabaseDialect;

	@Parameter(names = { "--createIni" }, descriptionKey = "CREATE_INI", arity = 1, order = 10009)
	private Boolean createIni = Boolean.FALSE;

	@Parameter(names = { "--deleteAll" }, descriptionKey = "DELETE_ALL", arity = 1, order = 10010)
	private Boolean deleteAll = Boolean.FALSE;

	@Parameter(names = {
			"--previousDirectory" }, descriptionKey = "PREVIOUS_DIRECTORY", converter = FileToStringConverter.class, order = 10011)
	private File previousDirectory;

	@Parameter(description = "directory", converter = FileToStringConverter.class)
	private File directory;

	@ParametersDelegate
	private IniConfig config = new IniConfig(false);

	private static final ThreadLocal<EntityManager> THREAD_LOCAL_ENTITY_MANAGER = new ThreadLocal<>();

	private static EntityManagerFactory entityManagerFactory;

	private ExecutorService executor;

	private SidDatabase previousSidDatabase;

	private volatile boolean quit;

	private void execute(String[] args) {
		try {
			JCommander commander = JCommander.newBuilder().addObject(this).programName(getClass().getName()).build();
			commander.parse(args);
			if (help || directory == null) {
				commander.usage();
				System.out.println("Press <enter> to exit!");
				System.in.read();
				System.exit(0);
			}
			if (config.getSidplay2Section().getHvsc() == null) {
				System.out.println("Parameter --hvsc is required!");
				System.exit(1);
			}
			if (fingerprinting) {
				config.getAudioSection().setSamplingRate(SamplingRate.VERY_LOW);
			}
			config.getSidplay2Section().setEnableDatabase(true);
			config.getSidplay2Section().setSingle(false);

			if (previousDirectory != null) {
				previousSidDatabase = new SidDatabase(previousDirectory);
			}

			if (fingerprinting) {
				entityManagerFactory = Persistence.createEntityManagerFactory(PersistenceProperties.WHATSSID_DS,
						new PersistenceProperties(whatsSidDatabaseDriver, whatsSidDatabaseUrl, whatsSidDatabaseUsername,
								whatsSidDatabasePassword, whatsSidDatabaseDialect));

				WhatsSidService whatsSidService = new WhatsSidService(getEntityManager());

				if (Boolean.TRUE.equals(deleteAll)) {
					deleteAllFingerprintings(whatsSidService);
				}
			}
			executor = Executors.newFixedThreadPool(maxThreads);
			System.out.println("Create Recordings... (To abort press q <return> and wait for termination)");

			processDirectory(executor, directory);
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			if (executor != null) {
				try {
					executor.shutdown();
					if (!executor.awaitTermination(AWAIT_TERMINATION, TimeUnit.DAYS)) {
						executor.shutdownNow();
					}
				} catch (InterruptedException e) {
					executor.shutdownNow();
				}
			}
			if (fingerprinting) {
				freeEntityManager();
			}
			if (entityManagerFactory != null && entityManagerFactory.isOpen()) {
				entityManagerFactory.close();
			}
			System.exit(0);
		}
	}

	private void deleteAllFingerprintings(WhatsSidService whatsSidService) throws IOException {
		System.out.println("Delete all fingerprintings...");
		switch (proceed()) {
		case 'y':
		case 'Y':
			whatsSidService.deleteAll();
			System.out.println("Done!");
			break;

		default:
			System.out.println("Aborted by user!");
			break;

		}
	}

	private int proceed() throws IOException {
		System.out.println(
				"You are about to delete all fingerprintings from the database. Are you sure to proceed? (y/N)");
		return System.in.read();
	}

	private void processDirectory(ExecutorService executor, File dir) throws IOException, SidTuneError {
		if (quit) {
			return;
		}
		File[] listFiles = Optional.ofNullable(dir.listFiles()).orElse(new File[0]);
		Arrays.sort(listFiles);
		for (File file : listFiles) {
			if (file.isDirectory()) {
				processDirectory(executor, file);
			} else if (file.isFile()) {
				if (!quit && (AUDIO_TUNE_FILE_FILTER.accept(file) || VIDEO_TUNE_FILE_FILTER.accept(file))) {
					executor.execute(() -> {
						try {
							if (quit) {
								return;
							}
							processFile(file);
							if (System.in.available() > 0) {
								final int key = System.in.read();
								if (key == 'q') {
									quit = true;
									System.err.println(
											"Termination after pressing q, please wait for last recordings to finish");
								}
							}
						} catch (IOException | SidTuneError e) {
							e.printStackTrace();
						}
					});
				}
			}
		}
	}

	private void processFile(File file) throws IOException, SidTuneError {
		try {
			SidTune tune = SidTune.load(file);
			String collectionName = IOUtils.getCollectionName(directory, file);

			Player player = new Player(config);
			player.getC64().getVIC().setPalEmulation(PALEmulation.NONE);
			player.setSidDatabase(new SidDatabase(config.getSidplay2Section().getHvsc()));

			AudioDriver audioDriver;
			if (fingerprinting) {
				final WhatsSidService whatsSidService = new WhatsSidService(getEntityManager());

				WhatsSidDriver whatsSidDriver = new WhatsSidDriver();
				whatsSidDriver.setFingerprintInserter(
						new FingerPrinting(new IniFingerprintConfig(createIni), whatsSidService));
				whatsSidDriver.setCollectionName(collectionName);
				whatsSidDriver.setTune(tune);
				audioDriver = whatsSidDriver;

				if (previousDirectory != null) {
					copyRecordingsOfPreviousDirectory(player, whatsSidDriver, file, tune, collectionName);
				}
			} else {
				audioDriver = player.getConfig().getAudioSection().getAudio().newAudioDriver();
				boolean allRecordingsExist = true;
				for (int songNo = 1; songNo <= tune.getInfo().getSongs(); songNo++) {
					allRecordingsExist &= new File(
							getRecordingFilename(collectionName, file, tune, songNo) + audioDriver.getExtension())
							.exists();
				}
				if (allRecordingsExist) {
					return;
				}
			}
			player.setAudioDriver(audioDriver);

			player.setRecordingFilenameProvider(
					theTune -> getRecordingFilename(collectionName, file, theTune, theTune.getInfo().getCurrentSong()));
			player.setTune(tune);
			player.startC64();
			player.stopC64(false);
		} finally {
			if (fingerprinting) {
				freeEntityManager();
			}
		}
	}

	private void copyRecordingsOfPreviousDirectory(Player player, AudioDriver whatsSidDriver, File file, SidTune tune,
			String collectionName) throws IOException, SidTuneError {
		File previousFile = new File(previousDirectory, collectionName);
		if (previousFile.exists()) {
			SidTune previousTune = SidTune.load(previousFile);
			if (Objects.equals(tune.getMD5Digest(MD5Method.MD5_CONTENTS),
					previousTune.getMD5Digest(MD5Method.MD5_CONTENTS))
					&& player.getSidDatabaseInfo(db -> db.getTuneLength(tune), 0.) == previousSidDatabase
							.getTuneLength(previousTune)) {
				for (int songNo = 1; songNo <= tune.getInfo().getSongs(); songNo++) {
					File recordedFile = new File(
							getRecordingFilename(collectionName, file, tune, songNo) + whatsSidDriver.getExtension());
					File previousRecordedFile = new File(
							getRecordingFilename(collectionName, previousFile, previousTune, songNo)
									+ whatsSidDriver.getExtension());
					if (!recordedFile.exists() && previousRecordedFile.exists()) {
						System.out.println(
								String.format("Tune is unchanged, copy %s to %s", previousRecordedFile, recordedFile));
						Files.copy(previousRecordedFile.toPath(), recordedFile.toPath(),
								StandardCopyOption.REPLACE_EXISTING);
					}
				}
			}
		}
	}

	private String getRecordingFilename(String collectionName, File file, SidTune tune, int song) {
		File targetFile;
		if (destinationDirectory != null) {
			targetFile = new File(destinationDirectory, collectionName);
			targetFile.getParentFile().mkdirs();
		} else {
			targetFile = file;
		}
		String filename = IOUtils.getFilenameWithoutSuffix(targetFile.getAbsolutePath());
		if (tune.getInfo().getSongs() > 1) {
			filename += String.format("-%02d", song);
		}
		return filename;
	}

	public static EntityManager getEntityManager() throws IOException {
		if (entityManagerFactory == null) {
			throw new IOException("Database required, please specify command line parameters!");
		}
		EntityManager em = THREAD_LOCAL_ENTITY_MANAGER.get();

		if (em == null) {
			em = entityManagerFactory.createEntityManager();
			THREAD_LOCAL_ENTITY_MANAGER.set(em);
		}
		return em;
	}

	public static void freeEntityManager() {
		EntityManager em = THREAD_LOCAL_ENTITY_MANAGER.get();

		if (em != null) {
			em.clear();
		}
	}

	public static void main(String[] args) {
		new RecordingTool().execute(args);
	}

}
