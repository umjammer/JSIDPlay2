package libsidutils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * This class provides general IO utility functions.
 *
 * @author Ken Händel
 */
public class IOUtils {
	/**
	 * Linux, OSX and ZIP entries use slash, Windows uses backslash.
	 */
	private static final Pattern SEPARATOR = Pattern.compile("[/\\\\]");

	private static final int COPY_BUFFER_CHUNK_SIZE = 16 * 1024;

	/**
	 * Create a filename of the given path relative to the collection root dir. <BR>
	 * e.g. "&lt;root&gt;/MUSICIANS/D/Daglish_Ben/Bombo.sid" -&gt;
	 * "/MUSICIANS/D/Daglish_Ben/Bombo.sid"
	 *
	 * @param collectionRoot root file of the path
	 * @param file           file to get the relative path for
	 * @return relative path to the collection root file (empty string, if the path
	 *         is not relative to the collection root file)
	 */
	public static final String getCollectionName(final File collectionRoot, final File file) {
		return createFilename(getFiles(file.getPath(), collectionRoot, null));
	}

	/**
	 * Reverse function of {@link #getFiles(String, File, FileFilter)}.<BR>
	 * e.g. [File(D), File(Daglish_Ben), File(Bombo.sid)] -&gt;
	 * "/MUSICIANS/D/Daglish_Ben/Bombo.sid"
	 *
	 * @param files file list to create a filename for
	 * @return filename filename where each path segment is delimited by a slash.
	 */
	private static final String createFilename(List<File> files) {
		StringBuilder result = new StringBuilder();
		for (File file : files) {
			result.append("/").append(file.getName());
		}
		return result.toString();
	}

	/**
	 * Get file for a given path. The path can be relative to HVSC or CGSC or even
	 * absolute.<BR>
	 * e.g. "&lt;root&gt;/MUSICIANS/D/Daglish_Ben/Bombo.sid" -&gt;
	 * File(/MUSICIANS/D/Daglish_Ben/Bombo.sid)
	 *
	 * @param path     path to get a file for, possible root directory can be either
	 *                 hvscRoot or cgscRoot or none, if absolute
	 * @param hvscRoot root of HVSC
	 * @param cgscRoot root of CGSC
	 * @return file of the path
	 */
	public static final File getFile(String path, File hvscRoot, File cgscRoot) {
		List<File> files = getFiles(path, hvscRoot, null);
		if (files.size() > 0) {
			// relative path name of HVSC?
			return files.get(files.size() - 1);
		}
		files = getFiles(path, cgscRoot, null);
		if (files.size() > 0) {
			// relative path name of CGSC?
			return files.get(files.size() - 1);
		}
		// absolute path name
		return new File(path);
	}

	/**
	 * Get the file list of the given file path. Each entry corresponds to a path
	 * segment. It is sorted from parent to child.<BR>
	 * e.g. "&lt;root&gt;/MUSICIANS/D/Daglish_Ben/Bombo.sid" -&gt; [File(MUSICIANS),
	 * File(D), File(Daglish_Ben), File(Bombo.sid)]
	 *
	 * @param path       file path to get the file list for. Each path segment is
	 *                   delimited by slash or backslash.
	 * @param rootFile   Root file to start. The first path segment must match a
	 *                   direct child of rootPath and so on.
	 * @param fileFilter Files contained in the file filter are visible as child
	 *                   files (null means filter disabled)
	 * @return a file list sorted from the parent file to the child file (empty
	 *         list, if the path is wrong or incomplete)
	 */
	public static final List<File> getFiles(String path, File rootFile, FileFilter fileFilter) {
		if (rootFile == null) {
			return Collections.emptyList();
		}
		String rootPath = rootFile.getPath();
		if (path.startsWith(rootPath)) {
			// remove root folder and separator (not for ZIP file entries)
			path = path.substring(rootPath.length());
			if (path.length() > 0) {
				path = path.substring(1);
			}
		}
		final List<File> pathSegs = new ArrayList<>();
		try (Scanner scanner = new Scanner(path)) {
			scanner.useDelimiter(SEPARATOR);
			nextPathSeg: while (scanner.hasNext()) {
				final String pathSeg = scanner.next();
				File[] childFiles = rootFile.listFiles(fileFilter);
				if (childFiles != null) {
					for (File childFile : childFiles) {
						if (childFile.getName().equalsIgnoreCase(pathSeg)) {
							pathSegs.add(rootFile = childFile);
							continue nextPathSeg;
						}
					}
				}
				return Collections.emptyList();
			}
			return pathSegs;
		}
	}

