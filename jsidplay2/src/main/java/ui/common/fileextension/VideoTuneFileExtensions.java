package ui.common.fileextension;

import static java.util.Arrays.asList;
import static libsidutils.IOUtils.addUpperCase;

import java.util.List;

public interface VideoTuneFileExtensions {

	List<String> EXTENSIONS = addUpperCase(asList("*.c64", "*.prg", "*.p00"));

	String DESCRIPTION = "C64 Tunes";
}
