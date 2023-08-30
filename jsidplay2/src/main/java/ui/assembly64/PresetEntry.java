package ui.assembly64;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import sidplay.ini.converter.BeanToStringConverter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class PresetEntry {

	private Integer id;

	private String aqlKey;

	private String name;

	public Integer getId() {
		return id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public String getAqlKey() {
		return aqlKey;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		return BeanToStringConverter.toString(this);
	}
}
