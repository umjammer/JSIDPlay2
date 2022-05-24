package sidplay.ini.validator;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class LatencyTimerValidator implements IParameterValidator {
	@Override
	public void validate(String name, String value) throws ParameterException {
		int n = Short.parseShort(value);
		if (n < 2 || n > 255) {
			throw new ParameterException("Invalid " + name + " value, expected 2ms..255ms (found " + value + ")");
		}
	}
}