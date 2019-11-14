package ui.assembly64;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ContentEntry {

	private StringProperty idProperty = new SimpleStringProperty();
	private StringProperty nameProperty = new SimpleStringProperty();

	public ContentEntry() {
	}

	public String getId() {
		return idProperty.get();
	}

	public void setId(String id) {
		idProperty.set(id);
	}

	public String getName() {
		try {
			return URLDecoder.decode(nameProperty.get(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return nameProperty.get();
		}
	}

	public void setName(String name) {
		nameProperty.set(name);
	}

}
