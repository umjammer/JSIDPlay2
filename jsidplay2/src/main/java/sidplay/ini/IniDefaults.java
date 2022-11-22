package sidplay.ini;

import java.io.File;
import java.util.List;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Engine;
import libsidplay.common.SamplingMethod;
import libsidplay.common.SamplingRate;
import libsidplay.common.SidReads;
import libsidplay.common.Ultimate64Mode;
import libsidplay.common.VideoCoderPreset;
import libsidplay.components.c1541.FloppyType;
import libsidplay.config.IAudioSection;
import libsidplay.config.IC1541Section;
import libsidplay.config.IDeviceMapping;
import libsidplay.config.IEmulationSection;
import libsidplay.config.IPrinterSection;
import libsidplay.config.ISidPlay2Section;
import libsidplay.config.IWhatsSidSection;
import sidplay.audio.Audio;

/**
 * Provide constants for all settings read from the internal INI file.
 *
 * @author ken
 *
 */
public interface IniDefaults {
	IniConfig DEFAULTS = IniConfig.getDefault();

	// SIDPlay2 Section

	ISidPlay2Section SIDPLAY2_SECTION = DEFAULTS.getSidplay2Section();
	boolean DEFAULT_ENABLE_DATABASE = SIDPLAY2_SECTION.isEnableDatabase();
	double DEFAULT_START_TIME = SIDPLAY2_SECTION.getStartTime();
	double DEFAULT_PLAY_LENGTH = SIDPLAY2_SECTION.getDefaultPlayLength();
	double DEFAULT_FADE_IN_TIME = SIDPLAY2_SECTION.getFadeInTime();
	double DEFAULT_FADE_OUT_TIME = SIDPLAY2_SECTION.getFadeOutTime();
	boolean DEFAULT_LOOP = SIDPLAY2_SECTION.isLoop();
	boolean DEFAULT_SINGLE_TRACK = SIDPLAY2_SECTION.isSingle();
	File DEFAULT_HVSC_DIR = SIDPLAY2_SECTION.getHvsc();
	File DEFAULT_LAST_DIR = SIDPLAY2_SECTION.getLastDirectory();
	boolean DEFAULT_PAL_EMULATION = SIDPLAY2_SECTION.isPalEmulation();
	File DEFAULT_TMP_DIR = new File(System.getProperty("user.home"), ".jsidplay2");
	float DEFAULT_BRIGHTNESS = SIDPLAY2_SECTION.getBrightness();
	float DEFAULT_CONTRAST = SIDPLAY2_SECTION.getContrast();
	float DEFAULT_GAMMA = SIDPLAY2_SECTION.getGamma();
	float DEFAULT_SATURATION = SIDPLAY2_SECTION.getSaturation();
	float DEFAULT_PHASE_SHIFT = SIDPLAY2_SECTION.getPhaseShift();
	float DEFAULT_OFFSET = SIDPLAY2_SECTION.getOffset();
	float DEFAULT_TINT = SIDPLAY2_SECTION.getTint();
	float DEFAULT_BLUR = SIDPLAY2_SECTION.getBlur();
	float DEFAULT_BLEED = SIDPLAY2_SECTION.getBleed();
	boolean DEFAULT_TURBO_TAPE = SIDPLAY2_SECTION.isTurboTape();

	// C1541 Section

	IC1541Section C1541_SECTION = DEFAULTS.getC1541Section();
	boolean DEFAULT_DRIVE_ON = C1541_SECTION.isDriveOn();
	boolean DEFAULT_PARALLEL_CABLE = C1541_SECTION.isParallelCable();
	boolean DEFAULT_JIFFYDOS_INSTALLED = C1541_SECTION.isJiffyDosInstalled();
	boolean DEFAULT_RAM_EXPAND_0X2000 = C1541_SECTION.isRamExpansionEnabled0();
	boolean DEFAULT_RAM_EXPAND_0X4000 = C1541_SECTION.isRamExpansionEnabled1();
	boolean DEFAULT_RAM_EXPAND_0X6000 = C1541_SECTION.isRamExpansionEnabled2();
	boolean DEFAULT_RAM_EXPAND_0X8000 = C1541_SECTION.isRamExpansionEnabled3();
	boolean DEFAULT_RAM_EXPAND_0XA000 = C1541_SECTION.isRamExpansionEnabled4();
	FloppyType DEFAULT_FLOPPY_TYPE = C1541_SECTION.getFloppyType();

