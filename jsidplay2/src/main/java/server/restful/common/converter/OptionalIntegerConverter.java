package server.restful.common.converter;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

public class OptionalIntegerConverter implements IStringConverter<Integer> {

	@Override
	public Integer convert(String value) {
		try {
			if ("null".equals(value)) {
				return null;
			}
			return Integer.parseInt(value);
		} catch (NumberFormatException ex) {
			throw new ParameterException("Invalid Integer, expected int value (found " + value + ")");
		}
	}
}
