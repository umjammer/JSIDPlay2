package sidplay.ini.converter;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;

import libsidutils.siddatabase.TimeConverter;

/**
 * Parse mm:ss.SSS (parse time in minutes and seconds and store as seconds)
 */
public final class ParameterTimeConverter extends BaseConverter<Double> {

	private final TimeConverter timeConverter = new TimeConverter();

	public ParameterTimeConverter(String optionName) {
		super(optionName);
	}

	@Override
	public Double convert(String value) {
		double seconds = timeConverter.fromString(value).doubleValue();
		if (seconds == -1) {
			try {
				return Double.parseDouble(value);
			} catch (NumberFormatException e) {
				throw new ParameterException(getErrorString(value, "a time in seconds (pattern: ss or mm:ss.SSS)"));
			}
		}
		return seconds;
	}

}