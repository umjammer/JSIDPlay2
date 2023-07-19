package libsidutils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

@SuppressWarnings("unchecked")
public class ZipFileUtils {

	private static Constructor<? extends InputStream> INPUT_STREAM;
	private static Constructor<File> FILE;

	static {
		try {
			// standard java.io functionality
			FILE = File.class.getConstructor(File.class, String.class);
			INPUT_STREAM = FileInputStream.class.getConstructor(File.class);
			// support for files contained in a ZIP (optionally in the classpath)
			FILE = (Constructor<File>) Class.forName("net.java.truevfs.access.TFile").getConstructor(File.class,
					String.class);
			INPUT_STREAM = (Constructor<InputStream>) Class.forName("net.java.truevfs.access.TFileInputStream")
					.getConstructor(File.class);
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException e) {
		}
	}

	public static File newFile(File parent, String child) {
		try {
			return FILE.newInstance(parent, child);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			return new File(parent, child);
		}
	}

	public static InputStream newFileInputStream(File file) throws FileNotFoundException {
		try {
			return INPUT_STREAM.newInstance(file);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
				| InvocationTargetException e) {
			return new FileInputStream(file);
		}
	}

}
