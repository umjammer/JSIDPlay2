package server.restful.common.validator;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class JoystickNumberValidator implements IParameterValidator {
	@Override
	public void validate(String name, String value) throws ParameterException {
		int n = Integer.parseInt(value);
		if (n < 0 || n > 255) {
			throw new ParameterException("Invalid " + name + " value, expected 0..255 (found " + value + ")");
		}
	}
}