package client.teavm;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.teavm.interop.Import;

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
import libsidplay.config.IConfig;
import libsidplay.config.IDeviceMapping;
import libsidplay.config.IEmulationSection;
import libsidplay.config.IFilterSection;
import libsidplay.config.IPrinterSection;
import libsidplay.config.ISidPlay2Section;
import libsidplay.config.IWhatsSidSection;
import sidplay.audio.Audio;

public class JavaScriptConfig implements IConfig {

	private static final String AUDIO_SECTION = "audiosection";
	private static final String EMULATION_SECTION = "emulationsection";

	@Override
	public ISidPlay2Section getSidplay2Section() {
		return new ISidPlay2Section() {

			@Override
			public void setVersion(int version) {
			}

			@Override
			public void setTurboTape(boolean turboTape) {
			}

			@Override
			public void setTmpDir(File tmpDir) {
			}

			@Override
			public void setTint(float tint) {
			}

			@Override
			public void setStartTime(double startTime) {
			}

			@Override
			public void setSingle(boolean singleSong) {
			}

			@Override
			public void setSaturation(float saturation) {
			}

			@Override
			public void setPhaseShift(float phaseShift) {
			}

			@Override
			public void setPalEmulation(boolean palEmulation) {
			}

			@Override
			public void setOffset(float offset) {
			}

			@Override
			public void setLoop(boolean loop) {
			}

			@Override
			public void setLastDirectory(File lastDir) {
			}

			@Override
			public void setHvsc(File hvsc) {
			}

			@Override
			public void setGamma(float gamma) {
			}

			@Override
			public void setFadeOutTime(double fadeOutTime) {
			}

			@Override
			public void setFadeInTime(double fadeInTime) {
			}

			@Override
			public void setEnableDatabase(boolean enable) {
			}

			@Override
			public void setDefaultPlayLength(double playLength) {
			}

			@Override
			public void setContrast(float contrast) {
			}

			@Override
			public void setBrightness(float brightness) {
			}

			@Override
			public void setBlur(float blur) {
			}

			@Override
			public void setBleed(float bleed) {
			}

			@Override
			public boolean isTurboTape() {
				return false;
			}

			@Override
			public boolean isSingle() {
				return false;
			}

			@Override
			public boolean isPalEmulation() {
				return false;
			}

			@Override
			public boolean isLoop() {
				return false;
			}

			@Override
			public boolean isEnableDatabase() {
				return false;
			}

			@Override
			public int getVersion() {
				return REQUIRED_CONFIG_VERSION;
			}

			@Override
			public File getTmpDir() {
				return null;
			}

			@Override
			public float getTint() {
				return 0;
			}

			@Override
			public double getStartTime() {
				return 0;
			}

			@Override
			public float getSaturation() {
				return 0;
			}

			@Override
			public float getPhaseShift() {
				return -15;
			}

			@Override
			public float getOffset() {
				return 1;
			}

			@Override
			public File getLastDirectory() {
				return null;
			}

			@Override
			public File getHvsc() {
				return null;
			}

			@Override
			public float getGamma() {
				return 2;
			}

			@Override
			public double getFadeOutTime() {
				return 0;
			}

			@Override
			public double getFadeInTime() {
				return 0;
			}

			@Override
			public double getDefaultPlayLength() {
				return 300;
			}

			@Override
			public float getContrast() {
				return 1;
			}

			@Override
			public float getBrightness() {
				return 0;
			}

			@Override
			public float getBlur() {
				return 0.5f;
			}

			@Override
			public float getBleed() {
				return 0.5f;
			}
		};
	}

