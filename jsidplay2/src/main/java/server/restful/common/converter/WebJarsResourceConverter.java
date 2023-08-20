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
		InputStream resourceAsStream = WebJarsResourceConverter.class
				.getResourceAsStream("/META-INF/resources/webjars" + value);
		if (resourceAsStream == null) {
			throw new ParameterException(getErrorString(value,
					"an internal web resource (/META-INF/resources/webjars/* as an InputStream)"));
		}
		return resourceAsStream;
	}
}
