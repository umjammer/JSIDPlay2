package libsidutils.directory;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import libsidplay.sidtune.SidTuneError;
import libsidplay.sidtune.T64;
import libsidplay.sidtune.T64.T64Entry;

/**
 * Pseudo directory to display tape contents.
 * 
 * @author ken
 *
 */
public class T64Directory extends Directory {

	public T64Directory(File file) throws IOException {
		final T64 t64 = new T64();

		// Load T64
		byte[] data = new byte[(int) file.length()];
		try (DataInputStream fd = new DataInputStream(new FileInputStream(file))) {
			fd.readFully(data);
			// Get title
			byte[] diskName = new byte[32];
			System.arraycopy(data, 0, diskName, 0, diskName.length);
			title = diskName;

			int totalEntries = (data[35] & 0xff) << 8 | data[34] & 0xff;
			for (int entryNum = 1; entryNum <= totalEntries; entryNum++) {
				try {
					final T64Entry entry = t64.getEntry(data, entryNum);
					dirEntries.add(new DirEntry(entry.c64dataLen, entry.name, (byte) 0x82) {
						@Override
						public void save(final File autostartFile) throws IOException {
							t64.save(autostartFile, data, entry.programOffset, entry.c64dataLen, entry.loadAddr);
						}
					});
				} catch (SidTuneError e) {
				}
			}
		}
	}

}
