package sidplay.ini.converter;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;

public class ShortConverter extends BaseConverter<Short> {

	public ShortConverter(String optionName) {
		super(optionName);
	}

	@Override
	public Short convert(String value) {
		try {
			return Short.parseShort(value);
		} catch (NumberFormatException e) {
			throw new ParameterException(getErrorString(value, "a short"));
		}
	}

}