	/**
	 * Delete directory and contained files and sub-directories.
	 * 
	 * @param directory directory to delete
	 * @throws IOException error during delete
	 */
	public static final void deleteDirectory(File directory) throws IOException {
		Files.walkFileTree(Paths.get(directory.toURI()), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	/**
	 * Strip suffix of a filename.
	 *
	 * @param filename filename to get the suffix for
	 * @return filename without suffix (e.g. "Bombo.sid" -&gt; "Bombo")
	 */
	public static final String getFilenameWithoutSuffix(final String filename) {
		return filename.substring(0, filename.length() - getFilenameSuffix(filename).length());
	}

	/**
	 * Get suffix of a filename.
	 *
	 * @param filename filename to get the suffix for
	 * @return suffix of a filename (e.g. "Bombo.sid" -&gt; ".sid")
	 */
	public static final String getFilenameSuffix(final String filename) {
		int lastIndexOf = filename.lastIndexOf('.');
		return lastIndexOf != -1 ? filename.substring(lastIndexOf) : "";
	}

	/**
	 * Add upper case file extensions to the file extensions list.
	 * 
	 * @param fileExtensions file extensions to add to
	 * @return file extensions with added upper case file extensions
	 */
	public static List<String> addUpperCase(List<String> fileExtensions) {
		List<String> result = new ArrayList<>(fileExtensions);
		fileExtensions.stream().map(fileName -> fileName.toUpperCase(Locale.US)).forEach(result::add);
		return result;
	}

	/**
	 * Get physical size in b, Kb, Mb, Gb or Tb.
	 * 
	 * @param size size to calculate physical size for
	 * @return physical size
	 */
	public static String getFileSize(long size) {
		if (size <= 0) {
			return "0b";
		}
		final String[] units = new String[] { "b", "Kb", "Mb", "Gb", "Tb" };
		int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
		return new DecimalFormat("#,##0.#").format(size / Math.pow(1024, digitGroups)) + units[digitGroups];
	}

	public static void copy(InputStream is, OutputStream os) throws IOException {
		final ReadableByteChannel inputChannel = Channels.newChannel(is);
		final WritableByteChannel outputChannel = Channels.newChannel(os);
		final ByteBuffer buffer = ByteBuffer.allocateDirect(COPY_BUFFER_CHUNK_SIZE);

		while (inputChannel.read(buffer) != -1) {
			((Buffer) buffer).flip();
			outputChannel.write(buffer);
			buffer.compact();
		}
		((Buffer) buffer).flip();
		while (buffer.hasRemaining()) {
			outputChannel.write(buffer);
		}
	}

	public static String convertStreamToString(java.io.InputStream is, String charsetName) {
		return convertStreamToString(is, charsetName, new HashMap<>());
	}

	public static String convertStreamToString(java.io.InputStream is, String charsetName,
			Map<String, String> replacements) {
		try (java.util.Scanner s = new java.util.Scanner(is, charsetName)) {
			s.useDelimiter("\\A");
			String string = s.hasNext() ? s.next() : "";
			List<Entry<String, String>> sortedEntries = replacements.entrySet().stream()
					.sorted((e1, e2) -> -Integer.compare(e1.getKey().length(), e2.getKey().length()))
					.collect(Collectors.toList());
			for (Entry<String, String> replacement : sortedEntries) {
				string = string.replace(replacement.getKey(), replacement.getValue());
			}
			return string;
		}
	}

	public static int readNBytes(InputStream is, byte[] b, int off, int len) throws IOException {
		if (off < 0 || len < 0 || len > b.length - off)
			throw new IndexOutOfBoundsException();
		int n = 0;
		while (n < len) {
			int count = is.read(b, off + n, len - n);
			if (count < 0)
				break;
			n += count;
		}
		return n;
	}

}