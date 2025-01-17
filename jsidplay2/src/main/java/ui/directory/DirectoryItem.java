package ui.directory;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import libsidutils.directory.DirEntry;

public class DirectoryItem {
	private ObjectProperty<DirEntry> dirEntry = new SimpleObjectProperty<>();
	private String text;

	public DirEntry getDirEntry() {
		return dirEntry.get();
	}

	public void setDirEntry(DirEntry value) {
		this.dirEntry.set(value);
	}

	public void setText(String text) {
		this.text = text;
	}

	public String getText() {
		return text;
	}
}
