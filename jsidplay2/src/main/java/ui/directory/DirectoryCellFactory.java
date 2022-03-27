package ui.directory;

import java.io.InputStream;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.text.Font;
import javafx.util.Callback;
import libsidutils.C64Font;
import ui.JSidPlay2Main;

/**
 * Classify SidDump table cells for CSS styling.
 *
 * @author Ken Händel
 *
 */
public class DirectoryCellFactory
		implements Callback<TableColumn<DirectoryItem, String>, TableCell<DirectoryItem, String>>, C64Font {

	protected static Font c64Font;

	static {
		try {
			InputStream fontStream = JSidPlay2Main.class.getResourceAsStream(FONT_NAME);
			c64Font = Font.loadFont(fontStream, 10);
		} catch (Exception e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	@Override
	public TableCell<DirectoryItem, String> call(final TableColumn<DirectoryItem, String> column) {
		final TableCell<DirectoryItem, String> cell = new TableCell<DirectoryItem, String>() {
			@Override
			public void updateItem(String value, boolean empty) {
				super.updateItem(value, empty);
				if (!empty) {
					setText(value);
				} else {
					setText("");
				}
				setFont(c64Font);
			}
		};
		return cell;
	}
}