	// Printer Section

	IPrinterSection PRINTER_SECTION = DEFAULTS.getPrinterSection();
	boolean DEFAULT_PRINTER_ON = PRINTER_SECTION.isPrinterOn();

	// Console Section

	IniConsoleSection CONSOLE_SECTION = DEFAULTS.getConsoleSection();
	char DEFAULT_CHAR_TOP_LEFT = CONSOLE_SECTION.getTopLeft();
	char DEFAULT_CHAR_TOP_RIGHT = CONSOLE_SECTION.getTopRight();
	char DEFAULT_CHAR_BOTTOM_LEFT = CONSOLE_SECTION.getBottomLeft();
	char DEFAULT_CHAR_BOTTOM_RIGHT = CONSOLE_SECTION.getBottomRight();
	char DEFAULT_CHAR_VERTICAL = CONSOLE_SECTION.getVertical();
	char DEFAULT_CHAR_HORIZONTAL = CONSOLE_SECTION.getHorizontal();
	char DEFAULT_CHAR_JUNCTION_LEFT = CONSOLE_SECTION.getJunctionLeft();
	char DEFAULT_CHAR_JUNCTION_RIGHT = CONSOLE_SECTION.getJunctionRight();

	// Audio Section

	IAudioSection AUDIO_SECTION = DEFAULTS.getAudioSection();
	Audio DEFAULT_AUDIO = AUDIO_SECTION.getAudio();
	int DEFAULT_DEVICE = AUDIO_SECTION.getDevice();
	SamplingRate DEFAULT_SAMPLING_RATE = AUDIO_SECTION.getSamplingRate();
	SamplingMethod DEFAULT_SAMPLING = AUDIO_SECTION.getSampling();
	float DEFAULT_MAIN_VOLUME = AUDIO_SECTION.getMainVolume();
	float DEFAULT_SECOND_VOLUME = AUDIO_SECTION.getSecondVolume();
	float DEFAULT_THIRD_VOLUME = AUDIO_SECTION.getThirdVolume();
	float DEFAULT_MAIN_BALANCE = AUDIO_SECTION.getMainBalance();
	float DEFAULT_SECOND_BALANCE = AUDIO_SECTION.getSecondBalance();
	float DEFAULT_THIRD_BALANCE = AUDIO_SECTION.getThirdBalance();
	int DEFAULT_MAIN_DELAY = AUDIO_SECTION.getMainDelay();
	int DEFAULT_SECOND_DELAY = AUDIO_SECTION.getSecondDelay();
	int DEFAULT_THIRD_DELAY = AUDIO_SECTION.getThirdDelay();
	int DEFAULT_BUFFER_SIZE = AUDIO_SECTION.getBufferSize();
	int DEFAULT_AUDIO_BUFFER_SIZE = AUDIO_SECTION.getAudioBufferSize();
	int DEFAULT_CBR = AUDIO_SECTION.getCbr();
	boolean DEFAULT_VBR = AUDIO_SECTION.isVbr();
	int DEFAULT_VBR_QUALITY = AUDIO_SECTION.getVbrQuality();
	boolean DEFAULT_PLAY_ORIGINAL = AUDIO_SECTION.isPlayOriginal();
	File DEFAULT_MP3_FILE = AUDIO_SECTION.getMp3();
	int DEFAULT_AUDIO_CODER_BIT_RATE = AUDIO_SECTION.getAudioCoderBitRate();
	int DEFAULT_AUDIO_CODER_BIT_RATE_TOLERANCE = AUDIO_SECTION.getAudioCoderBitRateTolerance();
	String DEFAULT_VIDEO_STREAMING_URL = AUDIO_SECTION.getVideoStreamingUrl();
	int DEFAULT_VIDEO_CODER_GOP = AUDIO_SECTION.getVideoCoderNumPicturesInGroupOfPictures();
	int DEFAULT_VIDEO_CODER_BIT_RATE = AUDIO_SECTION.getVideoCoderBitRate();
	int DEFAULT_VIDEO_CODER_BIT_RATE_TOLERANCE = AUDIO_SECTION.getVideoCoderBitRateTolerance();
	int DEFAULT_VIDEO_CODER_GLOBAL_QUALITY = AUDIO_SECTION.getVideoCoderGlobalQuality();
	VideoCoderPreset DEFAULT_VIDEO_CODER_PRESET = AUDIO_SECTION.getVideoCoderPreset();
	int DEFAULT_VIDEO_CODER_AUDIO_DELAY = AUDIO_SECTION.getVideoCoderAudioDelay();
	boolean DEFAULT_DELAY_BYPASS = AUDIO_SECTION.getDelayBypass();
	int DEFAULT_DELAY = AUDIO_SECTION.getDelay();
	int DEFAULT_DELAY_WET_LEVEL = AUDIO_SECTION.getDelayWetLevel();
	int DEFAULT_DELAY_DRY_LEVEL = AUDIO_SECTION.getDelayDryLevel();
	int DEFAULT_DELAY_FEEDBACK_LEVEL = AUDIO_SECTION.getDelayFeedbackLevel();
	boolean DEFAULT_REVERB_BYPASS = AUDIO_SECTION.getReverbBypass();
	float DEFAULT_REVERB_COMB1_DELAY = AUDIO_SECTION.getReverbComb1Delay();
	float DEFAULT_REVERB_COMB2_DELAY = AUDIO_SECTION.getReverbComb2Delay();
	float DEFAULT_REVERB_COMB3_DELAY = AUDIO_SECTION.getReverbComb3Delay();
	float DEFAULT_REVERB_COMB4_DELAY = AUDIO_SECTION.getReverbComb4Delay();
	float DEFAULT_REVERB_ALL_PASS1_DELAY = AUDIO_SECTION.getReverbAllPass1Delay();
	float DEFAULT_REVERB_ALL_PASS2_DELAY = AUDIO_SECTION.getReverbAllPass2Delay();
	float DEFAULT_REVERB_SUSTAIN_DELAY = AUDIO_SECTION.getReverbSustainDelay();
	float DEFAULT_REVERB_DRY_WET_MIX = AUDIO_SECTION.getReverbDryWetMix();

