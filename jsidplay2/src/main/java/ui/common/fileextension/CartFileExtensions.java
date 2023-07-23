package ui.common.fileextension;

import static java.util.Arrays.asList;
import static libsidutils.IOUtils.addUpperCase;

import java.util.List;

public interface CartFileExtensions {

	List<String> EXTENSIONS = addUpperCase(asList("*.reu", "*.ima", "*.crt", "*.img"));

	String DESCRIPTION = "C64 Cartridges";
}