	@Override
	public IC1541Section getC1541Section() {
		return new IC1541Section() {

			@Override
			public void setRamExpansionEnabled4(boolean on) {
			}

			@Override
			public void setRamExpansionEnabled3(boolean on) {
			}

			@Override
			public void setRamExpansionEnabled2(boolean on) {
			}

			@Override
			public void setRamExpansionEnabled1(boolean on) {
			}

			@Override
			public void setRamExpansionEnabled0(boolean on) {
			}

			@Override
			public void setParallelCable(boolean on) {
			}

			@Override
			public void setJiffyDosInstalled(boolean on) {
			}

			@Override
			public void setFloppyType(FloppyType floppyType) {
			}

			@Override
			public void setDriveOn(boolean on) {
			}

			@Override
			public boolean isRamExpansionEnabled4() {
				return false;
			}

			@Override
			public boolean isRamExpansionEnabled3() {
				return false;
			}

			@Override
			public boolean isRamExpansionEnabled2() {
				return false;
			}

			@Override
			public boolean isRamExpansionEnabled1() {
				return false;
			}

			@Override
			public boolean isRamExpansionEnabled0() {
				return false;
			}

			@Override
			public boolean isParallelCable() {
				return false;
			}

			@Override
			public boolean isJiffyDosInstalled() {
				return false;
			}

			@Override
			public boolean isDriveOn() {
				return false;
			}

			@Override
			public FloppyType getFloppyType() {
				return FloppyType.C1541_II;
			}
		};
	}

	@Override
	public IPrinterSection getPrinterSection() {
		return new IPrinterSection() {

			@Override
			public void setPrinterOn(boolean on) {
			}

			@Override
			public boolean isPrinterOn() {
				return false;
			}
		};
	}

