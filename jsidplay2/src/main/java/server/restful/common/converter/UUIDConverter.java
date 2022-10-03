package server.restful.common.converter;

import java.util.UUID;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;

public final class UUIDConverter extends BaseConverter<UUID> {

	public UUIDConverter(String optionName) {
		super(optionName);
	}

	@Override
	public UUID convert(String value) {
		try {
			return UUID.fromString(value);
		} catch (IllegalArgumentException e) {
			throw new ParameterException(getErrorString(value, "a UUID"));
		}
	}
}
