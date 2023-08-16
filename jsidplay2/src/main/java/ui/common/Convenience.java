package ui.common;

import static libsidutils.IOUtils.deleteDirectory;
import static libsidutils.directory.DirEntry.toFilename;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiPredicate;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import libsidplay.components.cart.CartridgeType;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.IOUtils;
import libsidutils.ZipFileUtils;
import net.java.truevfs.access.TArchiveDetector;
import net.java.truevfs.access.TFile;
import sidplay.Player;
import sidplay.filefilter.UUIDFileFilter;
import ui.JSidPlay2Main;
import ui.common.filefilter.CartFileFilter;
import ui.common.filefilter.DiskFileFilter;
import ui.common.filefilter.TapeFileFilter;
import ui.common.filefilter.TuneFileFilter;
import ui.common.util.Extract7ZipUtil;
import ui.menubar.MenuBar;

/**
 * Automation for the Player.
 *
 * @author Ken Händel
 *
 */
public class Convenience {

	private static final Comparator<? super File> TOP_LEVEL_FIRST_COMPARATOR = (f1, f2) -> {
		if (f1.isDirectory() && !f2.isDirectory()) {
			return 1;
		}
		if (!f1.isDirectory() && f2.isDirectory()) {
			return -1;
		}
		String ext1 = IOUtils.getFilenameSuffix(f1.getAbsolutePath());
		String ext2 = IOUtils.getFilenameSuffix(f2.getAbsolutePath());

		if (ext1.endsWith(".sid") && !ext2.endsWith(".sid")) {
			return 1;
		}
		if (!ext1.endsWith(".sid") && ext2.endsWith(".sid")) {
			return -1;
		}
		return f1.compareTo(f2);
	};

	/** NUVIE video player */
	private static final String NUVIE_PLAYER_PRG = "/libsidplay/roms/nuvieplayer-v1.0.prg";
	private static byte[] NUVIE_PLAYER;

