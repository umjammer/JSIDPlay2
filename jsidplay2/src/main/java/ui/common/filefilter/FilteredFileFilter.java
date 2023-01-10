package ui.common.filefilter;

import java.io.File;
import java.io.FileFilter;
import java.util.Locale;

public class FilteredFileFilter implements FileFilter {

	private String filter;

	public FilteredFileFilter(String filter) {
		this.filter = filter;
	}

	@Override
	public boolean accept(File file) {
		if (file.isDirectory() && file.getName().endsWith(".tmp")) {
			return false;
		}
		return file.isDirectory() || filter == null || file.getName().toLowerCase(Locale.US).matches(filter);
	}

}