	@Override
	public IAudioSection getAudioSection() {
		return new IAudioSection() {

			@Override
			public void setVideoStreamingUrl(String videoStreamingUrl) {
			}

			@Override
			public void setVideoCoderPreset(VideoCoderPreset preset) {
			}

			@Override
			public void setVideoCoderNumPicturesInGroupOfPictures(int numPicturesInGroupOfPictures) {
			}

			@Override
			public void setVideoCoderGlobalQuality(int bitGlobalQuality) {
			}

			@Override
			public void setVideoCoderBitRateTolerance(int bitRateTolerance) {
			}

			@Override
			public void setVideoCoderBitRate(int bitRate) {
			}

			@Override
			public void setVideoCoderAudioDelay(int audioDelay) {
			}

			@Override
			public void setVbrQuality(int vbr) {
			}

			@Override
			public void setVbr(boolean vbr) {
			}

			@Override
			public void setThirdVolume(float volume) {
			}

			@Override
			public void setThirdDelay(int delay) {
			}

			@Override
			public void setThirdBalance(float balance) {
			}

			@Override
			public void setSecondVolume(float volume) {
			}

			@Override
			public void setSecondDelay(int delay) {
			}

			@Override
			public void setSecondBalance(float balance) {
			}

			@Override
			public void setSamplingRate(SamplingRate samplingRate) {
			}

			@Override
			public void setSampling(SamplingMethod method) {
			}

			@Override
			public void setReverbSustainDelay(float reverbSustainDelay) {
			}

			@Override
			public void setReverbDryWetMix(float reverbDryWetMix) {
			}

			@Override
			public void setReverbComb4Delay(float reverbComb4Delay) {
			}

			@Override
			public void setReverbComb3Delay(float reverbComb3Delay) {
			}

			@Override
			public void setReverbComb2Delay(float reverbComb2Delay) {
			}

			@Override
			public void setReverbComb1Delay(float reverbComb1Delay) {
			}

			@Override
			public void setReverbBypass(boolean reverbBypass) {
			}

			@Override
			public void setReverbAllPass2Delay(float reverbAllPass2Delay) {
			}

			@Override
			public void setReverbAllPass1Delay(float reverbAllPass1Delay) {
			}

			@Override
			public void setPlayOriginal(boolean original) {
			}

			@Override
			public void setMp3(File recording) {
			}

			@Override
			public void setMainVolume(float volume) {
			}

			@Override
			public void setMainDelay(int delay) {
			}

			@Override
			public void setMainBalance(float balance) {
			}

			@Override
			public void setDevice(int device) {
			}

			@Override
			public void setDelayWetLevel(int delayWetLevel) {
			}

			@Override
			public void setDelayFeedbackLevel(int delayFeedbackLevel) {
			}

			@Override
			public void setDelayDryLevel(int delayDryLevel) {
			}

			@Override
			public void setDelayBypass(boolean delayBypass) {
			}

			@Override
			public void setDelay(int delay) {
			}

			@Override
			public void setCbr(int cbr) {
			}

			@Override
			public void setBufferSize(int bufferSize) {
			}

			@Override
			public void setAudioCoderBitRateTolerance(int bitRateTolerance) {
			}

			@Override
			public void setAudioCoderBitRate(int bitRate) {
			}

			@Override
			public void setAudioBufferSize(int audioBufferSize) {
			}

			@Override
			public void setAudio(Audio audio) {
			}

			@Override
			public boolean isVbr() {
				return false;
			}

			@Override
			public boolean isPlayOriginal() {
				return false;
			}

			@Override
			public String getVideoStreamingUrl() {
				return null;
			}

			@Override
			public VideoCoderPreset getVideoCoderPreset() {
				return null;
			}

			@Override
			public int getVideoCoderNumPicturesInGroupOfPictures() {
				return 0;
			}

			@Override
			public int getVideoCoderGlobalQuality() {
				return 0;
			}

			@Override
			public int getVideoCoderBitRateTolerance() {
				return 0;
			}

			@Override
			public int getVideoCoderBitRate() {
				return 0;
			}

			@Override
			public int getVideoCoderAudioDelay() {
				return 0;
			}

			@Override
			public int getVbrQuality() {
				return 0;
			}

			@Override
			public float getThirdVolume() {
				return 0;
			}

			@Override
			public int getThirdDelay() {
				return 0;
			}

			@Override
			public float getThirdBalance() {
				return 0.5f;
			}

			@Override
			public float getSecondVolume() {
				return 0;
			}

			@Override
			public int getSecondDelay() {
				return 0;
			}

			@Override
			public float getSecondBalance() {
				return 0.5f;
			}

			@Override
			public SamplingRate getSamplingRate() {
				switch (getSamplingRateAsInt()) {
				case 8000:
					return SamplingRate.VERY_LOW;
				case 44100:
				default:
					return SamplingRate.LOW;
				case 48000:
					return SamplingRate.MEDIUM;
				case 96000:
					return SamplingRate.HIGH;
				}
			}

			@Import(module = AUDIO_SECTION, name = "getSamplingRate")
			public native int getSamplingRateAsInt();

			@Override
			public SamplingMethod getSampling() {
				if (getSamplingMethodResample()) {
					return SamplingMethod.RESAMPLE;
				} else {
					return SamplingMethod.DECIMATE;
				}
			}

			@Import(module = AUDIO_SECTION, name = "getSamplingMethodResample")
			public native boolean getSamplingMethodResample();

			@Override
			public float getReverbSustainDelay() {
				return 500;
			}

			@Override
			public float getReverbDryWetMix() {
				return 0.25f;
			}

			@Override
			public float getReverbComb4Delay() {
				return 43.7f;
			}

			@Override
			public float getReverbComb3Delay() {
				return 41.1f;
			}

			@Override
			public float getReverbComb2Delay() {
				return 37.1f;
			}

			@Override
			public float getReverbComb1Delay() {
				return 29.7f;
			}

			@Override
			@Import(module = AUDIO_SECTION, name = "getReverbBypass")
			public native boolean getReverbBypass();

			@Override
			public float getReverbAllPass2Delay() {
				return 1.7f;
			}

			@Override
			public float getReverbAllPass1Delay() {
				return 5;
			}

			@Override
			public File getMp3() {
				return null;
			}

			@Override
			public float getMainVolume() {
				return 0;
			}

			@Override
			public int getMainDelay() {
				return 0;
			}

			@Override
			public float getMainBalance() {
				return 0.5f;
			}

			@Override
			public int getDevice() {
				return 0;
			}

			@Override
			public int getDelayWetLevel() {
				return 70;
			}

			@Override
			public int getDelayFeedbackLevel() {
				return 10;
			}

			@Override
			public int getDelayDryLevel() {
				return 70;
			}

			@Override
			public boolean getDelayBypass() {
				return true;
			}

			@Override
			public int getDelay() {
				return 10;
			}

			@Override
			public int getCbr() {
				return -1;
			}

			@Override
			@Import(module = AUDIO_SECTION, name = "getBufferSize")
			public native int getBufferSize();

			@Override
			public int getAudioCoderBitRateTolerance() {
				return 64000;
			}

			@Override
			public int getAudioCoderBitRate() {
				return 128000;
			}

			@Override
			@Import(module = AUDIO_SECTION, name = "getAudioBufferSize")
			public native int getAudioBufferSize();

			@Override
			public Audio getAudio() {
				return Audio.SOUNDCARD;
			}
		};
	}