	// Emulator Section

	IEmulationSection EMULATOR_SECTION = DEFAULTS.getEmulationSection();
	Engine DEFAULT_ENGINE = EMULATOR_SECTION.getEngine();
	Emulation DEFAULT_EMULATION = EMULATOR_SECTION.getDefaultEmulation();
	Emulation DEFAULT_USER_EMULATION = EMULATOR_SECTION.getUserEmulation();
	Emulation DEFAULT_STEREO_EMULATION = EMULATOR_SECTION.getStereoEmulation();
	Emulation DEFAULT_3SID_EMULATION = EMULATOR_SECTION.getThirdEmulation();
	CPUClock DEFAULT_CLOCK_SPEED = EMULATOR_SECTION.getDefaultClockSpeed();
	CPUClock DEFAULT_USER_CLOCK_SPEED = EMULATOR_SECTION.getUserClockSpeed();
	ChipModel DEFAULT_SID_MODEL = EMULATOR_SECTION.getDefaultSidModel();
	ChipModel DEFAULT_USER_MODEL = EMULATOR_SECTION.getUserSidModel();
	ChipModel DEFAULT_STEREO_MODEL = EMULATOR_SECTION.getStereoSidModel();
	ChipModel DEFAULT_3SID_MODEL = EMULATOR_SECTION.getThirdSIDModel();
	int DEFAULT_HARD_SID_6581 = EMULATOR_SECTION.getHardsid6581();
	int DEFAULT_HARD_SID_8580 = EMULATOR_SECTION.getHardsid8580();
	List<? extends IDeviceMapping> DEFAULT_SIDBLASTER_DEVICE_LIST = EMULATOR_SECTION.getSidBlasterDeviceList();
	int DEFAULT_SIDBLASTER_WRITE_BUFFER_SIZE = EMULATOR_SECTION.getSidBlasterWriteBufferSize();
	String DEFAULT_SIDBLASTER_SERIAL_NUMBER = EMULATOR_SECTION.getSidBlasterSerialNumber();
	boolean DEFAULT_SIDBLASTER_READ = EMULATOR_SECTION.isSidBlasterRead();
	short DEFAULT_SIDBLASTER_LATENCY_TIMER = EMULATOR_SECTION.getSidBlasterLatencyTimer();
	boolean DEFAULT_EXSID_FAKE_STEREO = EMULATOR_SECTION.isExsidFakeStereo();
	String DEFAULT_NETSIDDEV_HOST = EMULATOR_SECTION.getNetSIDDevHost();
	int DEFAULT_NETSIDDEV_PORT = EMULATOR_SECTION.getNetSIDDevPort();
	Ultimate64Mode DEFAULT_ULTIMATE64_MODE = EMULATOR_SECTION.getUltimate64Mode();
	String DEFAULT_ULTIMATE64_HOST = EMULATOR_SECTION.getUltimate64Host();
	int DEFAULT_ULTIMATE64_PORT = EMULATOR_SECTION.getUltimate64Port();
	int DEFAULT_ULTIMATE64_SYNC_DELAY = EMULATOR_SECTION.getUltimate64SyncDelay();
	boolean DEFAULT_USE_FILTER = EMULATOR_SECTION.isFilter();
	boolean DEFAULT_USE_STEREO_FILTER = EMULATOR_SECTION.isStereoFilter();
	boolean DEFAULT_USE_3SID_FILTER = EMULATOR_SECTION.isThirdSIDFilter();
	SidReads DEFAULT_SID_TO_READ = EMULATOR_SECTION.getSidToRead();
	boolean DEFAULT_DIGI_BOOSTED_8580 = EMULATOR_SECTION.isDigiBoosted8580();
	int DEFAULT_DUAL_SID_BASE = EMULATOR_SECTION.getDualSidBase();
	int DEFAULT_THIRD_SID_BASE = EMULATOR_SECTION.getThirdSIDBase();
	boolean DEFAULT_FAKE_STEREO = EMULATOR_SECTION.isFakeStereo();
	boolean DEFAULT_FORCE_STEREO_TUNE = EMULATOR_SECTION.isForceStereoTune();
	boolean DEFAULT_FORCE_3SID_TUNE = EMULATOR_SECTION.isForce3SIDTune();
	boolean DEFAULT_MUTE_VOICE1 = EMULATOR_SECTION.isMuteVoice1();
	boolean DEFAULT_MUTE_VOICE2 = EMULATOR_SECTION.isMuteVoice2();
	boolean DEFAULT_MUTE_VOICE3 = EMULATOR_SECTION.isMuteVoice3();
	boolean DEFAULT_MUTE_VOICE4 = EMULATOR_SECTION.isMuteVoice4();
	boolean DEFAULT_MUTE_STEREO_VOICE1 = EMULATOR_SECTION.isMuteStereoVoice1();
	boolean DEFAULT_MUTE_STEREO_VOICE2 = EMULATOR_SECTION.isMuteStereoVoice2();
	boolean DEFAULT_MUTE_STEREO_VOICE3 = EMULATOR_SECTION.isMuteStereoVoice3();
	boolean DEFAULT_MUTE_STEREO_VOICE4 = EMULATOR_SECTION.isMuteStereoVoice4();
	boolean DEFAULT_MUTE_THIRDSID_VOICE1 = EMULATOR_SECTION.isMuteThirdSIDVoice1();
	boolean DEFAULT_MUTE_THIRDSID_VOICE2 = EMULATOR_SECTION.isMuteThirdSIDVoice2();
	boolean DEFAULT_MUTE_THIRDSID_VOICE3 = EMULATOR_SECTION.isMuteThirdSIDVoice3();
	boolean DEFAULT_MUTE_THIRDSID_VOICE4 = EMULATOR_SECTION.isMuteThirdSIDVoice4();
	String DEFAULT_NETSID_FILTER_6581 = EMULATOR_SECTION.getNetSIDFilter6581();
	String DEFAULT_NETSID_STEREO_FILTER_6581 = EMULATOR_SECTION.getNetSIDStereoFilter6581();
	String DEFAULT_NETSID_3SID_FILTER_6581 = EMULATOR_SECTION.getNetSIDThirdSIDFilter6581();
	String DEFAULT_NETSID_FILTER_8580 = EMULATOR_SECTION.getNetSIDFilter8580();
	String DEFAULT_NETSID_STEREO_FILTER_8580 = EMULATOR_SECTION.getNetSIDStereoFilter8580();
	String DEFAULT_NETSID_3SID_FILTER_8580 = EMULATOR_SECTION.getNetSIDThirdSIDFilter8580();
	String DEFAULT_FILTER_6581 = EMULATOR_SECTION.getFilter6581();
	String DEFAULT_STEREO_FILTER_6581 = EMULATOR_SECTION.getStereoFilter6581();
	String DEFAULT_3SID_FILTER_6581 = EMULATOR_SECTION.getThirdSIDFilter6581();
	String DEFAULT_FILTER_8580 = EMULATOR_SECTION.getFilter8580();
	String DEFAULT_STEREO_FILTER_8580 = EMULATOR_SECTION.getStereoFilter8580();
	String DEFAULT_3SID_FILTER_8580 = EMULATOR_SECTION.getThirdSIDFilter8580();
	String DEFAULT_ReSIDfp_FILTER_6581 = EMULATOR_SECTION.getReSIDfpFilter6581();
	String DEFAULT_ReSIDfp_STEREO_FILTER_6581 = EMULATOR_SECTION.getReSIDfpStereoFilter6581();
	String DEFAULT_ReSIDfp_3SID_FILTER_6581 = EMULATOR_SECTION.getReSIDfpThirdSIDFilter6581();
	String DEFAULT_ReSIDfp_FILTER_8580 = EMULATOR_SECTION.getReSIDfpFilter8580();
	String DEFAULT_ReSIDfp_STEREO_FILTER_8580 = EMULATOR_SECTION.getReSIDfpStereoFilter8580();
	String DEFAULT_ReSIDfp_3SID_FILTER_8580 = EMULATOR_SECTION.getReSIDfpThirdSIDFilter8580();
	boolean DEFAULT_DETECT_PSID64_CHIP_MODEL = EMULATOR_SECTION.isDetectPSID64ChipModel();

