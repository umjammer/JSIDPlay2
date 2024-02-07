package server.restful.common.converter;

import java.util.Locale;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;

public final class LocaleConverter extends BaseConverter<Locale> {

	public LocaleConverter(String optionName) {
		super(optionName);
	}

	@Override
	public Locale convert(String value) {
		try {
			return Locale.forLanguageTag(value);
		} catch (IllegalArgumentException e) {
			throw new ParameterException(getErrorString(value, "a Locale"));
		}
	}
}
