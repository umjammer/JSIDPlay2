package ui.common.filefilter;

import static java.util.Arrays.asList;
import static ui.common.filefilter.TuneFileFilter.addUpperCase;

import java.util.List;

public interface CartFileExtensions {
	List<String> EXTENSIONS = addUpperCase(asList("*.reu", "*.ima", "*.crt", "*.img", "*.zip"));

	String DESCRIPTION = "C64 Cartridges";

}
