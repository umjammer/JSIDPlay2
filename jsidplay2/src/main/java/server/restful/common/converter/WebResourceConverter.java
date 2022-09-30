package server.restful.common.converter;

import java.io.InputStream;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

public final class WebResourceConverter implements IStringConverter<InputStream> {

	@Override
	public InputStream convert(String value) {
		InputStream resourceAsStream = WebResourceConverter.class.getResourceAsStream("/server/restful/webapp" + value);
		if (resourceAsStream == null) {
			throw new ParameterException("Invalid Resource (" + value + " not found)!");
		}
		return resourceAsStream;
	}
}
