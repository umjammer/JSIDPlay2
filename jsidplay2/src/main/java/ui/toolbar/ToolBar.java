package ui.toolbar;

import static java.nio.charset.StandardCharsets.UTF_8;
import static libsidplay.common.ChipModel.MOS6581;
import static libsidplay.common.ChipModel.MOS8580;
import static server.restful.common.Connectors.HTTP;
import static server.restful.common.Connectors.HTTPS;
import static server.restful.common.Connectors.HTTP_HTTPS;
import static ui.entities.config.OnlineSection.JSIDPLAY2_APP_URL;

import java.io.File;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Collections;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javax.sound.sampled.Mixer.Info;

import builder.netsiddev.NetSIDDevConnection;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.stage.FileChooser;
import javafx.util.converter.IntegerStringConverter;
import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Engine;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SamplingMethod;
import libsidplay.common.SamplingRate;
import libsidplay.sidtune.MP3Tune;
import libsidplay.sidtune.SidTune;
import libsidutils.DesktopIntegration;
import libsidutils.ZipFileUtils;
import server.restful.JSIDPlay2Server;
import server.restful.common.Connectors;
import sidplay.Player;
import sidplay.audio.Audio;
import sidplay.audio.JavaSound;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.EnumToStringConverter;
import ui.common.MixerInfoToStringConverter;
import ui.common.PositiveNumberToStringConverter;
import ui.common.TimeToStringConverter;
import ui.common.UIPart;
import ui.entities.config.AudioSection;
import ui.entities.config.Configuration;
import ui.entities.config.EmulationSection;
import ui.entities.config.SidPlay2Section;

public class ToolBar extends C64VBox implements UIPart {

	private static final String CELL_VALUE_OK = "cellValueOk";
	private static final String CELL_VALUE_ERROR = "cellValueError";

	@FXML
	private ComboBox<SamplingMethod> samplingBox;
	@FXML
	private ComboBox<CPUClock> videoStandardBox;
	@FXML
	private ComboBox<Integer> hardsid6581Box, hardsid8580Box, audioBufferSize;
	@FXML
	private ComboBox<SamplingRate> samplingRateBox;
	@FXML
	private ComboBox<Audio> audioBox;
	@FXML
	private ComboBox<Info> devicesBox;
	@FXML
	private ComboBox<Engine> engineBox;
	@FXML
	private ComboBox<Connectors> appServerConnectorsBox;
	@FXML
	private ComboBox<ChipModel> sidBlaster0Box, sidBlaster1Box, sidBlaster2Box;
	@FXML
	private CheckBox enableSldb, singleSong, proxyEnable, enableUltimate64;
	@FXML
	private TextField bufferSize, defaultTime, proxyHostname, proxyPort, hostname, port, ultimate64Hostname,
			ultimate64Port, ultimate64SyncDelay, appServerPort, appServerSecurePort, appServerKeyStorePassword,
			appServerKeyAlias, appServerKeyPassword, ultimate64StreamingTarget, ultimate64StreamingAudioPort,
			ultimate64StreamingVideoPort;
	@FXML
	protected RadioButton playMP3, playEmulation, startAppServer, stopAppServer;
	@FXML
	protected ToggleGroup playSourceGroup, appServerGroup;
	@FXML
	protected Button volumeButton, mp3Browse, keystoreBrowse;
	@FXML
	private Label hostnameLabel, portLabel, hardsid6581Label, hardsid8580Label, appIpAddress, appHostname,
			appServerPortLbl, appServerSecurePortLbl, appServerKeyStorePasswordLbl, appServerKeyAliasLbl,
			appServerKeyPasswordLbl;
	@FXML
	private Hyperlink appServerUsage, downloadApp;

	@FXML
	protected ProgressBar progress;

	private ObservableList<ChipModel> sidBlaster0Models, sidBlaster1Models, sidBlaster2Models;

	private boolean duringInitialization;

	/**
	 * JSIPlay2 REST based web-services
	 */
	private JSIDPlay2Server jsidplay2Server;

	public ToolBar() {
		super();
	}

	public ToolBar(C64Window window, Player player) {
		super(window, player);
	}