	static {
		try (DataInputStream is = new DataInputStream(MenuBar.class.getResourceAsStream(NUVIE_PLAYER_PRG))) {
			URL us2 = JSidPlay2Main.class.getResource(NUVIE_PLAYER_PRG);
			NUVIE_PLAYER = new byte[us2.openConnection().getContentLength()];
			is.readFully(NUVIE_PLAYER);
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	private static final Logger LOGGER = Logger.getLogger(Convenience.class.getName());

	/**
	 * Useless Apple directory.
	 */
	private static final String MACOSX = "__MACOSX";

	private static final UUIDFileFilter UUID_FILE_FILTER = new UUIDFileFilter();
	private static final TuneFileFilter TUNE_FILE_FILTER = new TuneFileFilter();
	private static final DiskFileFilter DISK_FILE_FILTER = new DiskFileFilter();
	private static final TapeFileFilter TAPE_FILE_FILTER = new TapeFileFilter();
	private static final CartFileFilter CART_FILE_FILTER = new CartFileFilter();

	/**
	 * Magically chooses files to be attached, rules are: Attach first supported
	 * file, eventually replace by lexically first disk or tape (e.g. side A, not
	 * B).
	 */
	public static final BiPredicate<File, File> LEXICALLY_FIRST_MEDIA = (file, toAttach) -> toAttach == null
			|| !TUNE_FILE_FILTER.accept(file) && file.getName().compareTo(toAttach.getName()) < 0;

	public static final BiPredicate<File, File> NO_MEDIA = (f1, f2) -> false;

	/**
	 * Auto-start commands.
	 */
	private static final String LOAD_8_1_RUN = "LOAD\"%s\",8,1\rRUN\r", LOAD_RUN = "LOAD\rRUN\r";

	private Player player;

	private static Thread deleteOutdatedTempDirectoriesHook;

	public Convenience(Player player) {
		this.player = player;
		try {
			if (deleteOutdatedTempDirectoriesHook == null) {
				deleteOutdatedTempDirectoriesHook = new Thread(this::deleteOutdatedTempDirectories);
				Runtime.getRuntime().addShutdownHook(deleteOutdatedTempDirectoriesHook);
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
	}

	public boolean autostart(File file, BiPredicate<File, File> isMediaToAttach, String dirEntry)
			throws IOException, SidTuneError {
		return autostart(file, isMediaToAttach, dirEntry, false);
	}

	/**
	 * Auto-start C64 bundle (ZIP containing well-known formats or un-zipped entry).
	 * Attach specific disk/tape/cartridge and automatically start entry.<BR>
	 *
	 * Note: temporary files are removed or marked to be removed on exit.
	 *
	 * @param file            file to open
	 * @param isMediaToAttach tester for media to attach
	 * @param dirEntry        if media to attach is a disk this directory entry is
	 *                        loaded after attaching the media (null means load
	 *                        first file on disk).
	 * @param deepScan        scan sub-directories and attach cartridges
	 * @throws IOException  image read error
	 * @throws SidTuneError invalid tune
	 */
	public boolean autostart(File file, BiPredicate<File, File> isMediaToAttach, String dirEntry, boolean deepScan)
			throws IOException, SidTuneError {
		if (!player.getC64().getCartridge().isCreatingSamples()) {
			player.getC64().ejectCartridge();
		}

		File tmpDir = new File(player.getConfig().getSidplay2Section().getTmpDir(), UUID.randomUUID().toString());
		tmpDir.mkdirs();
		boolean isCartridge = CART_FILE_FILTER.accept(file);
		TFile tFile = new TFile(file);
		File toAttach = null;
		if (tFile.exists()) {
			if (tFile.isArchive()) {
				// uncompress zip
				TFile.cp_rp(tFile, tmpDir, TArchiveDetector.ALL);
				toAttach = getToAttach(tmpDir, tFile, isMediaToAttach, null, !isCartridge);
			} else if (file.getName().toLowerCase(Locale.ENGLISH).endsWith(".gz")) {
				// uncompress gzip
				File dst = new File(tmpDir, IOUtils.getFilenameWithoutSuffix(file.getName()));
				try (InputStream is = new GZIPInputStream(ZipFileUtils.newFileInputStream(file))) {
					TFile.cp(is, dst);
				}
				toAttach = getToAttach(tmpDir, tmpDir, isMediaToAttach, null, !isCartridge);
			} else if (file.getName().toLowerCase(Locale.ENGLISH).endsWith("7z")) {
				// uncompress 7zip
				Extract7ZipUtil extract7Zip = new Extract7ZipUtil(tFile, tmpDir);
				extract7Zip.extract();
				toAttach = getToAttach(tmpDir, tmpDir, isMediaToAttach, null, !isCartridge);
			} else if (tFile.isEntry()) {
				// uncompress zip entry
				File zipEntry = new File(tmpDir, tFile.getName());
				TFile.cp_rp(tFile, zipEntry, TArchiveDetector.ALL);
				getToAttach(tmpDir, zipEntry.getParentFile(), NO_MEDIA, null, !isCartridge);
				toAttach = zipEntry;
			} else if (isSupportedMedia(file)) {
				// normal file
				if (deepScan) {
					getToAttach(file.getParentFile(), file.getParentFile(), NO_MEDIA, null, !isCartridge);
				}
				toAttach = file;
			}
		}
		if (toAttach != null) {
			if (TUNE_FILE_FILTER.accept(toAttach)) {
				player.play(SidTune.load(toAttach));
				return true;
			} else if (DISK_FILE_FILTER.accept(toAttach)) {
				player.insertDisk(toAttach);
				player.resetC64(String.format(LOAD_8_1_RUN, dirEntry != null ? toFilename(dirEntry) : "*"));
				return true;
			} else if (TAPE_FILE_FILTER.accept(toAttach)) {
				player.insertTape(toAttach);
				player.resetC64(LOAD_RUN);
				return true;
			} else if (toAttach.getName().toLowerCase(Locale.ENGLISH).endsWith(".reu")) {
				try (InputStream is = new ByteArrayInputStream(NUVIE_PLAYER)) {
					player.insertCartridge(CartridgeType.REU, toAttach);
					player.play(SidTune.load("nuvieplayer-v1.0.prg", is));
				}
				return true;
			} else if (CART_FILE_FILTER.accept(toAttach)) {
				player.insertCartridge(CartridgeType.CRT, toAttach);
				player.resetC64(null);
				return true;
			}
		}
		return false;
	}

	/**
	 * Get media file to attach, search recursively.<BR>
	 *
	 * Note: all files and folders are marked to be deleted.
	 *
	 * @param dir         directory where the files are located
	 * @param file        file to get traversed and searched for media
	 * @param mediaTester predicate to check desired media
	 * @param toAttach    current media to attach
	 * @return media to attach
	 */
	private File getToAttach(File dir, File file, BiPredicate<File, File> mediaTester, File toAttach,
			boolean canAttachCartridge) {
		final File[] listFiles = file.listFiles();
		if (listFiles == null) {
			return toAttach;
		}
		final List<File> asList = Arrays.asList(listFiles);
		asList.sort(TOP_LEVEL_FIRST_COMPARATOR);
		for (File member : asList) {
			File memberFile = new File(dir, member.getName());
			if (memberFile.isFile() && isSupportedMedia(memberFile)) {
				if (canAttachCartridge && !player.getC64().isCartridge()
						&& memberFile.getName().toLowerCase(Locale.ENGLISH).endsWith(".reu")) {
					try {
						player.insertCartridge(CartridgeType.REU, memberFile);
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else if (canAttachCartridge && !player.getC64().isCartridge()
						&& memberFile.getName().toLowerCase(Locale.ENGLISH).endsWith(".crt")) {
					try {
						player.insertCartridge(CartridgeType.CRT, memberFile);
						toAttach = memberFile;
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else if (mediaTester.test(memberFile, toAttach)) {
					toAttach = memberFile;
				}
			} else if (memberFile.isDirectory() && !memberFile.getName().equals(MACOSX)) {
				File toAttachChild = getToAttach(memberFile, new TFile(memberFile), mediaTester, toAttach,
						canAttachCartridge);
				if (toAttachChild != null) {
					toAttach = toAttachChild;
				}
			}
		}
		return toAttach;
	}

	/**
	 * Check well-known disk/tape/cartridge file extension
	 *
	 * @param file file to check
	 * @return is it a well-known format
	 */
	public boolean isSupportedMedia(File file) {
		return CART_FILE_FILTER.accept(file) || TUNE_FILE_FILTER.accept(file) || DISK_FILE_FILTER.accept(file)
				|| TAPE_FILE_FILTER.accept(file);
	}

	private void deleteOutdatedTempDirectories() {
		Arrays.asList(
				Optional.ofNullable(player.getConfig().getSidplay2Section().getTmpDir().listFiles(UUID_FILE_FILTER))
						.orElse(new File[0]))
				.stream().filter(File::isDirectory).forEach(dir -> {
					try {
						LOGGER.fine(String.format("Convenience: Delete temp. directory: %s", dir));
						deleteDirectory(dir);
					} catch (IOException e) {
						System.err.println(e.getMessage());
					}
				});
	}
}
