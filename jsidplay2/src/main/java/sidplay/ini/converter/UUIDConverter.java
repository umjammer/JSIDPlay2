package sidplay.ini.converter;

import java.util.UUID;

import com.beust.jcommander.IStringConverter;

public final class UUIDConverter implements IStringConverter<UUID> {

	@Override
	public UUID convert(String value) {
		try {
			return UUID.fromString(value);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