	@Override
	public IEmulationSection getEmulationSection() {
		return new IEmulationSection() {

			@Override
			public void setUserSidModel(ChipModel model) {
			}

			@Override
			public void setUserEmulation(Emulation emulation) {
			}

			@Override
			public void setUserClockSpeed(CPUClock speed) {
			}

			@Override
			public void setUltimate64SyncDelay(int syncDelay) {
			}

			@Override
			public void setUltimate64Port(int port) {
			}

			@Override
			public void setUltimate64Mode(Ultimate64Mode ultimate64Mode) {
			}

			@Override
			public void setUltimate64Host(String hostname) {
			}

			@Override
			public void setThirdSIDModel(ChipModel model) {
			}

			@Override
			public void setThirdSIDFilter8580(String filterName) {
			}

			@Override
			public void setThirdSIDFilter6581(String filterName) {
			}

			@Override
			public void setThirdSIDFilter(boolean enable) {
			}

			@Override
			public void setThirdSIDBase(int base) {
			}

			@Override
			public void setThirdEmulation(Emulation emulation) {
			}

			@Override
			public void setStereoSidModel(ChipModel model) {
			}

			@Override
			public void setStereoFilter8580(String filterName) {
			}

			@Override
			public void setStereoFilter6581(String filterName) {
			}

			@Override
			public void setStereoFilter(boolean enable) {
			}

			@Override
			public void setStereoEmulation(Emulation emulation) {
			}

			@Override
			public void setSidToRead(SidReads sidRead) {
			}

			@Override
			public void setSidBlasterWriteBufferSize(int sidBlasterWriteBufferSize) {
			}

			@Override
			public void setSidBlasterSerialNumber(String sidBlasterSerialNumber) {
			}

			@Override
			public void setSidBlasterRead(boolean sidBlasterRead) {
			}

			@Override
			public void setSidBlasterLatencyTimer(short sidBlasterLatencyTimer) {
			}

			@Override
			public void setReSIDfpThirdSIDFilter8580(String filterName) {
			}

			@Override
			public void setReSIDfpThirdSIDFilter6581(String filterName) {
			}

			@Override
			public void setReSIDfpStereoFilter8580(String filterName) {
			}

			@Override
			public void setReSIDfpStereoFilter6581(String filterName) {
			}

			@Override
			public void setReSIDfpFilter8580(String filterName) {
			}

			@Override
			public void setReSIDfpFilter6581(String filterName) {
			}

			@Override
			public void setNetSIDThirdSIDFilter8580(String filterName) {
			}

			@Override
			public void setNetSIDThirdSIDFilter6581(String filterName) {
			}

			@Override
			public void setNetSIDStereoFilter8580(String filterName) {
			}

			@Override
			public void setNetSIDStereoFilter6581(String filterName) {
			}

			@Override
			public void setNetSIDFilter8580(String filterName) {
			}

			@Override
			public void setNetSIDFilter6581(String filterName) {
			}

			@Override
			public void setNetSIDDevPort(int port) {
			}

			@Override
			public void setNetSIDDevHost(String hostname) {
			}

			@Override
			public void setMuteVoice4(boolean mute) {
			}

			@Override
			public void setMuteVoice3(boolean mute) {
			}

			@Override
			public void setMuteVoice2(boolean mute) {
			}

			@Override
			public void setMuteVoice1(boolean mute) {
			}

			@Override
			public void setMuteThirdSIDVoice4(boolean mute) {
			}

			@Override
			public void setMuteThirdSIDVoice3(boolean mute) {
			}

			@Override
			public void setMuteThirdSIDVoice2(boolean mute) {
			}

			@Override
			public void setMuteThirdSIDVoice1(boolean mute) {
			}

			@Override
			public void setMuteStereoVoice4(boolean mute) {
			}

			@Override
			public void setMuteStereoVoice3(boolean mute) {
			}

			@Override
			public void setMuteStereoVoice2(boolean mute) {
			}

			@Override
			public void setMuteStereoVoice1(boolean mute) {
			}

			@Override
			public void setHardsid8580(int chip) {
			}

			@Override
			public void setHardsid6581(int chip) {
			}

			@Override
			public void setForceStereoTune(boolean force) {
			}

			@Override
			public void setForce3SIDTune(boolean force) {
			}

			@Override
			public void setFilter8580(String filterName) {
			}

			@Override
			public void setFilter6581(String filterName) {
			}

			@Override
			public void setFilter(boolean enable) {
			}

			@Override
			public void setFakeStereo(boolean fakeStereo) {
			}

			@Override
			public void setExsidFakeStereo(boolean exsidFakeStereo) {
			}

			@Override
			public void setEngine(Engine engine) {
			}

			@Override
			public void setDualSidBase(int base) {
			}

			@Override
			public void setDigiBoosted8580(boolean boost) {
			}

			@Override
			public void setDetectPSID64ChipModel(boolean detectPSID64ChipModel) {
			}

			@Override
			public void setDefaultSidModel(ChipModel model) {
			}

			@Override
			public void setDefaultEmulation(Emulation emulation) {
			}

			@Override
			public void setDefaultClockSpeed(CPUClock speed) {
			}

			@Override
			public boolean isThirdSIDFilter() {
				return true;
			}

			@Override
			public boolean isStereoFilter() {
				return true;
			}

			@Override
			public boolean isSidBlasterRead() {
				return false;
			}

			@Override
			public boolean isMuteVoice4() {
				return false;
			}

			@Override
			public boolean isMuteVoice3() {
				return false;
			}

			@Override
			public boolean isMuteVoice2() {
				return false;
			}

			@Override
			public boolean isMuteVoice1() {
				return false;
			}

			@Override
			public boolean isMuteThirdSIDVoice4() {
				return false;
			}

			@Override
			public boolean isMuteThirdSIDVoice3() {
				return false;
			}

			@Override
			public boolean isMuteThirdSIDVoice2() {
				return false;
			}

			@Override
			public boolean isMuteThirdSIDVoice1() {
				return false;
			}

			@Override
			public boolean isMuteStereoVoice4() {
				return false;
			}

			@Override
			public boolean isMuteStereoVoice3() {
				return false;
			}

			@Override
			public boolean isMuteStereoVoice2() {
				return false;
			}

			@Override
			public boolean isMuteStereoVoice1() {
				return false;
			}

			@Override
			public boolean isForceStereoTune() {
				return false;
			}

			@Override
			public boolean isForce3SIDTune() {
				return false;
			}

			@Override
			public boolean isFilter() {
				return true;
			}

			@Override
			public boolean isFakeStereo() {
				return false;
			}

			@Override
			public boolean isExsidFakeStereo() {
				return false;
			}

			@Override
			public boolean isDigiBoosted8580() {
				return false;
			}

			@Override
			public boolean isDetectPSID64ChipModel() {
				return false;
			}

			@Override
			public ChipModel getUserSidModel() {
				return ChipModel.AUTO;
			}

			@Override
			public Emulation getUserEmulation() {
				return Emulation.DEFAULT;
			}

			@Override
			public CPUClock getUserClockSpeed() {
				return CPUClock.AUTO;
			}

			@Override
			public int getUltimate64SyncDelay() {
				return 0;
			}

			@Override
			public int getUltimate64Port() {
				return 0;
			}

			@Override
			public Ultimate64Mode getUltimate64Mode() {
				return null;
			}

			@Override
			public String getUltimate64Host() {
				return null;
			}

			@Override
			public ChipModel getThirdSIDModel() {
				return ChipModel.AUTO;
			}

			@Override
			public String getThirdSIDFilter8580() {
				return "FilterAverage8580";
			}

			@Override
			public String getThirdSIDFilter6581() {
				return "FilterAverage6581";
			}

			@Override
			public int getThirdSIDBase() {
				return 0xd440;
			}

			@Override
			public Emulation getThirdEmulation() {
				return Emulation.DEFAULT;
			}

			@Override
			public ChipModel getStereoSidModel() {
				return ChipModel.AUTO;
			}

			@Override
			public String getStereoFilter8580() {
				return "FilterAverage8580";
			}

			@Override
			public String getStereoFilter6581() {
				return "FilterAverage6581";
			}

			@Override
			public Emulation getStereoEmulation() {
				return Emulation.DEFAULT;
			}

			@Override
			public SidReads getSidToRead() {
				return SidReads.FIRST_SID;
			}

			@Override
			public int getSidBlasterWriteBufferSize() {
				return 0;
			}

			@Override
			public String getSidBlasterSerialNumber() {
				return null;
			}

			@Override
			public short getSidBlasterLatencyTimer() {
				return 0;
			}

			@Override
			public List<? extends IDeviceMapping> getSidBlasterDeviceList() {
				return Collections.emptyList();
			}

			@Override
			public String getReSIDfpThirdSIDFilter8580() {
				return "FilterTrurl8580R5_3691";
			}

			@Override
			public String getReSIDfpThirdSIDFilter6581() {
				return "FilterAlankila6581R4AR_3789";
			}

			@Override
			public String getReSIDfpStereoFilter8580() {
				return "FilterTrurl8580R5_3691";
			}

			@Override
			public String getReSIDfpStereoFilter6581() {
				return "FilterAlankila6581R4AR_3789";
			}

			@Override
			public String getReSIDfpFilter8580() {
				return "FilterTrurl8580R5_3691";
			}

			@Override
			public String getReSIDfpFilter6581() {
				return "FilterAlankila6581R4AR_3789";
			}

			@Override
			public OverrideSection getOverrideSection() {
				return new OverrideSection();
			}

			@Override
			public String getNetSIDThirdSIDFilter8580() {
				return null;
			}

			@Override
			public String getNetSIDThirdSIDFilter6581() {
				return null;
			}

			@Override
			public String getNetSIDStereoFilter8580() {
				return null;
			}

			@Override
			public String getNetSIDStereoFilter6581() {
				return null;
			}

			@Override
			public String getNetSIDFilter8580() {
				return null;
			}

			@Override
			public String getNetSIDFilter6581() {
				return null;
			}

			@Override
			public int getNetSIDDevPort() {
				return 0;
			}

			@Override
			public String getNetSIDDevHost() {
				return null;
			}

			@Override
			public int getHardsid8580() {
				return 0;
			}

			@Override
			public int getHardsid6581() {
				return 0;
			}

			@Override
			public String getFilter8580() {
				return "FilterAverage8580";
			}

			@Override
			public String getFilter6581() {
				return "FilterAverage6581";
			}

			@Override
			public Engine getEngine() {
				return Engine.EMULATION;
			}

			@Override
			public int getDualSidBase() {
				return 0xd420;
			}

			@Override
			public ChipModel getDefaultSidModel() {
				if (getDefaultSidModel8580()) {
					return ChipModel.MOS8580;
				} else {
					return ChipModel.MOS6581;
				}
			}

			@Import(module = EMULATION_SECTION, name = "getDefaultSidModel8580")
			public native boolean getDefaultSidModel8580();

			@Override
			public Emulation getDefaultEmulation() {
				return Emulation.RESID;
			}

			@Override
			public CPUClock getDefaultClockSpeed() {
				switch (getDefaultClockSpeedAsInt()) {
				case 50:
				default:
					return CPUClock.PAL;
				case 60:
					return CPUClock.NTSC;
				}
			}

			@Import(module = EMULATION_SECTION, name = "getDefaultClockSpeed")
			public native int getDefaultClockSpeedAsInt();

		};
	}

