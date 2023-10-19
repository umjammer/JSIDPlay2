package server.restful.common.converter;

import java.io.InputStream;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;

public final class WebJarsResourceConverter extends BaseConverter<InputStream> {

	public WebJarsResourceConverter(String optionName) {
		super(optionName);
	}

	@Override
	public InputStream convert(String value) {
		InputStream resourceAsStream = getClass().getResourceAsStream("/META-INF/resources/webjars" + value);
		if (resourceAsStream == null) {
			throw new ParameterException(
					getErrorString(value, "an internal web jars (/META-INF/resources/webjars/*) as an InputStream)"));
		}
		return resourceAsStream;
	}
}
