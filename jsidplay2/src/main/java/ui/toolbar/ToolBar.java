package ui.toolbar;

import static java.nio.charset.StandardCharsets.UTF_8;
import static javafx.beans.binding.Bindings.bindBidirectional;
import static libsidplay.components.pla.PLA.MAX_SIDS;
import static server.restful.common.Connectors.HTTP;
import static server.restful.common.Connectors.HTTPS;
import static server.restful.common.Connectors.HTTP_HTTPS;
import static sidplay.audio.Audio.getLiveAudio;
import static ui.common.properties.BindingUtils.bindBidirectional;
import static ui.common.properties.BindingUtils.bindBidirectionalThreadSafe;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.ResourceBundle;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.sound.sampled.Mixer.Info;

import builder.jsidblaster.JSIDBlasterBuilder;
import builder.netsiddev.NetSIDDevConnection;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.converter.IntegerStringConverter;
import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Engine;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.Mixer;
import libsidplay.common.OS;
import libsidplay.common.SamplingMethod;
import libsidplay.common.SamplingRate;
import libsidplay.common.Ultimate64Mode;
import libsidplay.sidtune.MP3Tune;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.IOUtils;
import server.restful.JSIDPlay2Server;
import server.restful.common.Connectors;
import sidplay.Player;
import sidplay.audio.Audio;
import sidplay.audio.JavaSound;
import sidplay.ini.IniConfig;
import sidplay.player.State;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.UIPart;
import ui.common.converter.EnumToStringConverter;
import ui.common.converter.HardSIDSlotToIntegerConverter;
import ui.common.converter.MinimumNumberToStringConverter;
import ui.common.converter.MixerInfoToStringConverter;
import ui.common.converter.TimeToStringConverter;
import ui.common.fileextension.KeyStoreFileExtensions;
import ui.common.fileextension.MP3TuneFileExtensions;
import ui.common.util.DesktopUtil;
import ui.entities.config.AudioSection;
import ui.entities.config.Configuration;
import ui.entities.config.DeviceMapping;
import ui.entities.config.EmulationSection;
import ui.entities.config.SidPlay2Section;

public class ToolBar extends C64VBox implements UIPart {

	private StateChangeListener propertyChangeListener;