	@FXML
	protected void initialize() {
		this.duringInitialization = true;

		final ResourceBundle bundle = util.getBundle();
		final Configuration config = util.getConfig();
		final SidPlay2Section sidplay2Section = config.getSidplay2Section();
		final AudioSection audioSection = config.getAudioSection();
		final EmulationSection emulationSection = config.getEmulationSection();

		jsidplay2Server = JSIDPlay2Server.getInstance(config);

		audioBox.setConverter(new EnumToStringConverter<Audio>(bundle));
		audioBox.setItems(FXCollections.<Audio>observableArrayList(Audio.SOUNDCARD, Audio.LIVE_WAV, Audio.LIVE_MP3,
				Audio.LIVE_AVI, Audio.LIVE_MP4, Audio.COMPARE_MP3));
		audioBox.valueProperty().addListener((obj, o, n) -> {
			mp3Browse.setDisable(!Audio.COMPARE_MP3.equals(n));
			playMP3.setDisable(!Audio.COMPARE_MP3.equals(n));
			playEmulation.setDisable(!Audio.COMPARE_MP3.equals(n));
		});
		audioBox.valueProperty().bindBidirectional(audioSection.audioProperty());

		devicesBox.setConverter(new MixerInfoToStringConverter());
		devicesBox.setItems(FXCollections.<Info>observableArrayList(JavaSound.getDevices()));
		devicesBox.getSelectionModel().select(Math.min(audioSection.getDevice(), devicesBox.getItems().size() - 1));

		samplingBox.setConverter(new EnumToStringConverter<SamplingMethod>(bundle));
		samplingBox.setItems(FXCollections.<SamplingMethod>observableArrayList(SamplingMethod.values()));
		samplingBox.valueProperty().bindBidirectional(audioSection.samplingProperty());

		samplingRateBox.setConverter(new EnumToStringConverter<SamplingRate>(bundle));
		samplingRateBox.setItems(FXCollections.<SamplingRate>observableArrayList(SamplingRate.values()));
		samplingRateBox.valueProperty().addListener((obj, o, n) -> audioSection.setSamplingRate(n));
		audioSection.samplingRateProperty()
				.addListener((obj, o, n) -> Platform.runLater(() -> samplingRateBox.setValue(n)));
		samplingRateBox.setValue(audioSection.getSamplingRate());

		videoStandardBox.setConverter(new EnumToStringConverter<CPUClock>(bundle));
		videoStandardBox.valueProperty().bindBidirectional(emulationSection.defaultClockSpeedProperty());
		videoStandardBox.setItems(FXCollections.<CPUClock>observableArrayList(CPUClock.values()));

		hardsid6581Box.valueProperty().bindBidirectional(emulationSection.hardsid6581Property());
		hardsid8580Box.valueProperty().bindBidirectional(emulationSection.hardsid8580Property());
		audioBufferSize.valueProperty().bindBidirectional(audioSection.audioBufferSizeProperty());

		engineBox.setConverter(new EnumToStringConverter<Engine>(bundle));
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
		Bindings.bindBidirectional(bufferSize.textProperty(), audioSection.bufferSizeProperty(),
				new PositiveNumberToStringConverter<>(2048));
		audioSection.bufferSizeProperty().addListener((obj, o, n) -> {
			final Tooltip tooltip = new Tooltip();
			bufferSize.getStyleClass().removeAll(CELL_VALUE_OK, CELL_VALUE_ERROR);
			if (n.intValue() >= 2048) {
				tooltip.setText(util.getBundle().getString("BUFFER_SIZE_TIP"));
				bufferSize.setTooltip(tooltip);
				bufferSize.getStyleClass().add(CELL_VALUE_OK);
			} else {
				tooltip.setText(util.getBundle().getString("BUFFER_SIZE_FORMAT"));
				bufferSize.setTooltip(tooltip);
				bufferSize.getStyleClass().add(CELL_VALUE_ERROR);
			}
		});

		sidBlaster0Models = FXCollections.<ChipModel>observableArrayList(MOS6581, MOS8580);
		sidBlaster0Box.setConverter(new EnumToStringConverter<ChipModel>(bundle));
		sidBlaster0Box.valueProperty().bindBidirectional(emulationSection.sidBlaster0ModelProperty());
		sidBlaster0Box.setItems(sidBlaster0Models);

		sidBlaster1Models = FXCollections.<ChipModel>observableArrayList(MOS6581, MOS8580);
		sidBlaster1Box.setConverter(new EnumToStringConverter<ChipModel>(bundle));
		sidBlaster1Box.valueProperty().bindBidirectional(emulationSection.sidBlaster1ModelProperty());
		sidBlaster1Box.setItems(sidBlaster1Models);

		sidBlaster2Models = FXCollections.<ChipModel>observableArrayList(MOS6581, MOS8580);
		sidBlaster2Box.setConverter(new EnumToStringConverter<ChipModel>(bundle));
		sidBlaster2Box.valueProperty().bindBidirectional(emulationSection.sidBlaster2ModelProperty());
		sidBlaster2Box.setItems(sidBlaster2Models);

		proxyEnable.selectedProperty().bindBidirectional(sidplay2Section.enableProxyProperty());
		proxyHostname.textProperty().bindBidirectional(sidplay2Section.proxyHostnameProperty());
		Bindings.bindBidirectional(proxyPort.textProperty(), sidplay2Section.proxyPortProperty(),
				new IntegerStringConverter());

		hostname.textProperty().bindBidirectional(emulationSection.netSidDevHostProperty());
		Bindings.bindBidirectional(port.textProperty(), emulationSection.netSidDevPortProperty(),
				new IntegerStringConverter());

		enableUltimate64.selectedProperty().bindBidirectional(emulationSection.enableUltimate64Property());
		ultimate64Hostname.textProperty().bindBidirectional(emulationSection.ultimate64HostProperty());
		Bindings.bindBidirectional(ultimate64Port.textProperty(), emulationSection.ultimate64PortProperty(),
				new IntegerStringConverter());
		Bindings.bindBidirectional(ultimate64SyncDelay.textProperty(), emulationSection.ultimate64SyncDelayProperty(),
				new IntegerStringConverter());

		Bindings.bindBidirectional(appServerPort.textProperty(), emulationSection.appServerPortProperty(),
				new IntegerStringConverter());
		Bindings.bindBidirectional(appServerSecurePort.textProperty(), emulationSection.appServerSecurePortProperty(),
				new IntegerStringConverter());

		appServerConnectorsBox.setConverter(new EnumToStringConverter<Connectors>(bundle));
		appServerConnectorsBox.valueProperty().addListener((obj, o, n) -> {
			switch (n) {
			case HTTP_HTTPS:
				for (Node node : Arrays.asList(appServerPortLbl, appServerSecurePortLbl, appServerKeyStorePasswordLbl,
						appServerKeyAliasLbl, appServerKeyPasswordLbl, appServerPort, appServerSecurePort,
						keystoreBrowse, appServerKeyStorePassword, appServerKeyAlias, appServerKeyPassword)) {
					node.setVisible(true);
					node.setManaged(true);
				}
				break;
			case HTTPS:
				for (Node node : Arrays.asList(appServerPortLbl, appServerPort)) {
					node.setVisible(false);
					node.setManaged(false);
				}
				for (Node node : Arrays.asList(appServerSecurePortLbl, appServerKeyStorePasswordLbl,
						appServerKeyAliasLbl, appServerKeyPasswordLbl, appServerSecurePort, keystoreBrowse,
						appServerKeyStorePassword, appServerKeyAlias, appServerKeyPassword)) {
					node.setVisible(true);
					node.setManaged(true);
				}
				break;

			case HTTP:
			default:
				for (Node node : Arrays.asList(appServerSecurePortLbl, appServerKeyStorePasswordLbl,
						appServerKeyAliasLbl, appServerKeyPasswordLbl, appServerSecurePort, keystoreBrowse,
						appServerKeyStorePassword, appServerKeyAlias, appServerKeyPassword)) {
					node.setVisible(false);
					node.setManaged(false);
				}
				for (Node node : Arrays.asList(appServerPortLbl, appServerPort)) {
					node.setVisible(true);
					node.setManaged(true);
				}
				break;
			}
		});
		appServerConnectorsBox.setItems(FXCollections.<Connectors>observableArrayList(HTTP, HTTP_HTTPS, HTTPS));
		appServerConnectorsBox.valueProperty().bindBidirectional(emulationSection.appServerConnectorsProperty());
		appServerKeyStorePassword.textProperty()
				.bindBidirectional(emulationSection.appServerKeystorePasswordProperty());
		appServerKeyPassword.textProperty().bindBidirectional(emulationSection.appServerKeyPasswordProperty());
		appServerKeyAlias.textProperty().bindBidirectional(emulationSection.appServerKeyAliasProperty());

		enableSldb.selectedProperty().bindBidirectional(sidplay2Section.enableDatabaseProperty());
		singleSong.selectedProperty().bindBidirectional(sidplay2Section.singleProperty());

		playEmulation.selectedProperty().set(!audioSection.isPlayOriginal());
		playMP3.selectedProperty().addListener((obj, o, n) -> playEmulation.selectedProperty().set(!n));
		playMP3.selectedProperty().bindBidirectional(audioSection.playOriginalProperty());

		appHostname.setText(util.getBundle().getString("APP_SERVER_HOSTNAME") + " " + getHostname());
		appIpAddress.setText(util.getBundle().getString("APP_SERVER_IP") + " " + getIpAddresses());
		startAppServer.selectedProperty().addListener((observable, oldValue, newValue) -> {
			appServerUsage.setDisable(!newValue);
			downloadApp.setDisable(!newValue);
		});

		ultimate64StreamingTarget.textProperty()
				.bindBidirectional(emulationSection.ultimate64StreamingTargetProperty());
		ultimate64StreamingAudioPort.textProperty().bindBidirectional(
				emulationSection.ultimate64StreamingAudioPortProperty(), new IntegerStringConverter());
		ultimate64StreamingVideoPort.textProperty().bindBidirectional(
				emulationSection.ultimate64StreamingVideoPortProperty(), new IntegerStringConverter());

		this.duringInitialization = false;
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
	private void setSidBlaster0() {
		restart();
	}

	@FXML
	private void setSidBlaster1() {
		restart();
	}

	@FXML
	private void setSidBlaster2() {
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
	private void setUltimate64Hostname() {
		restart();
	}

	@FXML
	private void setUltimate64Port() {
		restart();
	}

	@FXML
	private void setUltimate64SyncDelay() {
		restart();
	}

	@FXML
	private void setAudioBufferSize() {
		restart();
	}

	@FXML
	private void setUltimate64StreamingTarget() {
		restart();
	}

	@FXML
	private void setUtimate64StreamingAudioPort() {
		restart();
	}

	@FXML
	private void setUtimate64StreamingVideoPort() {
		restart();
	}

	@FXML
	private void doKeystoreBrowse() {
		final FileChooser fileDialog = new FileChooser();
		final FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("Keystore file (*.ks)", "*.ks");
		fileDialog.getExtensionFilters().add(extFilter);
		final File file = fileDialog.showOpenDialog(getScene().getWindow());
		if (file != null) {
			util.getConfig().getEmulationSection().setAppServerKeystoreFile(file.getAbsolutePath());
		}
	}

	@FXML
	private void startAppServer() {
		try {
			jsidplay2Server.start();
		} catch (Exception e) {
			openErrorDialog(e.getMessage());
		}
	}

	@FXML
	private void stopAppServer() {
		try {
			jsidplay2Server.stop();
		} catch (Exception e) {
			openErrorDialog(e.getMessage());
		}
	}

	@FXML
	private void gotoRestApiUsage() {
		EmulationSection emulationSection = util.getConfig().getEmulationSection();
		Connectors appServerConnectors = emulationSection.getAppServerConnectors();
		int port = appServerConnectors.getPreferredProtocol().equals("http") ? emulationSection.getAppServerPort()
				: emulationSection.getAppServerSecurePort();
		DesktopIntegration.browse(appServerConnectors.getPreferredProtocol() + "://127.0.0.1:" + port);
	}

	@FXML
	private void doEnableSldb() {
		final EventScheduler ctx = util.getPlayer().getC64().getEventScheduler();
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
		final File file = fileDialog.showOpenDialog(getScene().getWindow());
		if (file != null) {
			util.getConfig().getAudioSection().setMp3File(file.getAbsolutePath());
			if (util.getPlayer().getTune() instanceof MP3Tune) {
				util.getPlayer().setTune(SidTune.RESET);
			}
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

	@FXML
	private void downloadApp() {
		DesktopIntegration.browse(JSIDPLAY2_APP_URL);
	}

	@Override
	public void doClose() {
		stopAppServer();
	}

	private String getHostname() {
		try {
			Process proc = Runtime.getRuntime().exec("hostname");
			return ZipFileUtils.convertStreamToString(proc.getInputStream(), UTF_8.name());
		} catch (IOException e) {
			return "?hostname?";
		}
	}

	private String getIpAddresses() {
		try {
			return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
					.flatMap(iface -> Collections.list(iface.getInetAddresses()).stream())
					.filter(address -> !address.isLoopbackAddress() && address.isSiteLocalAddress())
					.map(address -> address.getHostAddress()).collect(Collectors.joining("\n"));
		} catch (SocketException ex) {
			return "?ip?";
		}
	}

	private void openErrorDialog(String msg) {
		Alert alert = new Alert(AlertType.ERROR, "");
		alert.setTitle(util.getBundle().getString("ALERT_TITLE"));
		alert.getDialogPane().setHeaderText(msg);
		alert.showAndWait();
	}

	private void restart() {
		if (!duringInitialization) {
			util.getPlayer().play(util.getPlayer().getTune());
		}
	}

}
