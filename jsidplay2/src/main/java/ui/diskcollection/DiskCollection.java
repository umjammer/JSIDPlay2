package ui.diskcollection;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.stage.DirectoryChooser;
import libsidplay.sidtune.SidTuneError;
import libsidutils.IOUtils;
import net.java.truevfs.access.TFile;
import net.java.truevfs.access.TFileInputStream;
import net.java.truevfs.access.TVFS;
import net.java.truevfs.kernel.spec.FsSyncException;
import sidplay.Player;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.Convenience;
import ui.common.UIPart;
import ui.common.download.DownloadThread;
import ui.common.download.ProgressListener;
import ui.common.filefilter.DiskFileFilter;
import ui.common.filefilter.DocsFileFilter;
import ui.common.filefilter.ScreenshotFileFilter;
import ui.common.filefilter.TapeFileFilter;
import ui.common.filefilter.TuneFileFilter;
import ui.common.util.DesktopUtil;
import ui.directory.Directory;
import ui.entities.config.SidPlay2Section;

public class DiskCollection extends C64VBox implements UIPart {

	private static final String HVMEC_DATA = "DATA";
	private static final String HVMEC_CONTROL = "CONTROL";

	@FXML
	private CheckBox autoConfiguration;
	@FXML
	private Directory directory;
	@FXML
	private ImageView screenshot;
	@FXML
	private TreeView<File> fileBrowser;
	@FXML
	private ContextMenu contextMenu;
	@FXML
	private MenuItem start, attachDisk;
	@FXML
	private TextField collectionDir;

	private Convenience convenience;
	private ObjectProperty<DiskCollectionType> type;

	public DiskCollectionType getType() {
		return type.get();
	}

	public void setType(DiskCollectionType type) {
		this.type.set(type);
	}

	private String downloadUrl;

	private final FileFilter screenshotsFileFilter = new ScreenshotFileFilter();
	protected final FileFilter diskFileFilter = new DiskFileFilter();
	private final FileFilter fileBrowserFileFilter = new FileFilter() {

		private final TuneFileFilter tuneFileFilter = new TuneFileFilter();
		private final TapeFileFilter tapeFileFilter = new TapeFileFilter();
		private final DocsFileFilter docsFileFilter = new DocsFileFilter();

		@Override
		public boolean accept(File file) {
			if (getType() == DiskCollectionType.HVMEC && file.isDirectory() && file.getName().equals(HVMEC_CONTROL)) {
				return false;
			}
			return file.getName().toLowerCase(Locale.ENGLISH).endsWith(".zip")
					|| file.getName().toLowerCase(Locale.ENGLISH).endsWith(".gz") || tuneFileFilter.accept(file)
					|| diskFileFilter.accept(file) || tapeFileFilter.accept(file) || docsFileFilter.accept(file);
		}
	};

	public DiskCollection() {
		super();
	}

	public DiskCollection(C64Window window, Player player) {
		super(window, player);
	}

