package ui.entities.config;

import static java.util.stream.Collectors.toList;
import static server.restful.common.Connectors.HTTP;
import static sidplay.ini.IniDefaults.DEFAULT_3SID_EMULATION;
import static sidplay.ini.IniDefaults.DEFAULT_3SID_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_3SID_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_3SID_MODEL;
import static sidplay.ini.IniDefaults.DEFAULT_CLOCK_SPEED;
import static sidplay.ini.IniDefaults.DEFAULT_DIGI_BOOSTED_8580;
import static sidplay.ini.IniDefaults.DEFAULT_DUAL_SID_BASE;
import static sidplay.ini.IniDefaults.DEFAULT_EMULATION;
import static sidplay.ini.IniDefaults.DEFAULT_ENGINE;
import static sidplay.ini.IniDefaults.DEFAULT_EXSID_FAKE_STEREO;
import static sidplay.ini.IniDefaults.DEFAULT_FAKE_STEREO;
import static sidplay.ini.IniDefaults.DEFAULT_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_FORCE_3SID_TUNE;
import static sidplay.ini.IniDefaults.DEFAULT_FORCE_STEREO_TUNE;
import static sidplay.ini.IniDefaults.DEFAULT_HARD_SID_6581;
import static sidplay.ini.IniDefaults.DEFAULT_HARD_SID_8580;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_STEREO_VOICE1;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_STEREO_VOICE2;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_STEREO_VOICE3;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_STEREO_VOICE4;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_THIRDSID_VOICE1;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_THIRDSID_VOICE2;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_THIRDSID_VOICE3;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_THIRDSID_VOICE4;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_VOICE1;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_VOICE2;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_VOICE3;
import static sidplay.ini.IniDefaults.DEFAULT_MUTE_VOICE4;
import static sidplay.ini.IniDefaults.DEFAULT_NETSIDDEV_HOST;
import static sidplay.ini.IniDefaults.DEFAULT_NETSIDDEV_PORT;
import static sidplay.ini.IniDefaults.DEFAULT_NETSID_3SID_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_NETSID_3SID_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_NETSID_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_NETSID_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_NETSID_STEREO_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_NETSID_STEREO_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_ReSIDfp_3SID_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_ReSIDfp_3SID_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_ReSIDfp_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_ReSIDfp_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_ReSIDfp_STEREO_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_ReSIDfp_STEREO_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_SIDBLASTER_DEVICE_LIST;
import static sidplay.ini.IniDefaults.DEFAULT_SIDBLASTER_LATENCY_TIMER;
import static sidplay.ini.IniDefaults.DEFAULT_SIDBLASTER_READ;
import static sidplay.ini.IniDefaults.DEFAULT_SIDBLASTER_SERIAL_NUMBER;
import static sidplay.ini.IniDefaults.DEFAULT_SIDBLASTER_WRITE_BUFFER_SIZE;
import static sidplay.ini.IniDefaults.DEFAULT_SID_MODEL;
import static sidplay.ini.IniDefaults.DEFAULT_SID_TO_READ;
import static sidplay.ini.IniDefaults.DEFAULT_STEREO_EMULATION;
import static sidplay.ini.IniDefaults.DEFAULT_STEREO_FILTER_6581;
import static sidplay.ini.IniDefaults.DEFAULT_STEREO_FILTER_8580;
import static sidplay.ini.IniDefaults.DEFAULT_STEREO_MODEL;
import static sidplay.ini.IniDefaults.DEFAULT_THIRD_SID_BASE;
import static sidplay.ini.IniDefaults.DEFAULT_ULTIMATE64_HOST;
import static sidplay.ini.IniDefaults.DEFAULT_ULTIMATE64_MODE;
import static sidplay.ini.IniDefaults.DEFAULT_ULTIMATE64_PORT;
import static sidplay.ini.IniDefaults.DEFAULT_ULTIMATE64_SYNC_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_USER_CLOCK_SPEED;
import static sidplay.ini.IniDefaults.DEFAULT_USER_EMULATION;
import static sidplay.ini.IniDefaults.DEFAULT_USER_MODEL;
import static sidplay.ini.IniDefaults.DEFAULT_USE_3SID_FILTER;
import static sidplay.ini.IniDefaults.DEFAULT_USE_FILTER;
import static sidplay.ini.IniDefaults.DEFAULT_USE_STEREO_FILTER;

import java.io.File;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Convert;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Engine;
import libsidplay.common.SidReads;
import libsidplay.common.Ultimate64Mode;
import libsidplay.config.IEmulationSection;
import server.restful.common.Connectors;
import sidplay.ini.converter.BeanToStringConverter;
import sidplay.ini.converter.FileToStringConverter;
import ui.common.converter.FileAttributeConverter;
import ui.common.converter.FileToStringDeserializer;
import ui.common.converter.FileToStringSerializer;
import ui.common.converter.FileXmlAdapter;
import ui.common.properties.LazyListField;
import ui.common.properties.ShadowField;

@Embeddable
@Parameters(resourceBundle = "ui.entities.config.EmulationSection")
public class EmulationSection implements IEmulationSection {

	/**
	 * Auto-detect PSID tune settings.
	 */
	public static final boolean DEFAULT_DETECT_PSID64_CHIP_MODEL = true;

	/**
	 * Connection types of the built-in Android App Server providing REST-based
	 * web-services to play SIDs on the mobile.
	 */
	public static final Connectors DEFAULT_CONNECTORS = HTTP;
	/**
	 * Port of the built-in Android App Server providing REST-based web-services to
	 * play SIDs on the mobile.
	 */
	public static final int DEFAULT_APP_SERVER_PORT = 8080;
	/**
	 * Secure port of the built-in Android App Server providing REST-based
	 * web-services to play SIDs on the mobile.
	 */
	public static final int DEFAULT_APP_SERVER_SECURE_PORT = 8443;