	// WhatsSID Section

	IWhatsSidSection WHATSSID_SECTION = DEFAULTS.getWhatsSidSection();
	boolean DEFAULT_WHATSSID_ENABLE = WHATSSID_SECTION.isEnable();
	String DEFAULT_WHATSSID_URL = WHATSSID_SECTION.getUrl();
	String DEFAULT_WHATSSID_USERNAME = WHATSSID_SECTION.getUsername();
	String DEFAULT_WHATSSID_PASSWORD = WHATSSID_SECTION.getPassword();
	int DEFAULT_WHATSSID_CONNECTION_TIMEOUT = WHATSSID_SECTION.getConnectionTimeout();
	int DEFAULT_WHATSSID_CAPTURE_TIME = WHATSSID_SECTION.getCaptureTime();
	int DEFAULT_WHATSSID_MATCH_START_TIME = WHATSSID_SECTION.getMatchStartTime();
	int DEFAULT_WHATSSID_MATCH_RETRY_TIME = WHATSSID_SECTION.getMatchRetryTime();
	float DEFAULT_WHATSSID_MINIMUM_RELATIVE_CONFIDENCE = WHATSSID_SECTION.getMinimumRelativeConfidence();
	boolean DEFAULT_WHATSSID_DETECT_CHIP_MODEL = WHATSSID_SECTION.isDetectChipModel();

}
