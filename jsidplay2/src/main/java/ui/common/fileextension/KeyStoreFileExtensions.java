package ui.common.fileextension;

import static java.util.Arrays.asList;
import static ui.common.fileextension.TuneFileExtensions.addUpperCase;

import java.util.List;

public interface KeyStoreFileExtensions {

	List<String> EXTENSIONS = addUpperCase(asList("*.ks"));

	String DESCRIPTION = "Keystore file (*.ks)";
}
