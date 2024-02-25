package server.restful.common.validator;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class MaxResultsValidator implements IParameterValidator {

	private static final int MAX_RESULTS = 50000;

	@Override
	public void validate(String name, String value) throws ParameterException {
		try {
			int n = Integer.parseInt(value);
			if (n < 0 || n > MAX_RESULTS) {
				throw new ParameterException(
						"Invalid " + name + " value, expected 0.." + MAX_RESULTS + " (found " + value + ")");
			}
		} catch (NumberFormatException e) {
			throw new ParameterException("Parameter " + name + " should be an int (found " + value + ")");
		}
	}

}
