package build;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static libsidutils.IOUtils.deleteDirectory;
import static ui.entities.PersistenceProperties.CGSC_DS;
import static ui.entities.PersistenceProperties.HVSC_DS;
import static ui.musiccollection.MusicCollectionType.CGSC;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.annotation.WebServlet;
import libsidutils.IOUtils;
import libsidutils.siddatabase.SidDatabase;
import libsidutils.stil.STIL;
import net.java.truevfs.access.TArchiveDetector;
import net.java.truevfs.access.TFile;
import net.java.truevfs.access.TVFS;
import server.restful.JSIDPlay2Server;
import server.restful.common.parameter.ServletParameterHelper;
import sidplay.Player;
import sidplay.ini.IniDefaults;
import sidplay.player.DebugUtil;
import ui.common.download.DownloadThread;
import ui.common.util.Extract7ZipUtil;
import ui.entities.DatabaseType;
import ui.entities.PersistenceProperties;
import ui.musiccollection.MusicCollectionType;
import ui.musiccollection.search.SearchIndexCreator;
import ui.musiccollection.search.SearchIndexerThread;

/**
 * Helper class to create all the optional downloadable content of our
 * web-server during deployment.
 * 
 * @author ken
 *
 */
@Parameters(resourceBundle = "build.OnlineContent")
public class OnlineContent {

	static {
		DebugUtil.init();
		System.setProperty("com.mysql.cj.disableAbandonedConnectionCleanup", "true");
	}

	private static final int MAX_ZIP_FILESIZE = 37748736;

	private static final int CHUNK_SIZE = 1 << 20;

	@Parameter(names = { "--help", "-h" }, descriptionKey = "USAGE", help = true, order = 0)
	private Boolean help = Boolean.FALSE;

	@Parameter(names = { "--phase" }, descriptionKey = "PHASE", order = 1)
	private String phase;

	@Parameter(names = { "--deployDir" }, descriptionKey = "DEPLOY_DIR", order = 2)
	private String deployDir;

	@Parameter(names = { "--projectVersion" }, descriptionKey = "PROJECT_VERSION", order = 3)
	private String projectVersion;

	@Parameter(names = { "--upxExe" }, descriptionKey = "UPX_EXE", order = 4)
	private String upxExe;

	@Parameter(names = { "--baseDir" }, descriptionKey = "BASE_DIR", order = 5)
	private String baseDir;

	@Parameter(names = { "--classesDir" }, descriptionKey = "CLASSES_DIR", order = 6)
	private String classesDir;

	@Parameter(names = { "--gb64" }, descriptionKey = "GB64", order = 7)
	private String gb64;

	@Parameter(names = { "--hvmec" }, descriptionKey = "HVMEC", order = 8)
	private String hvmec;

	@Parameter(names = { "--cgsc" }, descriptionKey = "CGSC", order = 9)
	private String cgsc;

	@Parameter(names = { "--hvsc" }, descriptionKey = "HVSC", order = 10)
	private String hvsc;

	private volatile boolean ready;

	private void execute(String[] args) throws Exception {
		JCommander commander = JCommander.newBuilder().addObject(this).programName(getClass().getName()).build();
		commander.parse(Arrays.asList(args).stream().map(arg -> arg == null ? "" : arg).toArray(String[]::new));
		if (help) {
			commander.usage();
			System.out.println("Press <enter> to exit!");
			System.in.read();
			System.exit(0);
		}

		if ("prepare-package".equals(phase)) {

			createServerClzListAndCheck();

		} else if ("install".equals(phase)) {

			if (upxExe != null) {
				upx();
			}
			createDemos();

			if (gb64 != null) {
				gb64();
			}
			if (hvmec != null) {
				hvmec();
			}
			if (cgsc != null) {
				cgsc();
			}
			if (hvsc != null) {
				hvsc();
			}
			latestVersion();
		}
	}

	private void upx() throws IOException, InterruptedException {
		if (!new File(upxExe).exists() || !new File(upxExe).canExecute()) {
			System.err.println("Warning: UPX Program not found or not executable: " + upxExe);
			return;
		}
		Process proc = Runtime.getRuntime().exec(new String[] { upxExe, "--lzma", "--best",
				/* "--ultra-brute", */deployDir + "/jsiddevice-" + projectVersion + ".exe" });

		IOUtils.copy(proc.getErrorStream(), System.err);
		IOUtils.copy(proc.getInputStream(), System.out);

		proc.waitFor();
	}

	private void createDemos() throws IOException {
		new File(deployDir, "online/demos").mkdirs();

		File demosZipFile = new File(deployDir, "online/demos/Demos.zip");
		File source = new File(baseDir, "src/test/resources/demos/Demos.zip");
		Files.copy(Paths.get(source.toURI()), Paths.get(demosZipFile.toURI()), REPLACE_EXISTING);

		createCRC(demosZipFile, new File(deployDir, "online/demos/Demos.crc"));
	}

