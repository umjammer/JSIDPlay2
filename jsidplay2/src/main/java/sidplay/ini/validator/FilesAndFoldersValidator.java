package sidplay.ini.validator;

import java.io.File;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class FilesAndFoldersValidator implements IParameterValidator {

	@Override
	public void validate(String name, String value) throws ParameterException {
		if (!new File(value).exists()) {
			throw new ParameterException("File or Folder " + value + " does not exist!");
		}

	}
}