	@Override
	public IWhatsSidSection getWhatsSidSection() {
		return new IWhatsSidSection() {

			@Override
			public void setUsername(String username) {
			}

			@Override
			public void setUrl(String url) {
			}

			@Override
			public void setPassword(String password) {
			}

			@Override
			public void setMinimumRelativeConfidence(float minimumRelativeConfidence) {
			}

			@Override
			public void setMatchStartTime(int matchStartTime) {
			}

			@Override
			public void setMatchRetryTime(int matchRetryTime) {
			}

			@Override
			public void setEnable(boolean enable) {
			}

			@Override
			public void setDetectChipModel(boolean detectChipModel) {
			}

			@Override
			public void setConnectionTimeout(int connectionTimeout) {
			}

			@Override
			public void setCaptureTime(int captureTime) {
			}

			@Override
			public boolean isEnable() {
				return false;
			}

			@Override
			public boolean isDetectChipModel() {
				return false;
			}

			@Override
			public String getUsername() {
				return null;
			}

			@Override
			public String getUrl() {
				return null;
			}

			@Override
			public String getPassword() {
				return null;
			}

			@Override
			public float getMinimumRelativeConfidence() {
				return 4.5f;
			}

			@Override
			public int getMatchStartTime() {
				return 15;
			}

			@Override
			public int getMatchRetryTime() {
				return 15;
			}

			@Override
			public int getConnectionTimeout() {
				return 5000;
			}

			@Override
			public int getCaptureTime() {
				return 15;
			}
		};
	}

