package ui;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.function.Function;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.converter.IntegerStringConverter;
import libsidplay.C64;
import libsidplay.common.CPUClock;
import libsidplay.common.Engine;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SamplingMethod;
import libsidplay.common.SamplingRate;
import libsidplay.components.c1541.ExtendImagePolicy;
import libsidplay.components.c1541.IExtendImageListener;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.PathUtils;
import netsiddev_builder.NetSIDDevConnection;
import sidplay.Player;
import sidplay.audio.Audio;
import sidplay.audio.JavaSound;
import sidplay.audio.JavaSound.Device;
import ui.common.C64Window;
import ui.common.EnumToString;
import ui.common.TimeToStringConverter;
import ui.common.dialog.YesNoDialog;
import ui.entities.config.AudioSection;
import ui.entities.config.Configuration;
import ui.entities.config.EmulationSection;
import ui.entities.config.SidPlay2Section;

public class JSidPlay2 extends C64Window implements IExtendImageListener, Function<SidTune, String> {

	private static final String CELL_VALUE_OK = "cellValueOk";
	private static final String CELL_VALUE_ERROR = "cellValueError";

	/** Build date calculated from our own modify time */
	private static String DATE = "unknown";

	static {
		try {
			URL us = JSidPlay2Main.class.getResource("/" + JSidPlay2.class.getName().replace('.', '/') + ".class");
			Date date = new Date(us.openConnection().getLastModified());
			DATE = DateFormat.getDateInstance(DateFormat.MEDIUM).format(date);
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	@FXML
	private ComboBox<SamplingMethod> samplingBox;
	@FXML
	private ComboBox<CPUClock> videoStandardBox;
	@FXML
	private ComboBox<Integer> hardsid6581Box, hardsid8580Box;
	@FXML
	private ComboBox<SamplingRate> samplingRateBox;
	@FXML
	private ComboBox<Audio> audioBox;
	@FXML
	private ComboBox<Device> devicesBox;
	@FXML
	private ComboBox<Engine> engineBox;
	@FXML
	private CheckBox enableSldb, singleSong;
	@FXML
	private TextField defaultTime, hostname, port;
	@FXML
	protected RadioButton playMP3, playEmulation;
	@FXML
	ToggleGroup playSourceGroup;
	@FXML
	protected Button volumeButton, mp3Browse;
	@FXML
	protected TabPane tabbedPane;
	@FXML
	private Label hostnameLabel, portLabel, hardsid6581Label, hardsid8580Label;

	private Scene scene;
	private boolean duringInitialization;

	public JSidPlay2(Stage primaryStage, Player player) {
		super(primaryStage, player);
	}

	@FXML
	private void initialize() {
		this.duringInitialization = true;
		getStage().setTitle(util.getBundle().getString("TITLE")
				+ String.format(", %s: %s", util.getBundle().getString("RELEASE"), DATE));

		final ResourceBundle bundle = util.getBundle();
		final Configuration config = util.getConfig();
		final SidPlay2Section sidplay2Section = config.getSidplay2Section();
		final AudioSection audioSection = config.getAudioSection();
		final EmulationSection emulationSection = config.getEmulationSection();

		this.scene = getStage().getScene();

		util.getPlayer().setRecordingFilenameProvider(this);
		util.getPlayer().setExtendImagePolicy(this);

		audioBox.setConverter(new EnumToString<Audio>(bundle));
		audioBox.setItems(FXCollections.<Audio>observableArrayList(Audio.SOUNDCARD, Audio.LIVE_WAV, Audio.LIVE_MP3,
				Audio.COMPARE_MP3));
		audioBox.valueProperty().addListener((obj, o, n) -> {
			mp3Browse.setDisable(!Audio.COMPARE_MP3.equals(n));
			playMP3.setDisable(!Audio.COMPARE_MP3.equals(n));
			playEmulation.setDisable(!Audio.COMPARE_MP3.equals(n));
		});
		audioBox.valueProperty().bindBidirectional(audioSection.audioProperty());

		devicesBox.setItems(JavaSound.getDevices());
		devicesBox.getSelectionModel().select(Math.min(audioSection.getDevice(), devicesBox.getItems().size() - 1));

		samplingBox.setConverter(new EnumToString<SamplingMethod>(bundle));
		samplingBox.setItems(FXCollections.<SamplingMethod>observableArrayList(SamplingMethod.values()));
		samplingBox.valueProperty().bindBidirectional(audioSection.samplingProperty());

		samplingRateBox.setConverter(new EnumToString<SamplingRate>(bundle));
		samplingRateBox.setItems(FXCollections.<SamplingRate>observableArrayList(SamplingRate.values()));
		samplingRateBox.valueProperty().bindBidirectional(audioSection.samplingRateProperty());

		videoStandardBox.setConverter(new EnumToString<CPUClock>(bundle));
		videoStandardBox.valueProperty().bindBidirectional(emulationSection.defaultClockSpeedProperty());
		videoStandardBox.setItems(FXCollections.<CPUClock>observableArrayList(CPUClock.values()));

		hardsid6581Box.valueProperty().bindBidirectional(emulationSection.hardsid6581Property());
		hardsid8580Box.valueProperty().bindBidirectional(emulationSection.hardsid8580Property());

		engineBox.setConverter(new EnumToString<Engine>(bundle));
		engineBox.setItems(FXCollections.<Engine>observableArrayList(Engine.values()));
		engineBox.valueProperty().addListener((obj, o, n) -> {
			hardsid6581Box.setDisable(!Engine.HARDSID.equals(n));
			hardsid8580Box.setDisable(!Engine.HARDSID.equals(n));
			hardsid6581Label.setDisable(!Engine.HARDSID.equals(n));
			hardsid8580Label.setDisable(!Engine.HARDSID.equals(n));
			hostnameLabel.setDisable(!Engine.NETSID.equals(n));
			hostname.setDisable(!Engine.NETSID.equals(n));
			portLabel.setDisable(!Engine.NETSID.equals(n));
			port.setDisable(!Engine.NETSID.equals(n));
		});
		engineBox.valueProperty().bindBidirectional(emulationSection.engineProperty());

		Bindings.bindBidirectional(defaultTime.textProperty(), sidplay2Section.defaultPlayLengthProperty(),
				new TimeToStringConverter());
		sidplay2Section.defaultPlayLengthProperty().addListener((obj, o, n) -> {
			final Tooltip tooltip = new Tooltip();
			defaultTime.getStyleClass().removeAll(CELL_VALUE_OK, CELL_VALUE_ERROR);
			if (n.intValue() != -1) {
				util.getPlayer().getTimer().updateEnd();
				tooltip.setText(util.getBundle().getString("DEFAULT_LENGTH_TIP"));
				defaultTime.setTooltip(tooltip);
				defaultTime.getStyleClass().add(CELL_VALUE_OK);
			} else {
				tooltip.setText(util.getBundle().getString("DEFAULT_LENGTH_FORMAT"));
				defaultTime.setTooltip(tooltip);
				defaultTime.getStyleClass().add(CELL_VALUE_ERROR);
			}
		});

		hostname.textProperty().bindBidirectional(emulationSection.netSidDevHostProperty());
		Bindings.bindBidirectional(port.textProperty(), emulationSection.netSidDevPortProperty(),
				new IntegerStringConverter());

		enableSldb.selectedProperty().bindBidirectional(sidplay2Section.enableDatabaseProperty());
		singleSong.selectedProperty().bindBidirectional(sidplay2Section.singleProperty());

		playEmulation.selectedProperty().set(!audioSection.isPlayOriginal());
		playMP3.selectedProperty().addListener((obj, o, n) -> playEmulation.selectedProperty().set(!n));
		playMP3.selectedProperty().bindBidirectional(audioSection.playOriginalProperty());

		this.duringInitialization = false;
	}

	public TabPane getTabbedPane() {
		return tabbedPane;
	}

	@Override
	public void doClose() {
		util.getPlayer().quit();
		Platform.exit();
	}

	@FXML
	private void setAudio() {
		restart();
	}

	@FXML
	public void setDevice() {
		int deviceIndex = devicesBox.getSelectionModel().getSelectedIndex();
		util.getConfig().getAudioSection().setDevice(deviceIndex);
		restart();
	}

	@FXML
	private void setSampling() {
		restart();
	}

	@FXML
	private void setSamplingRate() {
		restart();
	}

	@FXML
	private void setEngine() {
		restart();
	}

	@FXML
	private void setSid6581() {
		restart();
	}

	@FXML
	private void setSid8580() {
		restart();
	}

	@FXML
	private void setHostname() {
		NetSIDDevConnection.getInstance().invalidate();
		restart();
	}

	@FXML
	private void setPort() {
		NetSIDDevConnection.getInstance().invalidate();
		restart();
	}

	@FXML
	private void doEnableSldb() {
		final C64 c64 = util.getPlayer().getC64();
		final EventScheduler ctx = c64.getEventScheduler();
		ctx.scheduleThreadSafe(new Event("Update Play Timer!") {
			@Override
			public void event() {
				util.getPlayer().getTimer().updateEnd();
			}
		});
	}

	@FXML
	private void doBrowse() {
		final FileChooser fileDialog = new FileChooser();
		final FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("MP3 file (*.mp3)", "*.mp3");
		fileDialog.getExtensionFilters().add(extFilter);
		final File file = fileDialog.showOpenDialog(scene.getWindow());
		if (file != null) {
			util.getConfig().getAudioSection().setMp3File(file.getAbsolutePath());
			restart();
		}
	}

	@FXML
	private void showVolume() {
		String OS = System.getProperty("os.name").toLowerCase();
		if (OS.indexOf("win") >= 0) {
			SidPlay2Section section = util.getConfig().getSidplay2Section();
			int x = section.getFrameX() + section.getFrameWidth() / 2;
			try {
				Runtime.getRuntime().exec("sndvol -f " + x);
			} catch (IOException e) {
				try {
					Runtime.getRuntime().exec("sndvol32");
				} catch (IOException e1) {
					String toolTip = "For Windows: sndvol or sndvol32 not found!";
					volumeButton.setDisable(true);
					volumeButton.setTooltip(new Tooltip(toolTip));
					System.err.println(toolTip);
				}
			}
		} else if (OS.indexOf("nux") >= 0) {
			try {
				Runtime.getRuntime().exec("pavucontrol");
			} catch (IOException e2) {
				try {
					Runtime.getRuntime().exec("kmix");
				} catch (IOException e3) {
					String toolTip = "For Linux: pavucontrol(PulseAudio) or kmix(ALSA) not found!";
					volumeButton.setDisable(true);
					volumeButton.setTooltip(new Tooltip(toolTip));
					System.err.println(toolTip);
				}
			}
		} else if (OS.indexOf("mac") >= 0) {
			String toolTip = "For OSX: N.Y.I!";
			volumeButton.setDisable(true);
			volumeButton.setTooltip(new Tooltip(toolTip));
			System.err.println(toolTip);
		}
	}

	@FXML
	private void setVideoStandard() {
		restart();
	}

	private void restart() {
		if (!duringInitialization) {
			util.getPlayer().play(util.getPlayer().getTune());
		}
	}

	@Override
	public boolean isAllowed() {
		if (util.getConfig().getC1541Section().getExtendImagePolicy() == ExtendImagePolicy.EXTEND_ASK) {
			// EXTEND_ASK
			YesNoDialog dialog = new YesNoDialog(util.getPlayer());
			dialog.getStage().setTitle(util.getBundle().getString("EXTEND_DISK_IMAGE"));
			dialog.setText(util.getBundle().getString("EXTEND_DISK_IMAGE_TO_40_TRACKS"));
			dialog.open();
			return dialog.getConfirmed().get();
		} else if (util.getConfig().getC1541Section().getExtendImagePolicy() == ExtendImagePolicy.EXTEND_ACCESS) {
			// EXTEND_ACCESS
			return true;
		} else {
			// EXTEND_NEVER
			return false;
		}
	}

	/**
	 * Provide a filename for the tune containing some tune infos.
	 * 
	 * @see java.util.function.Function#apply(java.lang.Object)
	 */
	@Override
	public String apply(SidTune tune) {
		String defaultName = "jsidplay2";
		if (tune == SidTune.RESET) {
			return new File(util.getConfig().getSidplay2Section().getTmpDir(), defaultName).getAbsolutePath();
		}
		SidTuneInfo info = tune.getInfo();
		Iterator<String> infos = info.getInfoString().iterator();
		String name = infos.hasNext() ? infos.next().replaceAll("[:\\\\/*?|<>]", "_") : defaultName;
		String filename = new File(util.getConfig().getSidplay2Section().getTmpDir(),
				PathUtils.getFilenameWithoutSuffix(name)).getAbsolutePath();
		if (info.getSongs() > 1) {
			filename += String.format("-%02d", info.getCurrentSong());
		}
		return filename;
	}

}
