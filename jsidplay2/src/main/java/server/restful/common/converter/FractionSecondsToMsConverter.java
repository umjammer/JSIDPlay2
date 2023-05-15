package server.restful.common.converter;

import java.util.Optional;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;

public final class FractionSecondsToMsConverter extends BaseConverter<Long> {

	public FractionSecondsToMsConverter(String optionName) {
		super(optionName);
	}

	@Override
	public Long convert(String value) {
		try {
			return Optional.ofNullable(value).map(Float::parseFloat).map(seconds -> (long) (seconds * 1000))
					.orElse(null);
		} catch (NumberFormatException e) {
			throw new ParameterException(getErrorString(value, "a float as seconds"));
		}
	}
}
