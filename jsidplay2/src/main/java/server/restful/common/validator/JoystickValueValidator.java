package server.restful.common.validator;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class JoystickValueValidator implements IParameterValidator {
	@Override
	public void validate(String name, String value) throws ParameterException {
		try {
			int n = Integer.parseInt(value);
			if (n < 0 || n > 255) {
				throw new ParameterException("Invalid " + name + " value, expected 0..255 (found " + value + ")");
			}
		} catch (NumberFormatException e) {
			throw new ParameterException("Parameter " + name + " should be an int (found " + value + ")");
		}
	}
}