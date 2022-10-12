package ui.common.filefilter;

import java.io.File;
import java.io.FileFilter;
import java.util.regex.Pattern;

public class UUIDFileFilter implements FileFilter {

	private static final Pattern UUID_PATTERN = Pattern.compile("[a-f0-9]{8}(?:-[a-f0-9]{4}){4}[a-f0-9]{8}");

	@Override
	public boolean accept(File file) {
		return UUID_PATTERN.matcher(file.getName()).matches();
	}

}