	/**
	 * Hostname of the Ultimate64 streaming target to receive audio/video streams by
	 * UDP network packets.
	 */
	public static final String DEFAULT_ULTIMATE64_STREAMING_TARGET = "127.0.0.1";

	/**
	 * Port of the Ultimate64 streaming target to receive audio streams by UDP
	 * network packets.
	 */
	public static final int DEFAULT_ULTIMATE64_STREAMING_AUDIO_PORT = 30000;

	/**
	 * Port of the Ultimate64 streaming target to receive video streams by UDP
	 * network packets.
	 */
	public static final int DEFAULT_ULTIMATE64_STREAMING_VIDEO_PORT = 30001;

	public static final List<DeviceMapping> DEFAULT_SIDBLASTER_DEVICES = DEFAULT_SIDBLASTER_DEVICE_LIST.stream()
			.map(DeviceMapping::new).collect(toList());

	private ShadowField<ObjectProperty<Engine>, Engine> engine = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_ENGINE);

	@Enumerated(EnumType.STRING)
	@Override
	public Engine getEngine() {
		return this.engine.get();
	}

	@Override
	public void setEngine(Engine engine) {
		this.engine.set(engine);
	}

	public final ObjectProperty<Engine> engineProperty() {
		return engine.property();
	}

	private ShadowField<ObjectProperty<Emulation>, Emulation> defaultEmulation = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_EMULATION);

	@Enumerated(EnumType.STRING)
	@Override
	public Emulation getDefaultEmulation() {
		return this.defaultEmulation.get();
	}

	@Override
	public void setDefaultEmulation(Emulation emulation) {
		this.defaultEmulation.set(emulation);
	}

	public final ObjectProperty<Emulation> defaultEmulationProperty() {
		return defaultEmulation.property();
	}

	private ShadowField<ObjectProperty<Emulation>, Emulation> userEmulation = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_USER_EMULATION);

	@Enumerated(EnumType.STRING)
	@Override
	public Emulation getUserEmulation() {
		return this.userEmulation.get();
	}

	@Override
	public void setUserEmulation(Emulation userEmulation) {
		this.userEmulation.set(userEmulation);
	}

	public final ObjectProperty<Emulation> userEmulationProperty() {
		return userEmulation.property();
	}

	private ShadowField<ObjectProperty<Emulation>, Emulation> stereoEmulation = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_STEREO_EMULATION);

	@Enumerated(EnumType.STRING)
	@Override
	public Emulation getStereoEmulation() {
		return this.stereoEmulation.get();
	}

	@Override
	public void setStereoEmulation(Emulation stereoEmulation) {
		this.stereoEmulation.set(stereoEmulation);
	}

	public final ObjectProperty<Emulation> stereoEmulationProperty() {
		return stereoEmulation.property();
	}

	private ShadowField<ObjectProperty<Emulation>, Emulation> thirdEmulation = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_3SID_EMULATION);

	@Enumerated(EnumType.STRING)
	@Override
	public Emulation getThirdEmulation() {
		return this.thirdEmulation.get();
	}

	@Override
	public void setThirdEmulation(Emulation thirdEmulation) {
		this.thirdEmulation.set(thirdEmulation);
	}

	public final ObjectProperty<Emulation> thirdEmulationProperty() {
		return thirdEmulation.property();
	}

	private ShadowField<ObjectProperty<CPUClock>, CPUClock> defaultClockSpeed = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_CLOCK_SPEED);

	@Enumerated(EnumType.STRING)
	@Override
	public CPUClock getDefaultClockSpeed() {
		return this.defaultClockSpeed.get();
	}

	@Override
	public void setDefaultClockSpeed(CPUClock speed) {
		this.defaultClockSpeed.set(speed);
	}

	public final ObjectProperty<CPUClock> defaultClockSpeedProperty() {
		return defaultClockSpeed.property();
	}

	private ShadowField<ObjectProperty<CPUClock>, CPUClock> userClockSpeed = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_USER_CLOCK_SPEED);

	@Enumerated(EnumType.STRING)
	@Override
	public CPUClock getUserClockSpeed() {
		return userClockSpeed.get();
	}

	@Override
	public void setUserClockSpeed(CPUClock userClockSpeed) {
		this.userClockSpeed.set(userClockSpeed);
	}

	public final ObjectProperty<CPUClock> userClockSpeedProperty() {
		return userClockSpeed.property();
	}

	private ShadowField<ObjectProperty<ChipModel>, ChipModel> defaultSidModel = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_SID_MODEL);

	@Enumerated(EnumType.STRING)
	@Override
	public ChipModel getDefaultSidModel() {
		return defaultSidModel.get();
	}

	@Override
	public void setDefaultSidModel(ChipModel defaultSidModel) {
		this.defaultSidModel.set(defaultSidModel);
	}

	public final ObjectProperty<ChipModel> defaultSidModelProperty() {
		return defaultSidModel.property();
	}

	private ShadowField<ObjectProperty<ChipModel>, ChipModel> userSidModel = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_USER_MODEL);

	@Enumerated(EnumType.STRING)
	@Override
	public ChipModel getUserSidModel() {
		return userSidModel.get();
	}

	@Override
	public void setUserSidModel(ChipModel userSidModel) {
		this.userSidModel.set(userSidModel);
	}

	public final ObjectProperty<ChipModel> userSidModelProperty() {
		return userSidModel.property();
	}

	private ShadowField<ObjectProperty<ChipModel>, ChipModel> stereoSidModel = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_STEREO_MODEL);

	@Enumerated(EnumType.STRING)
	@Override
	public ChipModel getStereoSidModel() {
		return stereoSidModel.get();
	}

	@Override
	public void setStereoSidModel(ChipModel stereoSidModel) {
		this.stereoSidModel.set(stereoSidModel);
	}

	public final ObjectProperty<ChipModel> stereoSidModelProperty() {
		return stereoSidModel.property();
	}

	private ShadowField<ObjectProperty<ChipModel>, ChipModel> thirdSIDModel = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_3SID_MODEL);

	@Enumerated(EnumType.STRING)
	@Override
	public ChipModel getThirdSIDModel() {
		return thirdSIDModel.get();
	}

	@Override
	public void setThirdSIDModel(ChipModel stereoSidModel) {
		this.thirdSIDModel.set(stereoSidModel);
	}

	public final ObjectProperty<ChipModel> thirdSIDModelProperty() {
		return thirdSIDModel.property();
	}

	private ShadowField<BooleanProperty, Boolean> detectPSID64ChipModel = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_DETECT_PSID64_CHIP_MODEL);

	@Override
	public boolean isDetectPSID64ChipModel() {
		return detectPSID64ChipModel.get();
	}

	@Override
	public void setDetectPSID64ChipModel(boolean detectPSID64ChipModel) {
		this.detectPSID64ChipModel.set(detectPSID64ChipModel);
	}

	public final BooleanProperty detectPSID64ChipModelProperty() {
		return detectPSID64ChipModel.property();
	}

	private ShadowField<ObjectProperty<Integer>, Integer> hardsid6581 = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_HARD_SID_6581);

	@Override
	public int getHardsid6581() {
		return hardsid6581.get();
	}

	@Override
	public void setHardsid6581(int hardsid6581) {
		this.hardsid6581.set(hardsid6581);
	}

	public final ObjectProperty<Integer> hardsid6581Property() {
		return hardsid6581.property();
	}

	private ShadowField<ObjectProperty<Integer>, Integer> hardsid8580 = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_HARD_SID_8580);

	@Override
	public int getHardsid8580() {
		return hardsid8580.get();
	}

	@Override
	public void setHardsid8580(int hardsid8580) {
		this.hardsid8580.set(hardsid8580);
	}

	public final ObjectProperty<Integer> hardsid8580Property() {
		return hardsid8580.property();
	}

	private LazyListField<DeviceMapping> sidBlasterDeviceList = new LazyListField<>();

	@OneToMany(cascade = CascadeType.ALL)
	@Override
	public List<DeviceMapping> getSidBlasterDeviceList() {
		return sidBlasterDeviceList
				.get(() -> DEFAULT_SIDBLASTER_DEVICES.stream().map(DeviceMapping::new).collect(toList()));
	}

	public void setSidBlasterDeviceList(List<DeviceMapping> sidBlasterDeviceList) {
		this.sidBlasterDeviceList.set(sidBlasterDeviceList);
	}

	private ShadowField<ObjectProperty<Integer>, Integer> sidBlasterWriteBufferSize = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_SIDBLASTER_WRITE_BUFFER_SIZE);

	@Override
	public int getSidBlasterWriteBufferSize() {
		return sidBlasterWriteBufferSize.get();
	}

	@Override
	public void setSidBlasterWriteBufferSize(int sidBlasterWriteBufferSize) {
		this.sidBlasterWriteBufferSize.set(sidBlasterWriteBufferSize);
	}

	public final ObjectProperty<Integer> sidBlasterWriteBufferSizeProperty() {
		return sidBlasterWriteBufferSize.property();
	}

	private ShadowField<StringProperty, String> sidBlasterSerialNumber = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_SIDBLASTER_SERIAL_NUMBER);

	@Override
	public String getSidBlasterSerialNumber() {
		return sidBlasterSerialNumber.get();
	}

	@Override
	public void setSidBlasterSerialNumber(String sidBlasterSerialNumber) {
		this.sidBlasterSerialNumber.set(sidBlasterSerialNumber);
	}

	public final StringProperty sidBlasterSerialNumberProperty() {
		return sidBlasterSerialNumber.property();
	}

	private ShadowField<BooleanProperty, Boolean> sidBlasterRead = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_SIDBLASTER_READ);

	@Override
	public boolean isSidBlasterRead() {
		return sidBlasterRead.get();
	}

	@Override
	public void setSidBlasterRead(boolean sidBlasterRead) {
		this.sidBlasterRead.set(sidBlasterRead);
	}

	public final BooleanProperty sidBlasterReadProperty() {
		return sidBlasterRead.property();
	}

	private ShadowField<ObjectProperty<Short>, Short> sidBlasterLatencyTimer = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_SIDBLASTER_LATENCY_TIMER);

	@Override
	public short getSidBlasterLatencyTimer() {
		return sidBlasterLatencyTimer.get();
	}

	@Override
	public void setSidBlasterLatencyTimer(short sidBlasterLatencyTimer) {
		this.sidBlasterLatencyTimer.set(sidBlasterLatencyTimer);
	}

	public final ObjectProperty<Short> sidBlasterLatencyTimerProperty() {
		return sidBlasterLatencyTimer.property();
	}

	private ShadowField<BooleanProperty, Boolean> exsidFakeStereo = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_EXSID_FAKE_STEREO);

	@Override
	public boolean isExsidFakeStereo() {
		return exsidFakeStereo.get();
	}

	@Override
	public void setExsidFakeStereo(boolean exsidFakeStereo) {
		this.exsidFakeStereo.set(exsidFakeStereo);
	}

	public final BooleanProperty exsidFakeStereoProperty() {
		return exsidFakeStereo.property();
	}

	private ShadowField<StringProperty, String> netSidDevHost = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_NETSIDDEV_HOST);

	@Override
	public String getNetSIDDevHost() {
		return netSidDevHost.get();
	}

	@Override
	public void setNetSIDDevHost(String hostname) {
		this.netSidDevHost.set(hostname);
	}

	public final StringProperty netSidDevHostProperty() {
		return netSidDevHost.property();
	}

	private ShadowField<ObjectProperty<Integer>, Integer> netSidDevPort = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_NETSIDDEV_PORT);

	@Override
	public int getNetSIDDevPort() {
		return netSidDevPort.get();
	}

	@Override
	public void setNetSIDDevPort(int port) {
		this.netSidDevPort.set(port);
	}

	public final ObjectProperty<Integer> netSidDevPortProperty() {
		return netSidDevPort.property();
	}

	private ShadowField<ObjectProperty<Ultimate64Mode>, Ultimate64Mode> ultimate64Mode = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_ULTIMATE64_MODE);

	@Enumerated(EnumType.STRING)
	@Override
	public Ultimate64Mode getUltimate64Mode() {
		return ultimate64Mode.get();
	}

	@Override
	public void setUltimate64Mode(Ultimate64Mode ultimate64Mode) {
		this.ultimate64Mode.set(ultimate64Mode);
	}

	public final ObjectProperty<Ultimate64Mode> ultimate64ModeProperty() {
		return ultimate64Mode.property();
	}

	private ShadowField<StringProperty, String> ultimate64Host = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_ULTIMATE64_HOST);

	@Override
	public String getUltimate64Host() {
		return ultimate64Host.get();
	}

	@Override
	public void setUltimate64Host(String hostname) {
		this.ultimate64Host.set(hostname);
	}

	public final StringProperty ultimate64HostProperty() {
		return ultimate64Host.property();
	}

	private ShadowField<ObjectProperty<Integer>, Integer> ultimate64Port = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_ULTIMATE64_PORT);

	@Override
	public int getUltimate64Port() {
		return ultimate64Port.get();
	}

	@Override
	public void setUltimate64Port(int port) {
		this.ultimate64Port.set(port);
	}

	public final ObjectProperty<Integer> ultimate64PortProperty() {
		return ultimate64Port.property();
	}

	private ShadowField<ObjectProperty<Integer>, Integer> ultimate64SyncDelay = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_ULTIMATE64_SYNC_DELAY);

	@Override
	public int getUltimate64SyncDelay() {
		return ultimate64SyncDelay.get();
	}

	@Override
	public void setUltimate64SyncDelay(int syncDelay) {
		this.ultimate64SyncDelay.set(syncDelay);
	}

	public final ObjectProperty<Integer> ultimate64SyncDelayProperty() {
		return ultimate64SyncDelay.property();
	}

	private ShadowField<StringProperty, String> ultimate64StreamingTarget = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_ULTIMATE64_STREAMING_TARGET);

	public String getUltimate64StreamingTarget() {
		return ultimate64StreamingTarget.get();
	}

	public void setUltimate64StreamingTarget(String ultimate64StreamingTarget) {
		this.ultimate64StreamingTarget.set(ultimate64StreamingTarget);
	}

	public final StringProperty ultimate64StreamingTargetProperty() {
		return ultimate64StreamingTarget.property();
	}

	private ShadowField<ObjectProperty<Integer>, Integer> ultimate64StreamingAudioPort = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_ULTIMATE64_STREAMING_AUDIO_PORT);

	public int getUltimate64StreamingAudioPort() {
		return ultimate64StreamingAudioPort.get();
	}

	public void setUltimate64StreamingAudioPort(int ultimate64StreamingAudioPort) {
		this.ultimate64StreamingAudioPort.set(ultimate64StreamingAudioPort);
	}

	public final ObjectProperty<Integer> ultimate64StreamingAudioPortProperty() {
		return ultimate64StreamingAudioPort.property();
	}

	private ShadowField<ObjectProperty<Integer>, Integer> ultimate64StreamingVideoPort = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_ULTIMATE64_STREAMING_VIDEO_PORT);

	public int getUltimate64StreamingVideoPort() {
		return ultimate64StreamingVideoPort.get();
	}

	public void setUltimate64StreamingVideoPort(int ultimate64StreamingVideoPort) {
		this.ultimate64StreamingVideoPort.set(ultimate64StreamingVideoPort);
	}

	public final ObjectProperty<Integer> ultimate64StreamingVideoPortProperty() {
		return ultimate64StreamingVideoPort.property();
	}

	private ShadowField<ObjectProperty<Connectors>, Connectors> appServerConnectors = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_CONNECTORS);

	@Enumerated(EnumType.STRING)
	public Connectors getAppServerConnectors() {
		return this.appServerConnectors.get();
	}

	@Parameter(names = { "--appServerConnectors" }, descriptionKey = "APP_SERVER_CONNECTORS", order = 1000)
	public void setAppServerConnectors(Connectors appServerConnectors) {
		this.appServerConnectors.set(appServerConnectors);
	}

	public final ObjectProperty<Connectors> appServerConnectorsProperty() {
		return appServerConnectors.property();
	}

	private ShadowField<ObjectProperty<Integer>, Integer> appServerPort = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_APP_SERVER_PORT);

	public int getAppServerPort() {
		return appServerPort.get();
	}

	@Parameter(names = { "--appServerPort" }, descriptionKey = "APP_SERVER_PORT", order = 1001)
	public void setAppServerPort(int port) {
		this.appServerPort.set(port);
	}

	public final ObjectProperty<Integer> appServerPortProperty() {
		return appServerPort.property();
	}

	private ShadowField<ObjectProperty<Integer>, Integer> appServerSecurePort = new ShadowField<>(
			SimpleObjectProperty::new, DEFAULT_APP_SERVER_SECURE_PORT);

	public int getAppServerSecurePort() {
		return appServerSecurePort.get();
	}

	@Parameter(names = { "--appServerSecurePort" }, descriptionKey = "APP_SERVER_SECURE_PORT", order = 1002)
	public void setAppServerSecurePort(int securePort) {
		this.appServerSecurePort.set(securePort);
	}

	public final ObjectProperty<Integer> appServerSecurePortProperty() {
		return appServerSecurePort.property();
	}

	private ShadowField<ObjectProperty<File>, File> appServerKeystore = new ShadowField<>(SimpleObjectProperty::new,
			null);

	@Convert(converter = FileAttributeConverter.class)
	@XmlJavaTypeAdapter(FileXmlAdapter.class)
	@JsonSerialize(using = FileToStringSerializer.class)
	@JsonDeserialize(using = FileToStringDeserializer.class)
	public File getAppServerKeystoreFile() {
		return this.appServerKeystore.get();
	}

	@Parameter(names = {
			"--appServerKeystore" }, descriptionKey = "APP_SERVER_KEYSTORE", converter = FileToStringConverter.class, order = 1003)
	public void setAppServerKeystoreFile(File appServerKeystoreFile) {
		this.appServerKeystore.set(appServerKeystoreFile);
	}

	public final ObjectProperty<File> appServerKeystoreFile() {
		return this.appServerKeystore.property();
	}

	private ShadowField<StringProperty, String> appServerKeystorePassword = new ShadowField<>(SimpleStringProperty::new,
			null);

	/**
	 * <b>Note:</b> security reasons make it necessary to remove passwords!
	 */
	@Transient
	@XmlTransient
	@JsonIgnore
	public final String getAppServerKeystorePassword() {
		return appServerKeystorePassword.get();
	}

	@Parameter(names = { "--appServerKeystorePassword" }, descriptionKey = "APP_SERVER_KEYSTORE_PASSWORD", order = 1004)
	public final void setAppServerKeystorePassword(String appServerKeyStorePassword) {
		this.appServerKeystorePassword.set(appServerKeyStorePassword);
	}

	public final StringProperty appServerKeystorePasswordProperty() {
		return appServerKeystorePassword.property();
	}

	private ShadowField<StringProperty, String> appServerKeyAlias = new ShadowField<>(SimpleStringProperty::new, null);

	public String getAppServerKeyAlias() {
		return appServerKeyAlias.get();
	}

	@Parameter(names = { "--appServerKeyAlias" }, descriptionKey = "APP_SERVER_KEY_ALIAS", order = 1005)
	public void setAppServerKeyAlias(String appServerKeyAlias) {
		this.appServerKeyAlias.set(appServerKeyAlias);
	}

	public final StringProperty appServerKeyAliasProperty() {
		return appServerKeyAlias.property();
	}

	private ShadowField<StringProperty, String> appServerKeyPassword = new ShadowField<>(SimpleStringProperty::new,
			null);

	/**
	 * <b>Note:</b> security reasons make it necessary to remove passwords!
	 */
	@Transient
	@XmlTransient
	@JsonIgnore
	public final String getAppServerKeyPassword() {
		return appServerKeyPassword.get();
	}

	@Parameter(names = { "--appServerKeyPassword" }, descriptionKey = "APP_SERVER_KEY_PASSWORD", order = 1006)
	public final void setAppServerKeyPassword(String appServerKeyPassword) {
		this.appServerKeyPassword.set(appServerKeyPassword);
	}

	public final StringProperty appServerKeyPasswordProperty() {
		return appServerKeyPassword.property();
	}

	private ShadowField<BooleanProperty, Boolean> filter = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_USE_FILTER);

	@Override
	public boolean isFilter() {
		return filter.get();
	}

	@Override
	public void setFilter(boolean isFilter) {
		this.filter.set(isFilter);
	}

	public final BooleanProperty filterProperty() {
		return filter.property();
	}

	private ShadowField<BooleanProperty, Boolean> stereoFilter = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_USE_STEREO_FILTER);

	@Override
	public boolean isStereoFilter() {
		return stereoFilter.get();
	}

	@Override
	public void setStereoFilter(boolean isFilter) {
		this.stereoFilter.set(isFilter);
	}

	public final BooleanProperty stereoFilterProperty() {
		return stereoFilter.property();
	}

	private ShadowField<BooleanProperty, Boolean> thirdSIDFilter = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_USE_3SID_FILTER);

	@Override
	public boolean isThirdSIDFilter() {
		return thirdSIDFilter.get();
	}

	@Override
	public void setThirdSIDFilter(boolean isFilter) {
		this.thirdSIDFilter.set(isFilter);
	}

	public final BooleanProperty thirdSIDFilterProperty() {
		return thirdSIDFilter.property();
	}

	private ShadowField<ObjectProperty<SidReads>, SidReads> sidToRead = new ShadowField<>(SimpleObjectProperty::new,
			DEFAULT_SID_TO_READ);

	@Enumerated(EnumType.STRING)
	@Override
	public SidReads getSidToRead() {
		return this.sidToRead.get();
	}

	@Override
	public void setSidToRead(SidReads engine) {
		this.sidToRead.set(engine);
	}

	public final ObjectProperty<SidReads> sidToReadProperty() {
		return sidToRead.property();
	}

	private ShadowField<BooleanProperty, Boolean> digiBoosted8580 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_DIGI_BOOSTED_8580);

	@Override
	public boolean isDigiBoosted8580() {
		return digiBoosted8580.get();
	}

	@Override
	public void setDigiBoosted8580(boolean isDigiBoosted8580) {
		this.digiBoosted8580.set(isDigiBoosted8580);
	}

	public final BooleanProperty digiBoosted8580Property() {
		return digiBoosted8580.property();
	}

	private ShadowField<BooleanProperty, Boolean> fakeStereo = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_FAKE_STEREO);

	@Override
	public boolean isFakeStereo() {
		return fakeStereo.get();
	}

	@Override
	public void setFakeStereo(boolean fakeStereo) {
		this.fakeStereo.set(fakeStereo);
	}

	public final BooleanProperty fakeStereoProperty() {
		return fakeStereo.property();
	}

	private ShadowField<IntegerProperty, Number> dualSidBase = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_DUAL_SID_BASE);

	@Override
	public int getDualSidBase() {
		return dualSidBase.get().intValue();
	}

	@Override
	public void setDualSidBase(int dualSidBase) {
		this.dualSidBase.set(dualSidBase);
	}

	public final IntegerProperty dualSidBaseProperty() {
		return dualSidBase.property();
	}

	private ShadowField<IntegerProperty, Number> thirdSIDBase = new ShadowField<>(
			number -> new SimpleIntegerProperty(number.intValue()), DEFAULT_THIRD_SID_BASE);

	@Override
	public int getThirdSIDBase() {
		return thirdSIDBase.get().intValue();
	}

	@Override
	public void setThirdSIDBase(int dualSidBase) {
		this.thirdSIDBase.set(dualSidBase);
	}

	public final IntegerProperty thirdSIDBaseProperty() {
		return thirdSIDBase.property();
	}

	private ShadowField<BooleanProperty, Boolean> forceStereoTune = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_FORCE_STEREO_TUNE);

	@Override
	public boolean isForceStereoTune() {
		return forceStereoTune.get();
	}

	@Override
	public void setForceStereoTune(boolean isForceStereoTune) {
		this.forceStereoTune.set(isForceStereoTune);
	}

	public final BooleanProperty forceStereoTuneProperty() {
		return forceStereoTune.property();
	}

	private ShadowField<BooleanProperty, Boolean> force3SIDTune = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_FORCE_3SID_TUNE);

	@Override
	public boolean isForce3SIDTune() {
		return force3SIDTune.get();
	}

	@Override
	public void setForce3SIDTune(boolean isForceStereoTune) {
		this.force3SIDTune.set(isForceStereoTune);
	}

	public final BooleanProperty force3SIDTuneProperty() {
		return force3SIDTune.property();
	}

	private ShadowField<BooleanProperty, Boolean> muteVoice1 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_MUTE_VOICE1);

	@Override
	public boolean isMuteVoice1() {
		return muteVoice1.get();
	}

	@Override
	public void setMuteVoice1(boolean mute) {
		muteVoice1.set(mute);
	}

	public final BooleanProperty muteVoice1Property() {
		return muteVoice1.property();
	}

	private ShadowField<BooleanProperty, Boolean> muteVoice2 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_MUTE_VOICE2);

	@Override
	public boolean isMuteVoice2() {
		return muteVoice2.get();
	}

	@Override
	public void setMuteVoice2(boolean mute) {
		muteVoice2.set(mute);
	}

	public final BooleanProperty muteVoice2Property() {
		return muteVoice2.property();
	}

	private ShadowField<BooleanProperty, Boolean> muteVoice3 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_MUTE_VOICE3);

	@Override
	public boolean isMuteVoice3() {
		return muteVoice3.get();
	}

	@Override
	public void setMuteVoice3(boolean mute) {
		muteVoice3.set(mute);
	}

	public final BooleanProperty muteVoice3Property() {
		return muteVoice3.property();
	}

	private ShadowField<BooleanProperty, Boolean> muteVoice4 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_MUTE_VOICE4);

	@Override
	public boolean isMuteVoice4() {
		return muteVoice4.get();
	}

	@Override
	public void setMuteVoice4(boolean mute) {
		muteVoice4.set(mute);
	}

	public final BooleanProperty muteVoice4Property() {
		return muteVoice4.property();
	}

	private ShadowField<BooleanProperty, Boolean> muteStereoVoice1 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_MUTE_STEREO_VOICE1);

	@Override
	public boolean isMuteStereoVoice1() {
		return muteStereoVoice1.get();
	}

	@Override
	public void setMuteStereoVoice1(boolean mute) {
		muteStereoVoice1.set(mute);
	}

	public final BooleanProperty muteStereoVoice1Property() {
		return muteStereoVoice1.property();
	}

	private ShadowField<BooleanProperty, Boolean> muteStereoVoice2 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_MUTE_STEREO_VOICE2);

	@Override
	public boolean isMuteStereoVoice2() {
		return muteStereoVoice2.get();
	}

	@Override
	public void setMuteStereoVoice2(boolean mute) {
		muteStereoVoice2.set(mute);
	}

	public final BooleanProperty muteStereoVoice2Property() {
		return muteStereoVoice2.property();
	}

	private ShadowField<BooleanProperty, Boolean> muteStereoVoice3 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_MUTE_STEREO_VOICE3);

	@Override
	public boolean isMuteStereoVoice3() {
		return muteStereoVoice3.get();
	}

	@Override
	public void setMuteStereoVoice3(boolean mute) {
		muteStereoVoice3.set(mute);
	}

	public final BooleanProperty muteStereoVoice3Property() {
		return muteStereoVoice3.property();
	}

	private ShadowField<BooleanProperty, Boolean> muteStereoVoice4 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_MUTE_STEREO_VOICE4);

	@Override
	public boolean isMuteStereoVoice4() {
		return muteStereoVoice4.get();
	}

	@Override
	public void setMuteStereoVoice4(boolean mute) {
		muteStereoVoice4.set(mute);
	}

	public final BooleanProperty muteStereoVoice4Property() {
		return muteStereoVoice4.property();
	}

	private ShadowField<BooleanProperty, Boolean> muteThirdSIDVoice1 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_MUTE_THIRDSID_VOICE1);

	@Override
	public boolean isMuteThirdSIDVoice1() {
		return muteThirdSIDVoice1.get();
	}

	@Override
	public void setMuteThirdSIDVoice1(boolean mute) {
		muteThirdSIDVoice1.set(mute);
	}

	public final BooleanProperty muteThirdSIDVoice1Property() {
		return muteThirdSIDVoice1.property();
	}

	private ShadowField<BooleanProperty, Boolean> muteThirdSIDVoice2 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_MUTE_THIRDSID_VOICE2);

	@Override
	public boolean isMuteThirdSIDVoice2() {
		return muteThirdSIDVoice2.get();
	}

	@Override
	public void setMuteThirdSIDVoice2(boolean mute) {
		muteThirdSIDVoice2.set(mute);
	}

	public final BooleanProperty muteThirdSIDVoice2Property() {
		return muteThirdSIDVoice2.property();
	}

	private ShadowField<BooleanProperty, Boolean> muteThirdSIDVoice3 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_MUTE_THIRDSID_VOICE3);

	@Override
	public boolean isMuteThirdSIDVoice3() {
		return muteThirdSIDVoice3.get();
	}

	@Override
	public void setMuteThirdSIDVoice3(boolean mute) {
		muteThirdSIDVoice3.set(mute);
	}

	public final BooleanProperty muteThirdSIDVoice3Property() {
		return muteThirdSIDVoice3.property();
	}

	private ShadowField<BooleanProperty, Boolean> muteThirdSIDVoice4 = new ShadowField<>(SimpleBooleanProperty::new,
			DEFAULT_MUTE_THIRDSID_VOICE4);

	@Override
	public boolean isMuteThirdSIDVoice4() {
		return muteThirdSIDVoice4.get();
	}

	@Override
	public void setMuteThirdSIDVoice4(boolean mute) {
		muteThirdSIDVoice4.set(mute);
	}

	public final BooleanProperty muteThirdSIDVoice4Property() {
		return muteThirdSIDVoice4.property();
	}

	private ShadowField<StringProperty, String> netSIDFilter6581 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_NETSID_FILTER_6581);

	@Override
	public String getNetSIDFilter6581() {
		return netSIDFilter6581.get();
	}

	@Override
	public void setNetSIDFilter6581(String netSIDFilter6581) {
		this.netSIDFilter6581.set(netSIDFilter6581);
	}

	public final StringProperty netSIDFilter6581Property() {
		return netSIDFilter6581.property();
	}

	private ShadowField<StringProperty, String> netSIDStereoFilter6581 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_NETSID_STEREO_FILTER_6581);

	@Override
	public String getNetSIDStereoFilter6581() {
		return netSIDStereoFilter6581.get();
	}

	@Override
	public void setNetSIDStereoFilter6581(String netSIDFilter6581) {
		this.netSIDStereoFilter6581.set(netSIDFilter6581);
	}

	public final StringProperty netSIDStereoFilter6581Property() {
		return netSIDStereoFilter6581.property();
	}

	private ShadowField<StringProperty, String> netSID3rdSIDFilter6581 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_NETSID_3SID_FILTER_6581);

	@Override
	public String getNetSIDThirdSIDFilter6581() {
		return netSID3rdSIDFilter6581.get();
	}

	@Override
	public void setNetSIDThirdSIDFilter6581(String netSIDFilter6581) {
		this.netSID3rdSIDFilter6581.set(netSIDFilter6581);
	}

	public final StringProperty netSID3rdSIDFilter6581Property() {
		return netSID3rdSIDFilter6581.property();
	}

	private ShadowField<StringProperty, String> netSIDFilter8580 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_NETSID_FILTER_8580);

	@Override
	public String getNetSIDFilter8580() {
		return netSIDFilter8580.get();
	}

	@Override
	public void setNetSIDFilter8580(String netSIDFilter8580) {
		this.netSIDFilter8580.set(netSIDFilter8580);
	}

	public final StringProperty netSIDFilter8580Property() {
		return netSIDFilter8580.property();
	}

	private ShadowField<StringProperty, String> netSIDStereoFilter8580 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_NETSID_STEREO_FILTER_8580);

	@Override
	public String getNetSIDStereoFilter8580() {
		return netSIDStereoFilter8580.get();
	}

	@Override
	public void setNetSIDStereoFilter8580(String netSIDFilter8580) {
		this.netSIDStereoFilter8580.set(netSIDFilter8580);
	}

	public final StringProperty netSIDStereoFilter858Property() {
		return netSIDStereoFilter8580.property();
	}

	private ShadowField<StringProperty, String> netSID3rdSIDFilter8580 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_NETSID_3SID_FILTER_8580);

	@Override
	public String getNetSIDThirdSIDFilter8580() {
		return netSID3rdSIDFilter8580.get();
	}

	@Override
	public void setNetSIDThirdSIDFilter8580(String netSIDFilter8580) {
		this.netSID3rdSIDFilter8580.set(netSIDFilter8580);
	}

	public final StringProperty netSID3rdSIDFilter8580Property() {
		return netSID3rdSIDFilter8580.property();
	}

	private ShadowField<StringProperty, String> filter6581 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_FILTER_6581);

	@Override
	public String getFilter6581() {
		return filter6581.get();
	}

	@Override
	public void setFilter6581(String filter6581) {
		this.filter6581.set(filter6581);
	}

	public final StringProperty filter6581Property() {
		return filter6581.property();
	}

	private ShadowField<StringProperty, String> stereoFilter6581 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_STEREO_FILTER_6581);

	@Override
	public String getStereoFilter6581() {
		return stereoFilter6581.get();
	}

	@Override
	public void setStereoFilter6581(String filter6581) {
		this.stereoFilter6581.set(filter6581);
	}

	public final StringProperty stereoFilter6581Property() {
		return stereoFilter6581.property();
	}

	private ShadowField<StringProperty, String> thirdSIDFilter6581 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_3SID_FILTER_6581);

	@Override
	public String getThirdSIDFilter6581() {
		return thirdSIDFilter6581.get();
	}

	@Override
	public void setThirdSIDFilter6581(String filter6581) {
		this.thirdSIDFilter6581.set(filter6581);
	}

	public final StringProperty thirdSIDFilter6581Property() {
		return thirdSIDFilter6581.property();
	}

	private ShadowField<StringProperty, String> filter8580 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_FILTER_8580);

	@Override
	public String getFilter8580() {
		return filter8580.get();
	}

	@Override
	public void setFilter8580(String filter8580) {
		this.filter8580.set(filter8580);
	}

	public final StringProperty filter8580Property() {
		return filter8580.property();
	}

	private ShadowField<StringProperty, String> stereoFilter8580 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_STEREO_FILTER_8580);

	@Override
	public String getStereoFilter8580() {
		return stereoFilter8580.get();
	}

	@Override
	public void setStereoFilter8580(String filter8580) {
		this.stereoFilter8580.set(filter8580);
	}

	public final StringProperty stereoFilter8580Property() {
		return stereoFilter8580.property();
	}

	private ShadowField<StringProperty, String> thirdSIDFilter8580 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_3SID_FILTER_8580);

	@Override
	public String getThirdSIDFilter8580() {
		return thirdSIDFilter8580.get();
	}

	@Override
	public void setThirdSIDFilter8580(String filter8580) {
		this.thirdSIDFilter8580.set(filter8580);
	}

	public final StringProperty thirdSIDFilter8580Property() {
		return thirdSIDFilter8580.property();
	}

	private ShadowField<StringProperty, String> reSIDfpFilter6581 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_ReSIDfp_FILTER_6581);

	@Override
	public String getReSIDfpFilter6581() {
		return reSIDfpFilter6581.get();
	}

	@Override
	public void setReSIDfpFilter6581(String reSIDfpFilter6581) {
		this.reSIDfpFilter6581.set(reSIDfpFilter6581);
	}

	public final StringProperty reSIDfpFilter6581Property() {
		return reSIDfpFilter6581.property();
	}

	private ShadowField<StringProperty, String> reSIDfpStereoFilter6581 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_ReSIDfp_STEREO_FILTER_6581);

	@Override
	public String getReSIDfpStereoFilter6581() {
		return reSIDfpStereoFilter6581.get();
	}

	@Override
	public void setReSIDfpStereoFilter6581(String reSIDfpFilter6581) {
		this.reSIDfpStereoFilter6581.set(reSIDfpFilter6581);
	}

	public final StringProperty reSIDfpStereoFilter6581Property() {
		return reSIDfpStereoFilter6581.property();
	}

	private ShadowField<StringProperty, String> reSIDfp3rdSIDFilter6581 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_ReSIDfp_3SID_FILTER_6581);

	@Override
	public String getReSIDfpThirdSIDFilter6581() {
		return reSIDfp3rdSIDFilter6581.get();
	}

	@Override
	public void setReSIDfpThirdSIDFilter6581(String reSIDfpFilter6581) {
		this.reSIDfp3rdSIDFilter6581.set(reSIDfpFilter6581);
	}

	public final StringProperty reSIDfp3rdSIDFilter6581Property() {
		return reSIDfp3rdSIDFilter6581.property();
	}

	private ShadowField<StringProperty, String> reSIDfpFilter8580 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_ReSIDfp_FILTER_8580);

	@Override
	public String getReSIDfpFilter8580() {
		return reSIDfpFilter8580.get();
	}

	@Override
	public void setReSIDfpFilter8580(String reSIDfpFilter8580) {
		this.reSIDfpFilter8580.set(reSIDfpFilter8580);
	}

	public final StringProperty reSIDfpFilter8580Property() {
		return reSIDfpFilter8580.property();
	}

	private ShadowField<StringProperty, String> reSIDfpStereoFilter8580 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_ReSIDfp_STEREO_FILTER_8580);

	@Override
	public String getReSIDfpStereoFilter8580() {
		return reSIDfpStereoFilter8580.get();
	}

	@Override
	public void setReSIDfpStereoFilter8580(String reSIDfpFilter8580) {
		this.reSIDfpStereoFilter8580.set(reSIDfpFilter8580);
	}

	public final StringProperty reSIDfpStereoFilter858Property() {
		return reSIDfpStereoFilter8580.property();
	}

	private ShadowField<StringProperty, String> reSIDfp3rdSIDFilter8580 = new ShadowField<>(SimpleStringProperty::new,
			DEFAULT_ReSIDfp_3SID_FILTER_8580);

	@Override
	public String getReSIDfpThirdSIDFilter8580() {
		return reSIDfp3rdSIDFilter8580.get();
	}

	@Override
	public void setReSIDfpThirdSIDFilter8580(String reSIDfpFilter8580) {
		this.reSIDfp3rdSIDFilter8580.set(reSIDfpFilter8580);
	}

	public final StringProperty reSIDfp3rdSIDFilter8580Property() {
		return reSIDfp3rdSIDFilter8580.property();
	}

	private OverrideSection overrideSection = new OverrideSection();

	@Transient
	@XmlTransient
	@JsonIgnore
	@Override
	public final OverrideSection getOverrideSection() {
		return overrideSection;
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}
}
