package server.restful.common.converter;

import java.io.InputStream;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;

public final class WebResourceConverter extends BaseConverter<InputStream> {

	public WebResourceConverter(String optionName) {
		super(optionName);
	}

	@Override
	public InputStream convert(String value) {
		InputStream resourceAsStream = WebResourceConverter.class.getResourceAsStream("/server/restful/webapp" + value);
		if (resourceAsStream == null) {
			throw new ParameterException(
					getErrorString(value, "an internal web resource (/server/restful/webapp/* as an InputStream)"));
		}
		return resourceAsStream;
	}
}
