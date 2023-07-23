package ui.common.fileextension;

import static java.util.Arrays.asList;
import static libsidutils.IOUtils.addUpperCase;

import java.util.List;

public interface MP3TuneFileExtensions {

	List<String> EXTENSIONS = addUpperCase(asList("*.mp3"));

	String DESCRIPTION = "MP3 Tunes";
}