	private class StateChangeListener implements PropertyChangeListener {
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			Platform.runLater(() -> {
				if (event.getNewValue() == State.OPEN) {
					if (testPlayer != null) {
						testPlayer.stopC64();
					}
				} else if (event.getNewValue() == State.START) {
					util.getPlayer()
							.configureMixer(mixer -> Platform.runLater(() -> setActiveSidBlasterDevices(mixer)));
				}
			});
		}

	}

	private static final String SIDBLASTER_TEST_SID = "/ui/toolbar/sidblaster_test.sid";

	@FXML
	private ComboBox<SamplingMethod> samplingBox;
	@FXML
	private ComboBox<CPUClock> videoStandardBox;
	@FXML
	private ComboBox<Integer> hardsid6581Box, hardsid8580Box, audioBufferSize, sidBlasterWriteBufferSize;
	@FXML
	private ComboBox<Short> sidBlasterLatencyTimer;
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
	private ComboBox<Ultimate64Mode> ultimate64Box;
	@FXML
	private CheckBox sidBlasterRead, enableSldb, singleSong;
	@FXML
	private TextField bufferSize, defaultPlayLength, hostname, port, ultimate64Hostname, ultimate64Port,
			ultimate64SyncDelay, appServerPort, appServerSecurePort, appServerKeyStorePassword, appServerKeyAlias,
			appServerKeyPassword, ultimate64StreamingTarget, ultimate64StreamingAudioPort, ultimate64StreamingVideoPort,
			videoStreamingUrl;
	@FXML
	private ScrollPane sidBlasterScrollPane;
	@FXML
	private VBox sidBlasterDeviceParent;
	@FXML
	private Button addSidBlaster, autodetect, volumeButton, mp3Browse, keystoreBrowse;
	@FXML
	private RadioButton playMP3, playEmulation, startAppServer, stopAppServer;
	@FXML
	private ToggleGroup playSourceGroup, appServerGroup, testButtonGroup;
	@FXML
	private Label hostnameLabel, portLabel, hardsid6581Label, hardsid8580Label, appIpAddress, appHostname,
			appServerPortLbl, appServerSecurePortLbl, appServerKeyStorePasswordLbl, appServerKeyAliasLbl,
			appServerKeyPasswordLbl, sidBlasterWriteBufferSizeLbl, sidBlasterLatencyTimerLbl, sidBlasterReadLbl,
			streamingIpAddress, streamingHostname;
	@FXML
	private Hyperlink appServerUsage, onlinePlayer, sidBlasterDoc;
	@FXML
	private ProgressBar progress;

	private ObservableList<Ultimate64Mode> ultimate64Modes;

	private JSIDPlay2Server jsidplay2Server;

	private Player testPlayer;

	private boolean duringInitialization, duringReplay;

	public ToolBar() {
		super();
	}

	public ToolBar(C64Window window, Player player) {
		super(window, player);
	}

	@FXML
	@Override
	protected void initialize() {
		this.duringInitialization = true;

		final ResourceBundle bundle = util.getBundle();
		final Configuration config = util.getConfig();
		final SidPlay2Section sidplay2Section = config.getSidplay2Section();
		final AudioSection audioSection = config.getAudioSection();
		final EmulationSection emulationSection = config.getEmulationSection();

		jsidplay2Server = JSIDPlay2Server.getInstance(config);

		audioBox.setConverter(new EnumToStringConverter<Audio>(bundle));
		audioBox.setItems(FXCollections.<Audio>observableArrayList(getLiveAudio()));
		audioBox.valueProperty().addListener((obj, o, n) -> {
			mp3Browse.setDisable(!Audio.COMPARE_MP3.equals(n));
			playMP3.setDisable(!Audio.COMPARE_MP3.equals(n));
			playEmulation.setDisable(!Audio.COMPARE_MP3.equals(n));
		});
		audioBox.valueProperty().bindBidirectional(audioSection.audioProperty());

		devicesBox.setConverter(new MixerInfoToStringConverter(util.getBundle(), "NO_AUDIO"));
		devicesBox.setItems(FXCollections.<Info>observableArrayList(JavaSound.getDeviceInfos()));
		devicesBox.getSelectionModel().select(Math.min(audioSection.getDevice(), devicesBox.getItems().size() - 1));

		samplingBox.setConverter(new EnumToStringConverter<SamplingMethod>(bundle));
		samplingBox.setItems(FXCollections.<SamplingMethod>observableArrayList(SamplingMethod.values()));
		samplingBox.valueProperty().bindBidirectional(audioSection.samplingProperty());

		samplingRateBox.setConverter(new EnumToStringConverter<SamplingRate>(bundle));
		samplingRateBox.setItems(FXCollections.<SamplingRate>observableArrayList(SamplingRate.values()));
		bindBidirectionalThreadSafe(samplingRateBox.valueProperty(), audioSection.samplingRateProperty(),
				() -> duringReplay = util.getPlayer().stateProperty().get() == State.RESTART);

		videoStandardBox.setConverter(new EnumToStringConverter<CPUClock>(bundle));
		videoStandardBox.valueProperty().bindBidirectional(emulationSection.userClockSpeedProperty());
		videoStandardBox.setItems(FXCollections.<CPUClock>observableArrayList(CPUClock.values()));

		hardsid6581Box.setConverter(new HardSIDSlotToIntegerConverter(util.getBundle()));
		hardsid6581Box.valueProperty().bindBidirectional(emulationSection.hardsid6581Property());
		hardsid8580Box.setConverter(new HardSIDSlotToIntegerConverter(util.getBundle()));
		hardsid8580Box.valueProperty().bindBidirectional(emulationSection.hardsid8580Property());
		audioBufferSize.valueProperty().bindBidirectional(audioSection.audioBufferSizeProperty());

		engineBox.setConverter(new EnumToStringConverter<Engine>(bundle));
		engineBox.setItems(FXCollections.<Engine>observableArrayList(Engine.EMULATION, Engine.NETSID, Engine.HARDSID,
				Engine.SIDBLASTER, Engine.EXSID));
		engineBox.valueProperty().addListener((obj, o, n) -> {
			hardsid6581Box.setDisable(!Engine.HARDSID.equals(n));
			hardsid8580Box.setDisable(!Engine.HARDSID.equals(n));
			hardsid6581Label.setDisable(!Engine.HARDSID.equals(n));
			hardsid8580Label.setDisable(!Engine.HARDSID.equals(n));

			hostnameLabel.setDisable(!Engine.NETSID.equals(n));
			hostname.setDisable(!Engine.NETSID.equals(n));
			portLabel.setDisable(!Engine.NETSID.equals(n));
			port.setDisable(!Engine.NETSID.equals(n));

			audioBox.setDisable(!Engine.EMULATION.equals(n));
			devicesBox.setDisable(!Engine.EMULATION.equals(n));
			samplingBox.setDisable(!Engine.EMULATION.equals(n));
			samplingRateBox.setDisable(!Engine.EMULATION.equals(n));
			volumeButton.setDisable(!Engine.EMULATION.equals(n));

			disable(sidBlasterDeviceParent, !Engine.SIDBLASTER.equals(n));
			addSidBlaster.setDisable(!Engine.SIDBLASTER.equals(n));
			autodetect.setDisable(!Engine.SIDBLASTER.equals(n));
			sidBlasterWriteBufferSizeLbl.setDisable(!Engine.SIDBLASTER.equals(n));
			sidBlasterWriteBufferSize.setDisable(!Engine.SIDBLASTER.equals(n));
			sidBlasterLatencyTimerLbl.setDisable(!Engine.SIDBLASTER.equals(n));
			sidBlasterLatencyTimer.setDisable(!Engine.SIDBLASTER.equals(n));
			sidBlasterReadLbl.setDisable(!Engine.SIDBLASTER.equals(n));
			sidBlasterRead.setDisable(!Engine.SIDBLASTER.equals(n));
		});
		engineBox.valueProperty().bindBidirectional(emulationSection.engineProperty());

		bindBidirectional(defaultPlayLength.textProperty(), sidplay2Section.defaultPlayLengthProperty(),
				new TimeToStringConverter());
		sidplay2Section.defaultPlayLengthProperty()
				.addListener((obj, o, n) -> util.checkTextField(defaultPlayLength, () -> n.intValue() != -1,
						() -> util.getPlayer().getTimer().updateEnd(), "DEFAULT_LENGTH_TIP", "DEFAULT_LENGTH_FORMAT"));
		bindBidirectional(bufferSize.textProperty(), audioSection.bufferSizeProperty(),
				new MinimumNumberToStringConverter("#", 2048));
		audioSection.bufferSizeProperty()
				.addListener((obj, o, n) -> util.checkTextField(bufferSize, () -> n.intValue() >= 2048, () -> {
				}, "BUFFER_SIZE_TIP", "BUFFER_SIZE_FORMAT"));

		sidBlasterWriteBufferSize.valueProperty()
				.bindBidirectional(emulationSection.sidBlasterWriteBufferSizeProperty());
		emulationSection.getSidBlasterDeviceList().stream().forEach(this::addSidBlasterDeviceMapping);
		sidBlasterRead.selectedProperty().bindBidirectional(emulationSection.sidBlasterReadProperty());
		sidBlasterLatencyTimer.valueProperty().bindBidirectional(emulationSection.sidBlasterLatencyTimerProperty());

		hostname.textProperty().bindBidirectional(emulationSection.netSidDevHostProperty());
		bindBidirectional(port.textProperty(), emulationSection.netSidDevPortProperty(), new IntegerStringConverter());

		ultimate64Modes = FXCollections.<Ultimate64Mode>observableArrayList(Ultimate64Mode.values());
		ultimate64Box.setConverter(new EnumToStringConverter<Ultimate64Mode>(bundle));
		ultimate64Box.valueProperty().bindBidirectional(emulationSection.ultimate64ModeProperty());
		ultimate64Box.setItems(ultimate64Modes);

		ultimate64Hostname.textProperty().bindBidirectional(emulationSection.ultimate64HostProperty());
		bindBidirectional(ultimate64Port.textProperty(), emulationSection.ultimate64PortProperty(),
				new IntegerStringConverter());

		bindBidirectional(ultimate64SyncDelay.textProperty(), emulationSection.ultimate64SyncDelayProperty(),
				new IntegerStringConverter());

		bindBidirectional(appServerPort.textProperty(), emulationSection.appServerPortProperty(),
				new IntegerStringConverter());
		bindBidirectional(appServerSecurePort.textProperty(), emulationSection.appServerSecurePortProperty(),
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

		bindBidirectional(playSourceGroup, audioSection.playOriginalProperty());

		appHostname.setText(util.getBundle().getString("HOSTNAME") + " " + getHostname());
		appIpAddress.setText(util.getBundle().getString("IP") + " " + getIpAddresses());
		startAppServer.selectedProperty().addListener((observable, oldValue, newValue) -> {
			appServerUsage.setDisable(!newValue);
			onlinePlayer.setDisable(!newValue);
		});

		ultimate64StreamingTarget.textProperty()
				.bindBidirectional(emulationSection.ultimate64StreamingTargetProperty());
		ultimate64StreamingAudioPort.textProperty().bindBidirectional(
				emulationSection.ultimate64StreamingAudioPortProperty(), new IntegerStringConverter());
		ultimate64StreamingVideoPort.textProperty().bindBidirectional(
				emulationSection.ultimate64StreamingVideoPortProperty(), new IntegerStringConverter());
		streamingHostname.setText(util.getBundle().getString("HOSTNAME") + " " + getHostname());
		streamingIpAddress.setText(util.getBundle().getString("IP") + " " + getIpAddresses());

		videoStreamingUrl.textProperty().bindBidirectional(audioSection.videoStreamingUrlProperty());

		propertyChangeListener = new StateChangeListener();
		util.getPlayer().stateProperty().addListener(propertyChangeListener);

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
	private void addSidBlaster() {
		final EmulationSection emulationSection = util.getConfig().getEmulationSection();

		DeviceMapping deviceMapping = new DeviceMapping("", ChipModel.AUTO, true);
		emulationSection.getSidBlasterDeviceList().add(deviceMapping);
		addSidBlasterDeviceMapping(deviceMapping);
	}

	@FXML
	private void autodetect() {
		final EmulationSection emulationSection = util.getConfig().getEmulationSection();
		try {
			if (JSIDBlasterBuilder.getSerialNumbers() == null) {
				triggerFetchSerialNumbers();
			}
			// overwrite device list
			emulationSection.getSidBlasterDeviceList().clear();
			sidBlasterDeviceParent.getChildren().clear();
			for (int i = 0; i < JSIDBlasterBuilder.getSerialNumbers().length; i++) {
				String serialNumber = JSIDBlasterBuilder.getSerialNumbers()[i];
				ChipModel chipModel = JSIDBlasterBuilder.getSidType(i).asChipModel();
				DeviceMapping deviceMapping = new DeviceMapping(serialNumber, chipModel, true);
				emulationSection.getSidBlasterDeviceList().add(deviceMapping);
				addSidBlasterDeviceMapping(deviceMapping);
			}
		} catch (Error error) {
			openErrorDialog(error.getMessage());
		}
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
	private void setUltimate64() {
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
		final FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(
				KeyStoreFileExtensions.DESCRIPTION, KeyStoreFileExtensions.DESCRIPTION);
		fileDialog.getExtensionFilters().add(extFilter);
		final File file = fileDialog.showOpenDialog(getScene().getWindow());
		if (file != null) {
			util.getConfig().getEmulationSection().setAppServerKeystoreFile(file);
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

		DesktopUtil.browse(appServerConnectors.getPreferredProtocol() + "://127.0.0.1:" + port);
	}

	@FXML
	private void onlinePlayer() {
		try {
			EmulationSection emulationSection = util.getConfig().getEmulationSection();
			Connectors appServerConnectors = emulationSection.getAppServerConnectors();
			int port = appServerConnectors.getPreferredProtocol().equals("http") ? emulationSection.getAppServerPort()
					: emulationSection.getAppServerSecurePort();

			URI uri = new URI(util.getConfig().getOnlineSection().getOnlinePlayerUrl());
			URI localURI = new URI(appServerConnectors.getPreferredProtocol(), null, "127.0.0.1", port, uri.getPath(),
					uri.getQuery(), uri.getFragment());
			DesktopUtil.browse(localURI.toString());
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
	}

	@FXML
	private void gotoSidBlasterDoc() {
		DesktopUtil.browse(util.getConfig().getOnlineSection().getSidBlasterDocUrl());
	}

	@FXML
	private void doEnableSldb() {
		final EventScheduler ctx = util.getPlayer().getC64().getEventScheduler();
		ctx.scheduleThreadSafe(Event.of("Update Play Timer!", event -> util.getPlayer().getTimer().updateEnd()));
	}

	@FXML
	private void doBrowse() {
		final FileChooser fileDialog = new FileChooser();
		final FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter(MP3TuneFileExtensions.DESCRIPTION,
				MP3TuneFileExtensions.EXTENSIONS);
		fileDialog.getExtensionFilters().add(extFilter);
		final File file = fileDialog.showOpenDialog(getScene().getWindow());
		if (file != null) {
			util.getConfig().getAudioSection().setMp3(file);
			if (util.getPlayer().getTune() instanceof MP3Tune) {
				util.getPlayer().setTune(SidTune.RESET);
			}
			restart();
		}
	}

	@FXML
	private void showVolume() {
		OS os = OS.get();
		if (os == OS.WINDOWS) {
			SidPlay2Section section = util.getConfig().getSidplay2Section();
			int x = (int) (section.getFrameX() + section.getFrameWidth() / 2);
			try {
				Runtime.getRuntime().exec(new String[] { "sndvol", "-f", String.valueOf(x) }, null, null);
			} catch (IOException e) {
				String toolTip = "For Windows: sndvol not found!";
				volumeButton.setDisable(true);
				volumeButton.setTooltip(new Tooltip(toolTip));
				System.err.println(toolTip);
			}
		} else if (os == OS.LINUX) {
			try {
				Runtime.getRuntime().exec(new String[] { "pavucontrol" }, null, null);
			} catch (IOException e2) {
				try {
					Runtime.getRuntime().exec(new String[] { "kmix" }, null, null);
				} catch (IOException e3) {
					String toolTip = "For Linux: pavucontrol(PulseAudio) or kmix(ALSA) not found!";
					volumeButton.setDisable(true);
					volumeButton.setTooltip(new Tooltip(toolTip));
					System.err.println(toolTip);
				}
			}
		} else if (os == OS.MAC || os == OS.OTHER) {
			String toolTip = "For " + os + ": N.Y.I!";
			volumeButton.setDisable(true);
			volumeButton.setTooltip(new Tooltip(toolTip));
			System.err.println(toolTip);
		}
	}

	@FXML
	private void setVideoStandard() {
		restart();
	}

	@Override
	public void doClose() {
		util.getPlayer().stateProperty().removeListener(propertyChangeListener);
		stopAppServer();
	}

	private void disable(Node n, boolean disable) {
		n.setDisable(disable);
		if (n instanceof Parent) {
			for (Node c : ((Parent) n).getChildrenUnmodifiable()) {
				disable(c, disable);
			}
		}
	}

	private void addSidBlasterDeviceMapping(DeviceMapping deviceMapping) {
		SidBlasterDeviceMapping sidBlasterDeviceMapping = new SidBlasterDeviceMapping(util.getWindow(),
				util.getPlayer());
		sidBlasterDeviceMapping.init(deviceMapping, this::testSidBlasterDevice, this::removeSidBlasterDeviceMapping,
				testButtonGroup);
		sidBlasterDeviceParent.getChildren().add(sidBlasterDeviceMapping);

		// scroll to bottom automatically
		Platform.runLater(() -> {
			sidBlasterDeviceParent.requestLayout();
			Platform.runLater(() -> sidBlasterScrollPane.setVvalue(1.0));
		});
	}

	private void removeSidBlasterDeviceMapping(DeviceMapping deviceMapping) {
		final EmulationSection emulationSection = util.getConfig().getEmulationSection();

		sidBlasterDeviceParent.getChildren().remove(emulationSection.getSidBlasterDeviceList().indexOf(deviceMapping));
		emulationSection.getSidBlasterDeviceList().remove(deviceMapping);
	}

	private void testSidBlasterDevice(DeviceMapping deviceMapping, Boolean isSelected) {
		try {
			if (testPlayer == null) {
				testPlayer = new Player(new IniConfig());
				testPlayer.getConfig().getEmulationSection().setEngine(Engine.SIDBLASTER);
			} else {
				testPlayer.stopC64();
			}
			if (JSIDBlasterBuilder.getSerialNumbers() == null) {
				triggerFetchSerialNumbers();
			}
			if (isSelected) {
				util.getPlayer().stopC64(true);

				setActiveSidBlasterDevice(serial -> Objects.equals(deviceMapping.getSerialNum(), serial));

				testPlayer.getConfig().getEmulationSection().setSidBlasterSerialNumber(deviceMapping.getSerialNum());
				testPlayer.play(
						SidTune.load("sidblaster_test.sid", ToolBar.class.getResourceAsStream(SIDBLASTER_TEST_SID)));
			}
		} catch (IOException | SidTuneError e) {
			openErrorDialog(e.getMessage());
		}
	}

	private void triggerFetchSerialNumbers() {
		new JSIDBlasterBuilder(null, util.getConfig(), null);
	}

	private void setActiveSidBlasterDevices(Mixer mixer) {
		List<String> serialNumbers = new ArrayList<>();
		if (mixer instanceof JSIDBlasterBuilder) {
			JSIDBlasterBuilder sidBlasterBuilder = (JSIDBlasterBuilder) mixer;
			for (int sidNum = 0; sidNum < MAX_SIDS; sidNum++) {
				serialNumbers.add(sidBlasterBuilder.getDeviceName(sidNum));
			}
		}
		setActiveSidBlasterDevice(serialNoOfDevice -> serialNumbers.contains(serialNoOfDevice));
	}

	private void setActiveSidBlasterDevice(Predicate<String> serialNoSelector) {
		for (Node node : sidBlasterDeviceParent.getChildren()) {
			SidBlasterDeviceMapping sidBlasterDeviceMapping = (SidBlasterDeviceMapping) node;
			String serialNoOfDevice = sidBlasterDeviceMapping.getSerialNo();

			node.getStyleClass().remove("active");
			if (serialNoSelector.test(serialNoOfDevice)) {
				node.getStyleClass().add("active");
			}
		}
	}

	private String getHostname() {
		try {
			Process proc = Runtime.getRuntime().exec(new String[] { "hostname" }, null, null);
			return IOUtils.convertStreamToString(proc.getInputStream(), UTF_8.name());
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
		Alert alert = new Alert(AlertType.ERROR, msg);
		alert.setTitle(util.getBundle().getString("ALERT_TITLE"));
		alert.showAndWait();
	}

	private void restart() {
		if (!duringInitialization && !duringReplay && util.getPlayer().stateProperty().get() != State.RESTART) {
			util.getPlayer().play(util.getPlayer().getTune());
		}
		duringReplay = false;
	}

}
