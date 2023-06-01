package ui.common.filefilter;

import java.io.File;
import java.io.FileFilter;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public class FilteredFileFilter implements FileFilter {

	private Pattern pattern;

	public FilteredFileFilter(String filter) {
		this.pattern = Optional.ofNullable(filter).map(Pattern::compile).orElse(null);
	}

	@Override
	public boolean accept(File file) {
		if (file.isHidden()) {
			return false;
		}
		return file.isDirectory() || pattern == null
				|| pattern.matcher(file.getName().toLowerCase(Locale.ENGLISH)).matches();
	}

}
