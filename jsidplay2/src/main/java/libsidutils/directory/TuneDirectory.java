package libsidutils.directory;

import static libsidutils.Petscii.iso88591ToPetscii;
import static libsidutils.directory.DirEntry.FILETYPE_NONE;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Iterator;
import java.util.Locale;
import java.util.function.DoubleSupplier;
import java.util.stream.Collectors;

import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTune.Model;
import libsidplay.sidtune.SidTuneError;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.siddatabase.SidDatabase;

/**
 * Pseudo directory to display tune contents.
 * 
 * @author ken
 *
 */
public class TuneDirectory extends Directory {

	private static final int MAXLEN_FILENAME = 20;

	public TuneDirectory(File hvscRoot, File tuneFile) throws IOException, SidTuneError {
		SidTune tune = SidTune.load(tuneFile);
		DoubleSupplier lengthFnct = () -> 0;
		if (hvscRoot != null) {
			SidDatabase db = new SidDatabase(hvscRoot);
			lengthFnct = () -> db.getTuneLength(tune);
		}
		SidTuneInfo info = tune.getInfo();
		Iterator<String> descriptionIt = info.getInfoString().iterator();
		String title = tuneFile.getName();
		String author = null;
		String released = null;
		if (descriptionIt.hasNext()) {
			title = descriptionIt.next();
		}
		if (descriptionIt.hasNext()) {
			author = descriptionIt.next();
		}
		if (descriptionIt.hasNext()) {
			released = descriptionIt.next();
		}
		super.title = toTitle(title);
		id = toId(info.getStartSong(), info.getSongs());

		if (author != null) {
			dirEntries.add(toDirEntry(author));
		}
		if (released != null) {
			dirEntries.add(toDirEntry(released));
		}
		String playerId = tune.identify().stream().collect(Collectors.joining(","));
		if (playerId != null && playerId.length() > 0) {
			dirEntries.add(toDirEntry(playerId));
		}
		dirEntries.add(toDirEntry("FORMAT=" + tune.getClass().getSimpleName()));
		dirEntries.add(toDirEntry("CLOCKFREQ=" + info.getClockSpeed()));
		dirEntries.add(toDirEntry("SPEED=" + tune.getSongSpeed(1)));
		dirEntries.add(toDirEntry("SIDMODEL1=" + info.getSIDModel(0)));
		if (info.getSIDModel(1) != Model.UNKNOWN) {
			dirEntries.add(toDirEntry("SIDMODEL2=" + info.getSIDModel(1)));
		}
		if (info.getSIDModel(2) != Model.UNKNOWN) {
			dirEntries.add(toDirEntry("SIDMODEL3=" + info.getSIDModel(2)));
		}
		dirEntries.add(toDirEntry("COMPAT=" + info.getCompatibility()));
		dirEntries.add(toDirEntry("TUNE_LGTH=" + lengthFnct.getAsDouble()));
		dirEntries.add(toDirEntry("AUDIO=" + info.getAudioTypeString()));
		dirEntries.add(toDirEntry("CHIP_BASE1=" + info.getSIDChipBase(0)));
		if (info.getSIDChipBase(1) != 0) {
			dirEntries.add(toDirEntry("CHIP_BASE2=" + info.getSIDChipBase(1)));
		}
		if (info.getSIDChipBase(2) != 0) {
			dirEntries.add(toDirEntry("CHIP_BASE3=" + info.getSIDChipBase(2)));
		}
		dirEntries.add(toDirEntry("DRV_ADDR=" + info.getDeterminedDriverAddr()));
		dirEntries.add(toDirEntry("LOAD_ADDR=" + info.getLoadAddr()));
		dirEntries.add(toDirEntry("LOAD_LGTH=" + info.getC64dataLen()));
		dirEntries.add(toDirEntry("INIT_ADDR=" + info.getInitAddr()));
		dirEntries.add(toDirEntry("PLY_ADDR=" + info.getPlayAddr()));
		dirEntries.add(toDirEntry("FILE_DATE="
				+ Instant.ofEpochMilli(tuneFile.lastModified()).atZone(ZoneId.systemDefault()).toLocalDateTime()));
		dirEntries.add(toDirEntry("SIZE_KB=" + (tuneFile.length() >> 10)));
		dirEntries.add(toDirEntry("SIZE_B=" + tuneFile.length()));
		dirEntries.add(toDirEntry("RELOC_PAGE=" + info.getRelocStartPage()));
		dirEntries.add(toDirEntry("RELOC_PAGES=" + info.getRelocPages()));
	}

	private byte[] toTitle(String str) {
		return iso88591ToPetscii(str.toUpperCase(Locale.US), 16);
	}

	private byte[] toId(int startSong, int songs) {
		return iso88591ToPetscii(String.format("%02X/%02X", startSong, songs), 5);
	}

	private DirEntry toDirEntry(String str) {
		return new DirEntry(0, iso88591ToPetscii(str.toUpperCase(Locale.US), MAXLEN_FILENAME), FILETYPE_NONE);
	}
}
