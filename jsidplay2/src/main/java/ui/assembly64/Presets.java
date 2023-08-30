package ui.assembly64;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import sidplay.ini.converter.BeanToStringConverter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Presets {

	public static final Presets ALL = null;

	private String type;

	private Collection<PresetEntry> values;

	public Presets() {
	}

	public String getType() {
		return type;
	}

	public Collection<PresetEntry> getValues() {
		return values;
	}

	@Override
	public String toString() {
		return BeanToStringConverter.toString(this);
	}
}
