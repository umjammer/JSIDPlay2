package ui.common.fileextension;

import static java.util.Arrays.asList;
import static libsidutils.PathUtils.addUpperCase;

import java.util.List;

public interface RomFileExtensions {

	List<String> EXTENSIONS = addUpperCase(asList("*.bin"));

	String DESCRIPTION = "C64 ROMs";
}
