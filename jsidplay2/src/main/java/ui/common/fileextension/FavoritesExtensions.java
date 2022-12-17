package ui.common.fileextension;

import static java.util.Arrays.asList;
import static libsidutils.PathUtils.addUpperCase;

import java.util.List;

public interface FavoritesExtensions {

	List<String> EXTENSIONS = addUpperCase(asList("*.js2"));

	String DESCRIPTION = "C64 Favorites";
}
