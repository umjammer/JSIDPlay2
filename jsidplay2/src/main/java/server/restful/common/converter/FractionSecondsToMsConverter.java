package server.restful.common.converter;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
			return Optional.ofNullable(value).map(BigDecimal::new)
					.map(seconds -> seconds.setScale(3, RoundingMode.DOWN).movePointRight(3).longValue()).orElse(0L);
		} catch (NumberFormatException e) {
			throw new ParameterException(getErrorString(value, "a time in seconds (pattern: ss.SSS)"));
		}
	}
}
