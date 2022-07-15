package ui.assembly64;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import sidplay.ini.converter.BeanToStringConverter;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ContentEntry {

	private StringProperty idProperty = new SimpleStringProperty();

	public ContentEntry() {
	}

	public String getId() {
		return idProperty.get();
	}

	public void setId(String id) {
		idProperty.set(id);
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}
}
