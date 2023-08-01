package libsidutils.directory;

import static libsidutils.CBMCodeUtils.iso88591ToScreenRam;
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
import libsidplay.sidtune.SidTune.Clock;
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

	private static final int MAXLEN_FILENAME = 16;

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

		if (author != null && author.length() > 0) {
			dirEntries.add(toDirEntry(author));
		}
		if (released != null && released.length() > 0) {
			dirEntries.add(toDirEntry(released));
		}
		String playerId = tune.identify().stream().collect(Collectors.joining(","));
		if (playerId != null && playerId.length() > 0) {
			dirEntries.add(toDirEntry(playerId));
		}
		dirEntries.add(toDirEntry("" + info.getCompatibility()));
		if (info.getClockSpeed() != Clock.UNKNOWN) {
			dirEntries.add(toDirEntry("" + info.getClockSpeed()));
		}
		dirEntries.add(toDirEntry("" + tune.getSongSpeed(1)));
		dirEntries.add(toDirEntry("" + info.getAudioTypeString()));
		if (info.getSIDModel(0) != Model.UNKNOWN) {
			dirEntries.add(toDirEntry("" + info.getSIDModel(0)));
		}
		if (info.getSIDModel(1) != Model.UNKNOWN) {
			dirEntries.add(toDirEntry("" + info.getSIDModel(1)));
		}
		if (info.getSIDModel(2) != Model.UNKNOWN) {
			dirEntries.add(toDirEntry("" + info.getSIDModel(2)));
		}
		dirEntries.add(toDirEntry(String.format("$%04X", info.getSIDChipBase(0))));
		if (info.getSIDChipBase(1) != 0) {
			dirEntries.add(toDirEntry(String.format("$%04X", info.getSIDChipBase(1))));
		}
		if (info.getSIDChipBase(2) != 0) {
			dirEntries.add(toDirEntry(String.format("$%04X", info.getSIDChipBase(2))));
		}
		dirEntries.add(toDirEntry(lengthFnct.getAsDouble() + "s"));
		dirEntries.add(toDirEntry("DRV_ADDR=" + info.getDeterminedDriverAddr()));
		dirEntries.add(toDirEntry("LOAD_ADDR=" + info.getLoadAddr()));
		dirEntries.add(toDirEntry("LOAD_LGTH=" + info.getC64dataLen()));
		dirEntries.add(toDirEntry("INIT_ADDR=" + info.getInitAddr()));
		dirEntries.add(toDirEntry("PLY_ADDR=" + info.getPlayAddr()));
		dirEntries.add(toDirEntry("SIZE_KB=" + (tuneFile.length() >> 10)));
		dirEntries.add(toDirEntry("SIZE_B=" + tuneFile.length()));
		dirEntries.add(toDirEntry("RELOC_PAGE=" + info.getRelocStartPage()));
		dirEntries.add(toDirEntry("RELOC_PAGES=" + info.getRelocPages()));
		dirEntries.add(toDirEntry(
				"" + Instant.ofEpochMilli(tuneFile.lastModified()).atZone(ZoneId.systemDefault()).toLocalDateTime()));
	}

	private byte[] toTitle(String str) {
		return iso88591ToScreenRam(str.toUpperCase(Locale.US), MAXLEN_FILENAME);
	}

	private byte[] toId(int startSong, int songs) {
		return iso88591ToScreenRam(String.format("%02X/%02X", startSong, songs), 5);
	}

	private DirEntry toDirEntry(String str) {
		return new DirEntry(0, iso88591ToScreenRam(str.toUpperCase(Locale.US), MAXLEN_FILENAME), FILETYPE_NONE);
	}
}