	@Override
	public List<? extends IFilterSection> getFilterSection() {
		ArrayList<IFilterSection> arrayList = new ArrayList<IFilterSection>();
		IFilterSection fs1 = new IFilterSection() {

			@Override
			public void setVoiceNonlinearity(float voiceNonlinearity) {
			}

			@Override
			public void setSteepness(float steepness) {
			}

			@Override
			public void setResonanceFactor(float resonanceFactor) {
			}

			@Override
			public void setOffset(float offset) {
			}

			@Override
			public void setNonlinearity(float nonlinearity) {
			}

			@Override
			public void setName(String name) {
			}

			@Override
			public void setMinimumfetresistance(float minimumfetresistance) {
			}

			@Override
			public void setK(float k) {
			}

			@Override
			public void setFilter8580CurvePosition(float filter8580CurvePosition) {
			}

			@Override
			public void setFilter6581CurvePosition(float filter6581CurvePosition) {
			}

			@Override
			public void setBaseresistance(float baseresistance) {
			}

			@Override
			public void setB(float b) {
			}

			@Override
			public void setAttenuation(float attenuation) {
			}

			@Override
			public float getVoiceNonlinearity() {
				return 0;
			}

			@Override
			public float getSteepness() {
				return 0;
			}

			@Override
			public float getResonanceFactor() {
				return 0;
			}

			@Override
			public float getOffset() {
				return 0;
			}

			@Override
			public float getNonlinearity() {
				return 0;
			}

			@Override
			public String getName() {
				return "FilterAverage6581";
			}

			@Override
			public float getMinimumfetresistance() {
				return 0;
			}

			@Override
			public float getK() {
				return 0;
			}

			@Override
			public float getFilter8580CurvePosition() {
				return 0;
			}

			@Override
			public float getFilter6581CurvePosition() {
				return 0.5f;
			}

			@Override
			public float getBaseresistance() {
				return 0;
			}

			@Override
			public float getB() {
				return 0;
			}

			@Override
			public float getAttenuation() {
				return 0;
			}
		};
		arrayList.add(fs1);
		IFilterSection fs2 = new IFilterSection() {

			@Override
			public void setVoiceNonlinearity(float voiceNonlinearity) {
			}

			@Override
			public void setSteepness(float steepness) {
			}

			@Override
			public void setResonanceFactor(float resonanceFactor) {
			}

			@Override
			public void setOffset(float offset) {
			}

			@Override
			public void setNonlinearity(float nonlinearity) {
			}

			@Override
			public void setName(String name) {
			}

			@Override
			public void setMinimumfetresistance(float minimumfetresistance) {
			}

			@Override
			public void setK(float k) {
			}

			@Override
			public void setFilter8580CurvePosition(float filter8580CurvePosition) {
			}

			@Override
			public void setFilter6581CurvePosition(float filter6581CurvePosition) {
			}

			@Override
			public void setBaseresistance(float baseresistance) {
			}

			@Override
			public void setB(float b) {
			}

			@Override
			public void setAttenuation(float attenuation) {
			}

			@Override
			public float getVoiceNonlinearity() {
				return 0;
			}

			@Override
			public float getSteepness() {
				return 0;
			}

			@Override
			public float getResonanceFactor() {
				return 0;
			}

			@Override
			public float getOffset() {
				return 0;
			}

			@Override
			public float getNonlinearity() {
				return 0;
			}

			@Override
			public String getName() {
				return "FilterAverage8580";
			}

			@Override
			public float getMinimumfetresistance() {
				return 0;
			}

			@Override
			public float getK() {
				return 0;
			}

			@Override
			public float getFilter8580CurvePosition() {
				return 12500;
			}

			@Override
			public float getFilter6581CurvePosition() {
				return 0;
			}

			@Override
			public float getBaseresistance() {
				return 0;
			}

			@Override
			public float getB() {
				return 0;
			}

			@Override
			public float getAttenuation() {
				return 0;
			}
		};
		arrayList.add(fs2);
		IFilterSection fs3 = new IFilterSection() {

			@Override
			public void setVoiceNonlinearity(float voiceNonlinearity) {
			}

			@Override
			public void setSteepness(float steepness) {
			}

			@Override
			public void setResonanceFactor(float resonanceFactor) {
			}

			@Override
			public void setOffset(float offset) {
			}

			@Override
			public void setNonlinearity(float nonlinearity) {
			}

			@Override
			public void setName(String name) {
			}

			@Override
			public void setMinimumfetresistance(float minimumfetresistance) {
			}

			@Override
			public void setK(float k) {
			}

			@Override
			public void setFilter8580CurvePosition(float filter8580CurvePosition) {
			}

			@Override
			public void setFilter6581CurvePosition(float filter6581CurvePosition) {
			}

			@Override
			public void setBaseresistance(float baseresistance) {
			}

			@Override
			public void setB(float b) {
			}

			@Override
			public void setAttenuation(float attenuation) {
			}

			@Override
			public float getVoiceNonlinearity() {
				return 03.3e6f;
			}

			@Override
			public float getSteepness() {
				return 1.0066634233403395f;
			}

			@Override
			public float getResonanceFactor() {
				return 1.0f;
			}

			@Override
			public float getOffset() {
				return 274228796.97550374f;
			}

			@Override
			public float getNonlinearity() {
				return 0.9613160610660189f;
			}

			@Override
			public String getName() {
				return "FilterAlankila6581R4AR_3789";
			}

			@Override
			public float getMinimumfetresistance() {
				return 16125.154840564108f;
			}

			@Override
			public float getK() {
				return 0;
			}

			@Override
			public float getFilter8580CurvePosition() {
				return 0;
			}

			@Override
			public float getFilter6581CurvePosition() {
				return 0;
			}

			@Override
			public float getBaseresistance() {
				return 1147036.4394268463f;
			}

			@Override
			public float getB() {
				return 0;
			}

			@Override
			public float getAttenuation() {
				return 0.5f;
			}
		};
		arrayList.add(fs3);
		IFilterSection fs4 = new IFilterSection() {

			@Override
			public void setVoiceNonlinearity(float voiceNonlinearity) {
			}

			@Override
			public void setSteepness(float steepness) {
			}

			@Override
			public void setResonanceFactor(float resonanceFactor) {
			}

			@Override
			public void setOffset(float offset) {
			}

			@Override
			public void setNonlinearity(float nonlinearity) {
			}

			@Override
			public void setName(String name) {
			}

			@Override
			public void setMinimumfetresistance(float minimumfetresistance) {
			}

			@Override
			public void setK(float k) {
			}

			@Override
			public void setFilter8580CurvePosition(float filter8580CurvePosition) {
			}

			@Override
			public void setFilter6581CurvePosition(float filter6581CurvePosition) {
			}

			@Override
			public void setBaseresistance(float baseresistance) {
			}

			@Override
			public void setB(float b) {
			}

			@Override
			public void setAttenuation(float attenuation) {
			}

			@Override
			public float getVoiceNonlinearity() {
				return 0;
			}

			@Override
			public float getSteepness() {
				return 0;
			}

			@Override
			public float getResonanceFactor() {
				return 1.0f;
			}

			@Override
			public float getOffset() {
				return 0;
			}

			@Override
			public float getNonlinearity() {
				return 1.0f;
			}

			@Override
			public String getName() {
				return "FilterTrurl8580R5_3691";
			}

			@Override
			public float getMinimumfetresistance() {
				return 0;
			}

			@Override
			public float getK() {
				return 6.55f;
			}

			@Override
			public float getFilter8580CurvePosition() {
				return 0;
			}

			@Override
			public float getFilter6581CurvePosition() {
				return 0;
			}

			@Override
			public float getBaseresistance() {
				return 0;
			}

			@Override
			public float getB() {
				return 20;
			}

			@Override
			public float getAttenuation() {
				return 0;
			}
		};
		arrayList.add(fs4);
		return arrayList;
	}

}