	private void gb64() throws IOException {
		File mdbFile = new File(gb64);
		if (mdbFile.exists()) {
			new File(deployDir, "online/gamebase").mkdirs();

			File mdbZipFile = new File(deployDir, "/online/gamebase/GameBase64.zip");
			mdbZipFile.delete();
			TFile.cp_rp(mdbFile, new TFile(mdbZipFile, mdbFile.getName()), TArchiveDetector.ALL);
			TVFS.umount();

			createCRC(mdbZipFile, new File(deployDir, "online/gamebase/GameBase64.crc"));
		}
	}

	private void hvmec() throws IOException {
		File hvmecFile = new File(hvmec);
		if (hvmecFile.exists()) {
			new File(deployDir, "online/hvmec").mkdirs();

			File hvmecZipFile = new File(deployDir, "/online/hvmec/HVMEC.zip");
			hvmecZipFile.delete();
			TFile.cp_rp(hvmecFile, hvmecZipFile, TArchiveDetector.ALL);
			TVFS.umount();

			createCRC(hvmecZipFile, new File(deployDir, "online/hvmec/HVMEC.crc"));
		}
	}

	private void cgsc() throws Exception {
		File cgsc7zFile = new File(cgsc);
		if (cgsc7zFile.exists()) {
			new File(deployDir, "online/cgsc").mkdirs();

			System.out.println("Extracting archive, please wait a moment...");
			File cgscZipFile = new File(deployDir, "/online/cgsc/CGSC.zip");
			Extract7ZipUtil extract7Zip = new Extract7ZipUtil(new File(cgsc), new File(deployDir, "online/cgsc"));
			extract7Zip.extract();

			cgscZipFile.delete();
			TFile.cp_rp(new File(deployDir, "online/cgsc/CGSC"), new TFile(cgscZipFile), TArchiveDetector.ALL);
			TVFS.umount();

			deleteDirectory(new File(deployDir, "online/cgsc/CGSC"));

			createCRC(cgscZipFile, new File(deployDir, "online/cgsc/CGSC.crc"));

			doCreateIndex(MusicCollectionType.CGSC, cgscZipFile.getAbsolutePath());
		}
	}

	private void hvsc() throws Exception {
		File hvsc7zFile = new File(hvsc);
		if (hvsc7zFile.exists()) {
			new File(deployDir, "online/hvsc").mkdirs();

			System.out.println("Extracting archive, please wait a moment...");
			File hvscZipFile = new File(deployDir, "/online/hvsc/C64Music.zip");
			Extract7ZipUtil extract7Zip = new Extract7ZipUtil(new File(hvsc), new File(deployDir, "online/hvsc"));
			extract7Zip.extract();

			hvscZipFile.delete();
			TFile.cp_rp(new File(deployDir, "online/hvsc/C64Music"), new TFile(hvscZipFile), TArchiveDetector.ALL);
			TVFS.umount();

			deleteDirectory(new File(deployDir, "online/hvsc/C64Music"));

			createCRC(hvscZipFile, new File(deployDir, "online/hvsc/C64Music.crc"));

			doCreateIndex(MusicCollectionType.HVSC, hvscZipFile.getAbsolutePath());

			doSplit(MAX_ZIP_FILESIZE, hvscZipFile.getAbsolutePath());

			hvscZipFile.delete();
		}
	}

	private void latestVersion() throws IOException, FileNotFoundException, UnsupportedEncodingException {
		File versionFile = new File(baseDir, "latest.properties");
		versionFile.delete();
		versionFile.createNewFile();
		try (Writer writer = new PrintWriter(versionFile, StandardCharsets.ISO_8859_1.toString())) {
			writer.append("version=" + projectVersion);
		}
	}

