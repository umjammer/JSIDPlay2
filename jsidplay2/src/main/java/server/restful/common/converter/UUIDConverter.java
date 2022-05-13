package server.restful.common.converter;

import java.util.UUID;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.ParameterException;

public final class UUIDConverter implements IStringConverter<UUID> {

	@Override
	public UUID convert(String value) {
		try {
			return UUID.fromString(value);
		} catch (IllegalArgumentException e) {
			throw new ParameterException(
					"Invalid UUID, expected pattern xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx (found " + value + ")");
		}
	}
}
