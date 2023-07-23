package ui.common.fileextension;

import static java.util.Arrays.asList;
import static libsidutils.IOUtils.addUpperCase;

import java.util.List;

public interface TapeFileExtensions {

	List<String> EXTENSIONS = addUpperCase(asList("*.tap", "*.t64"));

	String DESCRIPTION = "C64 Tapes";
}