	@FXML
	@Override
	protected void initialize() {
		convenience = new Convenience(util.getPlayer());
		directory.setPrefHeight(Double.MAX_VALUE);
		directory.getAutoStartFileProperty().addListener(
				observable -> attachAndRunDemo(fileBrowser.getSelectionModel().getSelectedItem().getValue(),
						directory.getAutoStartFileProperty().get()));
		fileBrowser.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue != null && newValue.getValue().isFile()) {
				File file = newValue.getValue();
				showScreenshot(file);
				try {
					directory.loadPreview(IOUtils.extract(util.getConfig().getSidplay2Section().getTmpDir(), file));
				} catch (Exception e) {
					System.err.println(String.format("Cannot insert media file '%s'.", file.getAbsolutePath()));
				}
			}
		});
		fileBrowser.setOnKeyPressed(event -> {
			TreeItem<File> selectedItem = fileBrowser.getSelectionModel().getSelectedItem();
			if (event.getCode() == KeyCode.ENTER && selectedItem != null) {
				if (selectedItem.getValue().isFile()) {
					File file = selectedItem.getValue();
					if (file.isFile()) {
						attachAndRunDemo(selectedItem.getValue(), null);
					}
				}
			}
		});
		fileBrowser.setOnMousePressed(event -> {
			final TreeItem<File> selectedItem = fileBrowser.getSelectionModel().getSelectedItem();
			if (selectedItem != null && selectedItem.getValue().isFile() && event.isPrimaryButtonDown()
					&& event.getClickCount() > 1) {
				attachAndRunDemo(fileBrowser.getSelectionModel().getSelectedItem().getValue(), null);
			}
		});
		contextMenu.setOnShown(event -> {
			TreeItem<File> selectedItem = fileBrowser.getSelectionModel().getSelectedItem();
			boolean disable = selectedItem == null || !selectedItem.getValue().isFile();
			start.setDisable(disable);
			attachDisk.setDisable(disable);
		});
		type = new SimpleObjectProperty<>();
		type.addListener((observable, oldValue, newValue) -> {
			Platform.runLater(() -> {
				File initialRoot;
				switch (getType()) {
				case HVMEC:
					this.downloadUrl = util.getConfig().getOnlineSection().getHvmecUrl();
					initialRoot = util.getConfig().getSidplay2Section().getHVMEC();
					break;

				case DEMOS:
					this.downloadUrl = util.getConfig().getOnlineSection().getDemosUrl();
					initialRoot = util.getConfig().getSidplay2Section().getDemos();
					break;

				case MAGS:
					this.downloadUrl = util.getConfig().getOnlineSection().getMagazinesUrl();
					initialRoot = util.getConfig().getSidplay2Section().getMags();
					break;

				default:
					throw new RuntimeException("Illegal disk collection type : " + type);
				}
				if (initialRoot != null) {
					setRootFile(initialRoot);
				}
			});
		});
	}

	@Override
	public void doClose() {
		if (fileBrowser.getRoot() != null && fileBrowser.getRoot().getValue() instanceof TFile) {
			TFile tf = (TFile) fileBrowser.getRoot().getValue();
			try {
				TVFS.umount(tf);
			} catch (FsSyncException e) {
				e.printStackTrace();
			}
		}
	}

	@FXML
	private void doAutoConfiguration() {
		if (autoConfiguration.isSelected()) {
			autoConfiguration.setDisable(true);
			try {
				DownloadThread downloadThread = new DownloadThread(util.getConfig(),
						new ProgressListener(util, fileBrowser.getScene()) {

							@Override
							public void downloaded(final File downloadedFile) {
								Platform.runLater(() -> {
									autoConfiguration.setDisable(false);
									if (downloadedFile != null) {
										setRootFile(downloadedFile);
									}
								});
							}
						}, new URI(downloadUrl).toURL(), true);
				downloadThread.start();
			} catch (MalformedURLException | URISyntaxException e2) {
				e2.printStackTrace();
			}
		}
	}

	@FXML
	private void attachDisk() {
		File file = fileBrowser.getSelectionModel().getSelectedItem().getValue();
		try {
			File extractedFile = IOUtils.extract(util.getConfig().getSidplay2Section().getTmpDir(), file);
			util.getPlayer().insertDisk(extractedFile);
		} catch (IOException e) {
			System.err.println(String.format("Cannot insert media file '%s'.", file.getAbsolutePath()));
		}
	}

	@FXML
	private void start() {
		attachAndRunDemo(fileBrowser.getSelectionModel().getSelectedItem().getValue(), null);
	}

	@FXML
	private void doBrowse() {
		DirectoryChooser fileDialog = new DirectoryChooser();
		SidPlay2Section sidplay2 = util.getConfig().getSidplay2Section();
		fileDialog.setInitialDirectory(sidplay2.getLastDirectory());
		File directory = fileDialog.showDialog(autoConfiguration.getScene().getWindow());
		if (directory != null) {
			sidplay2.setLastDirectory(directory);
			setRootFile(directory);
		}
	}

	protected void setRootFile(final File rootFile) {
		if (rootFile.exists()) {
			collectionDir.setText(rootFile.getAbsolutePath());

			final File theRootFile = new TFile(rootFile);
			fileBrowser.setRoot(new DiskCollectionTreeItem(theRootFile, theRootFile, fileBrowserFileFilter));

			if (getType() == DiskCollectionType.HVMEC) {
				util.getConfig().getSidplay2Section().setHVMEC(theRootFile);
			} else if (getType() == DiskCollectionType.DEMOS) {
				util.getConfig().getSidplay2Section().setDemos(theRootFile);
			} else if (getType() == DiskCollectionType.MAGS) {
				util.getConfig().getSidplay2Section().setMags(theRootFile);
			}
		}
	}

	protected void attachAndRunDemo(File file, final String dirEntry) {
		try {
			File extractedFile = IOUtils.extract(util.getConfig().getSidplay2Section().getTmpDir(), file);
			if (file.getName().toLowerCase(Locale.ENGLISH).endsWith(".pdf")) {
				DesktopUtil.open(extractedFile);
			} else {
				if (convenience.autostart(extractedFile, Convenience.LEXICALLY_FIRST_MEDIA, dirEntry, true)
						.isSuccess()) {
					util.setPlayingTab(this);
				}
			}
		} catch (IOException | SidTuneError e) {
			System.err.println(String.format("Cannot insert media file '%s'.", file.getAbsolutePath()));
		}
	}

	protected void showScreenshot(final File file) {
		Image image = createImage(file);
		if (image != null) {
			screenshot.setImage(image);
		}
	}

	private Image createImage(final File file) {
		File screenshot = findScreenshot(file);
		if (screenshot != null && screenshot.exists()) {
			try (InputStream is = new TFileInputStream(screenshot)) {
				return new Image(is);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	private File findScreenshot(File file) {
		TreeItem<File> rootItem = fileBrowser.getRoot();
		if (rootItem == null) {
			return null;
		}
		String parentPath = getType() == DiskCollectionType.HVMEC
				? file.getParentFile().getPath().replace(HVMEC_DATA, HVMEC_CONTROL)
				: file.getParentFile().getPath();
		List<File> parentFiles = IOUtils.getFiles(parentPath, rootItem.getValue(), null);
		if (parentFiles.size() > 0) {
			File parentFile = parentFiles.get(parentFiles.size() - 1);
			final File[] listFiles = parentFile.listFiles();
			if (listFiles == null) {
				return null;
			}
			for (File photoFile : listFiles) {
				if (!photoFile.isDirectory() && screenshotsFileFilter.accept(photoFile)) {
					return photoFile;
				}
			}
		}
		return null;
	}

}
