package ui.common.fileextension;

import static java.util.Arrays.asList;
import static libsidutils.PathUtils.addUpperCase;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public interface DiskFileExtensions {

	List<String> EXTENSIONS = addUpperCase(asList("*.d64", "*.g64", "*.nib"));

	String DESCRIPTION = "C64 Disks";

	static List<String> STD_EXTENSIONS() {
		return EXTENSIONS.stream().filter(ext -> ext.toLowerCase(Locale.ENGLISH).startsWith("*.d"))
				.collect(Collectors.toList());
	}
}
