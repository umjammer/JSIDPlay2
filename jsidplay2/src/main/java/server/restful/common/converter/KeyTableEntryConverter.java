package server.restful.common.converter;

import com.beust.jcommander.ParameterException;
import com.beust.jcommander.converters.BaseConverter;

import libsidplay.components.keyboard.KeyTableEntry;

public final class KeyTableEntryConverter extends BaseConverter<KeyTableEntry> {

	public KeyTableEntryConverter(String optionName) {
		super(optionName);
	}

	@Override
	public KeyTableEntry convert(String value) {
		try {
			return KeyTableEntry.valueOf(KeyTableEntry.class, value);
		} catch (IllegalArgumentException e) {
			throw new ParameterException(getErrorString(value, "a KeyTableEntry"));
		}
	}
}
