package ui.common;

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
import java.util.function.BiPredicate;
import java.util.zip.GZIPInputStream;

import libsidplay.components.cart.CartridgeType;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import libsidutils.ZipFileUtils;
import net.java.truevfs.access.TArchiveDetector;
import net.java.truevfs.access.TFile;
import sidplay.Player;
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
		String ext1 = PathUtils.getFilenameSuffix(f1.getAbsolutePath());
		String ext2 = PathUtils.getFilenameSuffix(f2.getAbsolutePath());

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

	/**
	 * Useless Apple directory.
	 */
	private static final String MACOSX = "__MACOSX";

	private static final TuneFileFilter tuneFileFilter = new TuneFileFilter();
	private static final DiskFileFilter diskFileFilter = new DiskFileFilter();
	private static final TapeFileFilter tapeFileFilter = new TapeFileFilter();
	private static final CartFileFilter cartFileFilter = new CartFileFilter();

	/**
	 * Magically chooses files to be attached, rules are: Attach first supported
	 * file, eventually replace by lexically first disk or tape (e.g. side A, not
	 * B).
	 */
	public static final BiPredicate<File, File> LEXICALLY_FIRST_MEDIA = (file, toAttach) -> toAttach == null
			|| !tuneFileFilter.accept(file) && file.getName().compareTo(toAttach.getName()) < 0;

	/**
	 * Auto-start commands.
	 */
	private static final String LOAD_8_1_RUN = "LOAD\"%s\",8,1\rRUN\r", LOAD_RUN = "LOAD\rRUN\r";

	private Player player;

	public Convenience(Player player) {
		this.player = player;
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
	 * @throws IOException  image read error
	 * @throws SidTuneError invalid tune
	 */
	public boolean autostart(File file, BiPredicate<File, File> isMediaToAttach, String dirEntry)
			throws IOException, SidTuneError {
		player.getC64().ejectCartridge();
		File tmpDir = player.getConfig().getSidplay2Section().getTmpDir();
		boolean fileIsModule = cartFileFilter.accept(file);
		TFile zip = new TFile(file);
		File toAttach = null;
		if (zip.exists()) {
			if (zip.isArchive()) {
				// uncompress zip
				TFile.cp_rp(zip, tmpDir, TArchiveDetector.ALL);
				// search media file to attach
				toAttach = getToAttach(tmpDir, zip, isMediaToAttach, null, true, fileIsModule);
				TFile.rm_r(zip);
			} else if (file.getName().toLowerCase(Locale.ENGLISH).endsWith(".gz")) {
				File dst = new File(file.getParentFile(), PathUtils.getFilenameWithoutSuffix(file.getName()));
				try (InputStream is = new GZIPInputStream(ZipFileUtils.newFileInputStream(file))) {
					TFile.cp(is, dst);
				}
				toAttach = getToAttach(file.getParentFile(), file.getParentFile(), isMediaToAttach, null, true,
						fileIsModule);
				TFile.rm_r(zip);
			} else if (file.getName().toLowerCase(Locale.ENGLISH).endsWith("7z")) {
				Extract7ZipUtil extract7Zip = new Extract7ZipUtil(zip, tmpDir);
				extract7Zip.extract();
				toAttach = getToAttach(tmpDir, extract7Zip.getZipFile(), isMediaToAttach, null, true, fileIsModule);
				TFile.rm_r(zip);
			} else if (zip.isEntry()) {
				// uncompress zip entry
				File zipEntry = new File(tmpDir, zip.getName());
				zipEntry.deleteOnExit();
				TFile.cp_rp(zip, zipEntry, TArchiveDetector.ALL);
				// search media file to attach
				getToAttach(tmpDir, zipEntry.getParentFile(), (f1, f2) -> false, null, false, fileIsModule);
				toAttach = zipEntry;
			} else if (isSupportedMedia(file)) {
				getToAttach(file.getParentFile(), file.getParentFile(), (f1, f2) -> false, null, false, fileIsModule);
				toAttach = file;
			}
		}
		if (toAttach != null) {
			if (tuneFileFilter.accept(toAttach)) {
				player.play(SidTune.load(toAttach));
				return true;
			} else if (diskFileFilter.accept(toAttach)) {
				player.insertDisk(toAttach);
				player.resetC64(String.format(LOAD_8_1_RUN, dirEntry != null ? toFilename(dirEntry) : "*"));
				return true;
			} else if (tapeFileFilter.accept(toAttach)) {
				player.insertTape(toAttach);
				player.resetC64(LOAD_RUN);
				return true;
			} else if (toAttach.getName().toLowerCase(Locale.ENGLISH).endsWith(".reu")) {
				try (InputStream is = new ByteArrayInputStream(NUVIE_PLAYER)) {
					player.insertCartridge(CartridgeType.REU, toAttach);
					player.play(SidTune.load("nuvieplayer-v1.0.prg", is));
				}
				return true;
			} else if (cartFileFilter.accept(toAttach)) {
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
			boolean deleteOnExit, boolean fileIsModule) {
		final File[] listFiles = file.listFiles();
		if (listFiles == null) {
			return toAttach;
		}
		final List<File> asList = Arrays.asList(listFiles);
		asList.sort(TOP_LEVEL_FIRST_COMPARATOR);
		for (File member : asList) {
			File memberFile = new File(dir, member.getName());
			if (deleteOnExit) {
				memberFile.deleteOnExit();
			}
			if (memberFile.isFile() && isSupportedMedia(memberFile)) {
				if (!fileIsModule && memberFile.getName().toLowerCase(Locale.ENGLISH).endsWith(".reu")) {
					try {
						player.insertCartridge(CartridgeType.REU, memberFile);
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else if (!fileIsModule && memberFile.getName().toLowerCase(Locale.ENGLISH).endsWith(".crt")) {
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
				File toAttachChild = getToAttach(memberFile, new TFile(memberFile), mediaTester, toAttach, deleteOnExit,
						fileIsModule);
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
		return cartFileFilter.accept(file) || tuneFileFilter.accept(file) || diskFileFilter.accept(file)
				|| tapeFileFilter.accept(file);
	}

}