	private void doCreateIndex(MusicCollectionType collectionType, String zipFile) throws Exception {
		File rootFile = new TFile(zipFile);

		String persistenceUnitName = collectionType == CGSC ? CGSC_DS : HVSC_DS;
		File dbFilename = new File(rootFile.getParentFile(), collectionType.toString());
		EntityManagerFactory emFactory = Persistence.createEntityManagerFactory(persistenceUnitName,
				new PersistenceProperties(DatabaseType.HSQL_FILE, "", "", dbFilename.getAbsolutePath()));
		EntityManager em = emFactory.createEntityManager();

		Player player = new Player(IniDefaults.DEFAULTS);
		if (collectionType == MusicCollectionType.HVSC) {
			setSIDDatabase(player, rootFile);
			setSTIL(player, rootFile);
		}

		SearchIndexCreator searchIndexCreator = new SearchIndexCreator(rootFile, player, em);
		Consumer<Void> searchStart = v -> searchIndexCreator.getSearchStart().accept(v);
		Consumer<File> searchHit = searchIndexCreator.getSearchHit();
		Consumer<Boolean> searchStop = cancelled -> {
			searchIndexCreator.getSearchStop().accept(cancelled);
			ready = true;
		};
		ready = false;
		new SearchIndexerThread(rootFile, searchStart, searchHit, searchStop).start();

		System.out.println("Creating index, please wait a moment...");
		while (!ready) {
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				System.err.println("Interrupted while sleeping!");
			}
		}
		emFactory.close();
		// Really persist the databases
		org.hsqldb.DatabaseManager.closeDatabases(org.hsqldb.Database.CLOSEMODE_NORMAL);
	}

	private void setSTIL(Player player, File zipFile) throws NoSuchFieldException, IllegalAccessException {
		try {
			player.setSTIL(new STIL(zipFile));
		} catch (IOException e) {
			System.err.println("WARNING: STIL can not be read: " + e.getMessage());
		}
	}

	private void setSIDDatabase(Player player, File zipFile) {
		try {
			player.setSidDatabase(new SidDatabase(zipFile));
		} catch (IOException e) {
			System.err.println("WARNING: song length database can not be read: " + e.getMessage());
		}
	}

	private void doSplit(int maxFileSize, String filename) throws IOException {
		int partNum = 1;
		String output = createOutputFilename(filename, partNum);

		byte[] buffer = new byte[CHUNK_SIZE];
		BufferedOutputStream os = null;
		try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(new File(filename)), CHUNK_SIZE)) {
			int bytesRead = 0, totalBytesRead = 0;
			os = createOutputStream(output);
			int len = Math.min(buffer.length, maxFileSize - totalBytesRead);
			while ((bytesRead = is.read(buffer, 0, len)) >= 0) {
				os.write(buffer, 0, bytesRead);
				totalBytesRead += bytesRead;
				len = Math.min(buffer.length, maxFileSize - totalBytesRead);
				if (totalBytesRead == maxFileSize) {
					os.close();
					++partNum;
					output = createOutputFilename(filename, partNum);
					os = createOutputStream(output);
					totalBytesRead = 0;
				}
			}
		} finally {
			if (os != null) {
				try {
					os.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	private String createOutputFilename(String filename, int partNum) {
		return IOUtils.getFilenameWithoutSuffix(filename) + String.format(".%03d", partNum);
	}

	private BufferedOutputStream createOutputStream(String filename) throws FileNotFoundException {
		return new BufferedOutputStream(new FileOutputStream(new File(filename)), CHUNK_SIZE);
	}

	private void createCRC(File demosZipFile, File crcFile)
			throws IOException, FileNotFoundException, UnsupportedEncodingException {
		try (Writer writer = new PrintWriter(crcFile, StandardCharsets.ISO_8859_1.toString())) {
			Properties properties = new Properties();
			properties.setProperty("filename", demosZipFile.getName());
			properties.setProperty("size", String.valueOf(demosZipFile.length()));
			properties.setProperty("crc32", DownloadThread.calculateCRC32(demosZipFile));
			properties.store(writer, null);
		}
	}

	private void createServerClzListAndCheck() throws IOException, SecurityException, ClassNotFoundException {
		Path root = new File(classesDir).toPath();

		Collection<String> clzList = new ArrayList<>();
		Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
				String relPath = root.relativize(path).toFile().getPath();
				if (relPath.endsWith(".class")) {
					clzList.add(IOUtils.getFilenameWithoutSuffix(relPath.replace(File.separatorChar, '.')));
				}
				return FileVisitResult.CONTINUE;
			}
		});
		try (FileWriter servlets = new FileWriter(root.resolve("tomcat-servlets.list").toFile());
				FileWriter filters = new FileWriter(root.resolve("tomcat-filters.list").toFile());) {
			for (String clzName : clzList) {
				try {
					Class<?> clz = getClass().getClassLoader().loadClass(clzName);
					if (clz.getAnnotation(WebServlet.class) != null) {
						servlets.write(clzName + ",");
					}
					if (clz.getAnnotation(WebFilter.class) != null) {
						filters.write(clzName + ",");
					}
					if (clz.getAnnotation(Parameters.class) != null) {
						// Parameter classes are being checked for development errors at build time
						if (clzName.startsWith(JSIDPlay2Server.class.getPackage().getName())) {
							// ... check server parameters
							ServletParameterHelper.check(clz, true);
						} else {
							for (Class<?> cls : Arrays.asList(clz, clz.getEnclosingClass()).stream()
									.filter(Objects::nonNull).collect(Collectors.toList())) {
								try {
									// ... check main parameters
									cls.getMethod("main", String[].class);
									ServletParameterHelper.check(clz, false);
								} catch (NoSuchMethodException e) {
								}
							}
						}
					}
				} catch (ClassNotFoundException e) {
					// ignore (maybe TeaVM dependencies not found?)
				}
			}
		}
	}

	public static void main(String[] args) throws Exception {
		new OnlineContent().execute(args);
	}

}
