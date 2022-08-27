package ui.directory;

import static libsidutils.C64Font.TRUE_TYPE_FONT_BIG;
import static libsidutils.C64Font.TRUE_TYPE_FONT_INVERSE_BIG;
import static libsidutils.C64Font.TRUE_TYPE_FONT_INVERSE_SMALL;
import static libsidutils.C64Font.TRUE_TYPE_FONT_SMALL;
import static libsidutils.C64Font.petsciiToFont;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Locale;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Priority;
import libsidplay.sidtune.SidTuneError;
import libsidutils.directory.CartridgeDirectory;
import libsidutils.directory.DirEntry;
import libsidutils.directory.DiskDirectory;
import libsidutils.directory.T64Directory;
import libsidutils.directory.TuneDirectory;
import sidplay.Player;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.common.filefilter.DiskFileFilter;
import ui.common.filefilter.TuneFileFilter;
import ui.entities.config.SidPlay2Section;

public class Directory extends C64VBox implements UIPart {

	private static TuneFileFilter tuneFilter = new TuneFileFilter();

	private static DiskFileFilter diskFilter = new DiskFileFilter();

	@FXML
	protected TableView<DirectoryItem> directory;

	@FXML
	private TableColumn<DirectoryItem, String> dirColumn;

	@FXML
	private ContextMenu contentEntryContextMenu;

	@FXML
	private MenuItem startMenu;

	private ObservableList<DirectoryItem> directoryEntries;

	private ObjectProperty<String> autoStartFileProperty = new SimpleObjectProperty<>();

	private int fontSet = TRUE_TYPE_FONT_BIG;
	private int fontSetHeader = TRUE_TYPE_FONT_INVERSE_BIG;

	private File previewFile;

	public Directory() {
		super();
	}

	public Directory(C64Window window, Player player) {
		super(window, player);
	}

	@FXML
	@Override
	protected void initialize() {
		dirColumn.prefWidthProperty().bind(directory.widthProperty());
		directoryEntries = FXCollections.<DirectoryItem>observableArrayList();
		directory.setItems(directoryEntries);
		directory.setOnKeyPressed(event -> {
			DirectoryItem selectedItem = directory.getSelectionModel().getSelectedItem();
			if (event.getCode() == KeyCode.ENTER && selectedItem != null) {
				autoStartProgram();
			}
		});
		directory.setOnMousePressed(event -> {
			if (event.isPrimaryButtonDown() && event.getClickCount() > 1) {
				autoStartProgram();
			}
		});
		contentEntryContextMenu.setOnShown(event -> {
			DirectoryItem directoryItem = directory.getSelectionModel().getSelectedItem();
			startMenu.setDisable(directoryItem == null || directoryItem.getDirEntry() == null);
		});
		if (directory.getUserData() != null) {
			// JavaFX Preview, only
			loadPreview(new File(directory.getUserData().toString()));
			doSwitchFont();
		}
		setVgrow(directory, Priority.ALWAYS);
	}

	@FXML
	private void doSwitchFont() {
		fontSet = fontSet == TRUE_TYPE_FONT_BIG ? TRUE_TYPE_FONT_SMALL : TRUE_TYPE_FONT_BIG;
		fontSetHeader = fontSetHeader == TRUE_TYPE_FONT_INVERSE_BIG ? TRUE_TYPE_FONT_INVERSE_SMALL
				: TRUE_TYPE_FONT_INVERSE_BIG;
		loadPreview(previewFile);
	}

	@FXML
	private void autoStartProgram() {
		DirectoryItem dirItem = directory.getSelectionModel().getSelectedItem();
		if (dirItem == null) {
			return;
		}
		DirEntry dirEntry = dirItem.getDirEntry();
		if (dirEntry != null) {
			autoStartFileProperty.set(dirEntry.getDirectoryLine());
		}
	}

	public ObjectProperty<String> getAutoStartFileProperty() {
		return autoStartFileProperty;
	}

	public void loadPreview(File previewFile) {
		SidPlay2Section sidplay2Section = util.getConfig().getSidplay2Section();
		if (previewFile == null) {
			return;
		}
		this.previewFile = previewFile;
		directoryEntries.clear();
		try {
			libsidutils.directory.Directory dir = createDirectory(sidplay2Section.getHvsc(), previewFile);
			if (dir != null) {
				// Print directory title/id
				DirectoryItem headerItem = new DirectoryItem();
				headerItem.setText(petsciiToFont(dir.getTitle(), fontSetHeader));
				directoryEntries.add(headerItem);
				Collection<DirEntry> dirEntries = dir.getDirEntries();
				// Print directory entries
				for (DirEntry dirEntry : dirEntries) {
					DirectoryItem dirItem = new DirectoryItem();
					dirItem.setText(petsciiToFont(dirEntry.getDirectoryLine(), fontSet));
					dirItem.setDirEntry(dirEntry);
					directoryEntries.add(dirItem);
				}
				// Print directory result
				if (dir.getStatusLine() != null) {
					DirectoryItem dirItem = new DirectoryItem();
					dirItem.setText(petsciiToFont(dir.getStatusLine(), fontSet));
					directoryEntries.add(dirItem);
				}
			} else {
				throw new IOException();
			}
		} catch (IOException | SidTuneError ioE) {
			DirectoryItem dirItem = new DirectoryItem();
			dirItem.setText(petsciiToFont("SORRY, NO PREVIEW AVAILABLE!", TRUE_TYPE_FONT_BIG));
			directoryEntries.add(dirItem);
		}
		DirectoryItem dirItem = new DirectoryItem();
		dirItem.setText(petsciiToFont("READY.", TRUE_TYPE_FONT_BIG));
		directoryEntries.add(dirItem);
	}

	public void clear() {
		directoryEntries.clear();

	}

	private libsidutils.directory.Directory createDirectory(File hvscRoot, final File file)
			throws IOException, SidTuneError {
		if (diskFilter.accept(file)) {
			return new DiskDirectory(file);
		} else if (file.getName().toLowerCase(Locale.ENGLISH).endsWith(".t64")) {
			return new T64Directory(file);
		} else if (file.getName().toLowerCase(Locale.ENGLISH).endsWith(".crt")) {
			return new CartridgeDirectory(file);
		} else if (tuneFilter.accept(file)) {
			return new TuneDirectory(hvscRoot, file);
		}
		return null;
	}

}
