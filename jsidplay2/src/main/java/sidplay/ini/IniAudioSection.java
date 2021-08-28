package sidplay.ini;

import static sidplay.ini.IniDefaults.DEFAULT_AUDIO;
import static sidplay.ini.IniDefaults.DEFAULT_AUDIO_BUFFER_SIZE;
import static sidplay.ini.IniDefaults.DEFAULT_AUDIO_CODER_BIT_RATE;
import static sidplay.ini.IniDefaults.DEFAULT_AUDIO_CODER_BIT_RATE_TOLERANCE;
import static sidplay.ini.IniDefaults.DEFAULT_BUFFER_SIZE;
import static sidplay.ini.IniDefaults.DEFAULT_CBR;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY_BYPASS;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY_DRY_LEVEL;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY_FEEDBACK_LEVEL;
import static sidplay.ini.IniDefaults.DEFAULT_DELAY_WET_LEVEL;
import static sidplay.ini.IniDefaults.DEFAULT_DEVICE;
import static sidplay.ini.IniDefaults.DEFAULT_EXSID_FAKE_STEREO;
import static sidplay.ini.IniDefaults.DEFAULT_MAIN_BALANCE;
import static sidplay.ini.IniDefaults.DEFAULT_MAIN_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_MAIN_VOLUME;
import static sidplay.ini.IniDefaults.DEFAULT_MP3_FILE;
import static sidplay.ini.IniDefaults.DEFAULT_PLAY_ORIGINAL;
import static sidplay.ini.IniDefaults.DEFAULT_REVERB_BYPASS;
import static sidplay.ini.IniDefaults.DEFAULT_REVERB_COMB1_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_REVERB_COMB2_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_REVERB_COMB3_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_REVERB_COMB4_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_REVERB_DRY_WET_MIX;
import static sidplay.ini.IniDefaults.DEFAULT_REVERB_SUSTAIN_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_SAMPLING;
import static sidplay.ini.IniDefaults.DEFAULT_SAMPLING_RATE;
import static sidplay.ini.IniDefaults.DEFAULT_SECOND_BALANCE;
import static sidplay.ini.IniDefaults.DEFAULT_SECOND_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_SECOND_VOLUME;
import static sidplay.ini.IniDefaults.DEFAULT_THIRD_BALANCE;
import static sidplay.ini.IniDefaults.DEFAULT_THIRD_DELAY;
import static sidplay.ini.IniDefaults.DEFAULT_THIRD_VOLUME;
import static sidplay.ini.IniDefaults.DEFAULT_VBR;
import static sidplay.ini.IniDefaults.DEFAULT_VBR_QUALITY;
import static sidplay.ini.IniDefaults.DEFAULT_VIDEO_CODER_BIT_RATE;
import static sidplay.ini.IniDefaults.DEFAULT_VIDEO_CODER_BIT_RATE_TOLERANCE;
import static sidplay.ini.IniDefaults.DEFAULT_VIDEO_CODER_GLOBAL_QUALITY;
import static sidplay.ini.IniDefaults.DEFAULT_VIDEO_CODER_GOP;
import static sidplay.ini.IniDefaults.DEFAULT_VIDEO_CODER_PRESET;
import static sidplay.ini.IniDefaults.DEFAULT_VIDEO_STREAMING_URL;

import java.io.File;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import libsidplay.common.SamplingMethod;
import libsidplay.common.SamplingRate;
import libsidplay.common.VideoCoderPreset;
import libsidplay.config.IAudioSection;
import sidplay.audio.Audio;
import sidplay.ini.converter.BeanToStringConverter;

/**
 * Audio section of the INI file.
 *
 * @author Ken Händel
 *
 */
@Parameters(resourceBundle = "sidplay.ini.IniAudioSection")
public class IniAudioSection extends IniSection implements IAudioSection {

	private static final String SECTION_ID = "Audio";

	public IniAudioSection(IniReader iniReader) {
		super(iniReader);
	}

	@Override
	public Audio getAudio() {
		return iniReader.getPropertyEnum(SECTION_ID, "Audio", DEFAULT_AUDIO, Audio.class);
	}

	@Override
	@Parameter(names = { "--audio", "-a" }, descriptionKey = "DRIVER", order = 100)
	public void setAudio(Audio audio) {
		iniReader.setProperty(SECTION_ID, "Audio", audio);
	}

	@Override
	public int getDevice() {
		return iniReader.getPropertyInt(SECTION_ID, "Device", DEFAULT_DEVICE);
	}

	@Override
	@Parameter(names = { "--deviceIndex", "-A" }, descriptionKey = "DEVICEINDEX", order = 101)
	public void setDevice(int device) {
		iniReader.setProperty(SECTION_ID, "Device", device);
	}

	/**
	 * Getter of the Playback/Recording frequency.
	 *
	 * @return Playback/Recording frequency
	 */
	@Override
	public final SamplingRate getSamplingRate() {
		return iniReader.getPropertyEnum(SECTION_ID, "Sampling Rate", DEFAULT_SAMPLING_RATE, SamplingRate.class);
	}

	/**
	 * Setter of the Playback/Recording frequency.
	 *
	 * @param samplingRate Playback/Recording frequency
	 */
	@Override
	@Parameter(names = { "--frequency", "-f" }, descriptionKey = "FREQUENCY", order = 102)
	public final void setSamplingRate(final SamplingRate samplingRate) {
		iniReader.setProperty(SECTION_ID, "Sampling Rate", samplingRate);
	}

	/**
	 * Getter of the sampling method.
	 *
	 * @return the sampling method
	 */
	@Override
	public final SamplingMethod getSampling() {
		return iniReader.getPropertyEnum(SECTION_ID, "Sampling", DEFAULT_SAMPLING, SamplingMethod.class);
	}

	/**
	 * Setter of the sampling method.
	 *
	 * @param method the sampling method
	 */
	@Override
	@Parameter(names = { "--sampling" }, descriptionKey = "SAMPLING", order = 103)
	public final void setSampling(final SamplingMethod method) {
		iniReader.setProperty(SECTION_ID, "Sampling", method);
	}

	/**
	 * Getter of the main SID volume setting.
	 *
	 * @return the main SID volume setting
	 */
	@Override
	public final float getMainVolume() {
		return iniReader.getPropertyFloat(SECTION_ID, "MainVolume", DEFAULT_MAIN_VOLUME);
	}

	/**
	 * Setter of the main SID volume setting.
	 *
	 * @param volume the main SID volume setting
	 */
	@Override
	@Parameter(names = { "--mainVolume" }, descriptionKey = "MAIN_VOLUME", order = 104)
	public final void setMainVolume(final float volume) {
		iniReader.setProperty(SECTION_ID, "MainVolume", volume);
	}

	/**
	 * Getter of the second SID volume setting.
	 *
	 * @return the second SID volume setting
	 */
	@Override
	public float getSecondVolume() {
		return iniReader.getPropertyFloat(SECTION_ID, "SecondVolume", DEFAULT_SECOND_VOLUME);
	}

	/**
	 * Setter of the second SID volume setting.
	 *
	 * @param volume the second SID volume setting
	 */
	@Override
	@Parameter(names = { "--secondVolume" }, descriptionKey = "SECOND_VOLUME", order = 105)
	public void setSecondVolume(final float volume) {
		iniReader.setProperty(SECTION_ID, "SecondVolume", volume);
	}

	/**
	 * Getter of the third SID volume setting.
	 *
	 * @return the third SID volume setting
	 */
	@Override
	public float getThirdVolume() {
		return iniReader.getPropertyFloat(SECTION_ID, "ThirdVolume", DEFAULT_THIRD_VOLUME);
	}

	/**
	 * Setter of the third SID volume setting.
	 *
	 * @param volume the third SID volume setting
	 */
	@Override
	@Parameter(names = { "--thirdVolume" }, descriptionKey = "THIRD_VOLUME", order = 106)
	public void setThirdVolume(final float volume) {
		iniReader.setProperty(SECTION_ID, "ThirdVolume", volume);
	}

	@Override
	public float getMainBalance() {
		return iniReader.getPropertyFloat(SECTION_ID, "MainBalance", DEFAULT_MAIN_BALANCE);
	}

	@Override
	@Parameter(names = { "--mainBalance" }, descriptionKey = "MAIN_BALANCE", order = 107)
	public void setMainBalance(float balance) {
		iniReader.setProperty(SECTION_ID, "MainBalance", balance);
	}

	@Override
	public float getSecondBalance() {
		return iniReader.getPropertyFloat(SECTION_ID, "SecondBalance", DEFAULT_SECOND_BALANCE);
	}

	@Override
	@Parameter(names = { "--secondBalance" }, descriptionKey = "SECOND_BALANCE", order = 108)
	public void setSecondBalance(float balance) {
		iniReader.setProperty(SECTION_ID, "SecondBalance", balance);
	}

	@Override
	public float getThirdBalance() {
		return iniReader.getPropertyFloat(SECTION_ID, "ThirdBalance", DEFAULT_THIRD_BALANCE);
	}

	@Override
	@Parameter(names = { "--thirdBalance" }, descriptionKey = "THIRD_BALANCE", order = 109)
	public void setThirdBalance(float balance) {
		iniReader.setProperty(SECTION_ID, "ThirdBalance", balance);
	}

	@Override
	public int getMainDelay() {
		return iniReader.getPropertyInt(SECTION_ID, "MainDelay", DEFAULT_MAIN_DELAY);
	}

	@Override
	@Parameter(names = { "--mainDelay" }, descriptionKey = "MAIN_DELAY", order = 110)
	public void setMainDelay(int delay) {
		iniReader.setProperty(SECTION_ID, "MainDelay", delay);
	}

	@Override
	public int getSecondDelay() {
		return iniReader.getPropertyInt(SECTION_ID, "SecondDelay", DEFAULT_SECOND_DELAY);
	}

	@Override
	@Parameter(names = { "--secondDelay" }, descriptionKey = "SECOND_DELAY", order = 111)
	public void setSecondDelay(int delay) {
		iniReader.setProperty(SECTION_ID, "SecondDelay", delay);
	}

	@Override
	public int getThirdDelay() {
		return iniReader.getPropertyInt(SECTION_ID, "ThirdDelay", DEFAULT_THIRD_DELAY);
	}

	@Override
	@Parameter(names = { "--thirdDelay" }, descriptionKey = "THIRD_DELAY", order = 112)
	public void setThirdDelay(int delay) {
		iniReader.setProperty(SECTION_ID, "ThirdDelay", delay);
	}

	@Override
	public int getBufferSize() {
		return iniReader.getPropertyInt(SECTION_ID, "Buffer Size", DEFAULT_BUFFER_SIZE);
	}

	@Override
	@Parameter(names = { "--bufferSize", "-B" }, descriptionKey = "BUFFER_SIZE", order = 113)
	public void setBufferSize(int bufferSize) {
		iniReader.setProperty(SECTION_ID, "Buffer Size", bufferSize);
	}

	@Override
	public int getAudioBufferSize() {
		return iniReader.getPropertyInt(SECTION_ID, "Audio Buffer Size", DEFAULT_AUDIO_BUFFER_SIZE);
	}

	@Override
	@Parameter(names = { "--audioBufferSize" }, descriptionKey = "AUDIO_BUFFER_SIZE", order = 114)
	public void setAudioBufferSize(int audioBufferSize) {
		iniReader.setProperty(SECTION_ID, "Audio Buffer Size", audioBufferSize);
	}

	@Override
	public int getCbr() {
		return iniReader.getPropertyInt(SECTION_ID, "MP3 CBR", DEFAULT_CBR);
	}

	@Override
	@Parameter(names = { "--cbr" }, descriptionKey = "CBR", order = 115)
	public void setCbr(int cbr) {
		iniReader.setProperty(SECTION_ID, "MP3 CBR", cbr);
	}

	@Override
	public boolean isVbr() {
		return iniReader.getPropertyBool(SECTION_ID, "MP3 VBR", DEFAULT_VBR);
	}

	@Override
	@Parameter(names = { "--vbr" }, descriptionKey = "VBR", arity = 1, order = 116)
	public void setVbr(boolean vbr) {
		iniReader.setProperty(SECTION_ID, "MP3 VBR", vbr);
	}

	@Override
	public int getVbrQuality() {
		return iniReader.getPropertyInt(SECTION_ID, "MP3 VBR Quality", DEFAULT_VBR_QUALITY);
	}

	@Override
	@Parameter(names = { "--vbrQuality" }, descriptionKey = "VBR_QUALITY", order = 117)
	public void setVbrQuality(int vbr) {
		iniReader.setProperty(SECTION_ID, "MP3 VBR Quality", vbr);
	}

	@Override
	public int getAudioCoderBitRate() {
		return iniReader.getPropertyInt(SECTION_ID, "Audio Coder Bit Rate", DEFAULT_AUDIO_CODER_BIT_RATE);
	}

	@Override
	@Parameter(names = { "--acBitRate" }, descriptionKey = "AUDIO_CODER_BIT_RATE", order = 118)
	public void setAudioCoderBitRate(int bitRate) {
		iniReader.setProperty(SECTION_ID, "Audio Coder Bit Rate", bitRate);
	}

	@Override
	public int getAudioCoderBitRateTolerance() {
		return iniReader.getPropertyInt(SECTION_ID, "Audio Coder Bit Rate Tolerance",
				DEFAULT_AUDIO_CODER_BIT_RATE_TOLERANCE);
	}

	@Override
	@Parameter(names = { "--acBitRateTolerance" }, descriptionKey = "AUDIO_CODER_BIT_RATE_TOLERANCE", order = 119)
	public void setAudioCoderBitRateTolerance(int bitRateTolerance) {
		iniReader.setProperty(SECTION_ID, "Audio Coder Bit Rate Tolerance", bitRateTolerance);
	}

	@Override
	public String getVideoStreamingUrl() {
		return iniReader.getPropertyString(SECTION_ID, "Video Streaming URL", DEFAULT_VIDEO_STREAMING_URL);
	}

	@Override
	public void setVideoStreamingUrl(String videoStreamingUrl) {
		iniReader.setProperty(SECTION_ID, "Video Streaming URL", videoStreamingUrl);
	}

	@Override
	public int getVideoCoderNumPicturesInGroupOfPictures() {
		return iniReader.getPropertyInt(SECTION_ID, "Video Coder GOP", DEFAULT_VIDEO_CODER_GOP);
	}

	@Override
	@Parameter(names = { "--vcGOP" }, descriptionKey = "VIDEO_CODER_GOP", order = 120)
	public void setVideoCoderNumPicturesInGroupOfPictures(int numPicturesInGroupOfPictures) {
		iniReader.setProperty(SECTION_ID, "Video Coder GOP", numPicturesInGroupOfPictures);
	}

	@Override
	public int getVideoCoderBitRate() {
		return iniReader.getPropertyInt(SECTION_ID, "Video Coder Bit Rate", DEFAULT_VIDEO_CODER_BIT_RATE);
	}

	@Override
	@Parameter(names = { "--vcBitRate" }, descriptionKey = "VIDEO_CODER_BIT_RATE", order = 121)
	public void setVideoCoderBitRate(int bitRate) {
		iniReader.setProperty(SECTION_ID, "Video Coder Bit Rate", bitRate);
	}

	@Override
	public int getVideoCoderBitRateTolerance() {
		return iniReader.getPropertyInt(SECTION_ID, "Video Coder Bit Rate Tolerance",
				DEFAULT_VIDEO_CODER_BIT_RATE_TOLERANCE);
	}

	@Override
	@Parameter(names = { "--vcBitRateTolerance" }, descriptionKey = "VIDEO_CODER_BIT_RATE_TOLERANCE", order = 122)
	public void setVideoCoderBitRateTolerance(int bitRateTolerance) {
		iniReader.setProperty(SECTION_ID, "Video Coder Bit Rate Tolerance", bitRateTolerance);
	}

	@Override
	public int getVideoCoderGlobalQuality() {
		return iniReader.getPropertyInt(SECTION_ID, "Video Coder Global Quality", DEFAULT_VIDEO_CODER_GLOBAL_QUALITY);
	}

	@Override
	@Parameter(names = { "--vcGlobalQuality" }, descriptionKey = "VIDEO_CODER_GLOBAL_QUALITY", order = 123)
	public void setVideoCoderGlobalQuality(int globalQuality) {
		iniReader.setProperty(SECTION_ID, "Video Coder Global Quality", globalQuality);
	}

	@Override
	public VideoCoderPreset getVideoCoderPreset() {
		return iniReader.getPropertyEnum(SECTION_ID, "Video Coder Preset", DEFAULT_VIDEO_CODER_PRESET,
				VideoCoderPreset.class);
	}

	@Override
	@Parameter(names = { "--vcPreset" }, descriptionKey = "VIDEO_CODER_PRESET", order = 124)
	public void setVideoCoderPreset(VideoCoderPreset preset) {
		iniReader.setProperty(SECTION_ID, "Video Coder Preset", preset);
	}

	@Override
	public final boolean isPlayOriginal() {
		return iniReader.getPropertyBool(SECTION_ID, "Play Original", DEFAULT_PLAY_ORIGINAL);
	}

	@Override
	public final void setPlayOriginal(final boolean original) {
		iniReader.setProperty(SECTION_ID, "Play Original", original);
	}

	@Override
	public final File getMp3() {
		return iniReader.getPropertyFile(SECTION_ID, "MP3 File", DEFAULT_MP3_FILE);
	}

	@Override
	public final void setMp3(final File recording) {
		iniReader.setProperty(SECTION_ID, "MP3 File", recording);
	}

	@Override
	public boolean getDelayBypass() {
		return iniReader.getPropertyBool(SECTION_ID, "Delay Bypass", DEFAULT_DELAY_BYPASS);
	}

	@Override
	@Parameter(names = { "--delayBypass" }, descriptionKey = "DELAY_BYPASS", arity = 1, order = 125)
	public void setDelayBypass(boolean delayBypass) {
		iniReader.setProperty(SECTION_ID, "Delay Bypass", delayBypass);
	}

	@Override
	public int getDelay() {
		return iniReader.getPropertyInt(SECTION_ID, "Delay", DEFAULT_DELAY);
	}

	@Override
	@Parameter(names = { "--delay" }, descriptionKey = "DELAY", order = 126)
	public void setDelay(int delay) {
		iniReader.setProperty(SECTION_ID, "Delay", delay);
	}

	@Override
	public int getDelayWetLevel() {
		return iniReader.getPropertyInt(SECTION_ID, "Delay Wet Level", DEFAULT_DELAY_WET_LEVEL);
	}

	@Override
	@Parameter(names = { "--delayWetLevel" }, descriptionKey = "DELAY_WET_LEVEL", order = 127)
	public void setDelayWetLevel(int delayWetLevel) {
		iniReader.setProperty(SECTION_ID, "Delay Wet Level", delayWetLevel);
	}

	@Override
	public int getDelayDryLevel() {
		return iniReader.getPropertyInt(SECTION_ID, "Delay Dry Level", DEFAULT_DELAY_DRY_LEVEL);
	}

	@Override
	@Parameter(names = { "--delayDryLevel" }, descriptionKey = "DELAY_DRY_LEVEL", order = 128)
	public void setDelayDryLevel(int delayDryLevel) {
		iniReader.setProperty(SECTION_ID, "Delay Dry Level", delayDryLevel);
	}

	@Override
	public int getDelayFeedbackLevel() {
		return iniReader.getPropertyInt(SECTION_ID, "Delay Feedback Level", DEFAULT_DELAY_FEEDBACK_LEVEL);
	}

	@Override
	@Parameter(names = { "--delayFeedbackLevel" }, descriptionKey = "DELAY_FEEDBACK_LEVEL", order = 129)
	public void setDelayFeedbackLevel(int delayFeedbackLevel) {
		iniReader.setProperty(SECTION_ID, "Delay Feedback Level", delayFeedbackLevel);
	}

	@Override
	public boolean getReverbBypass() {
		return iniReader.getPropertyBool(SECTION_ID, "Reverb Bypass", DEFAULT_REVERB_BYPASS);
	}

	@Override
	@Parameter(names = { "--reverbBypass" }, descriptionKey = "REVERB_BYPASS", arity = 1, order = 130)
	public void setReverbBypass(boolean reverbBypass) {
		iniReader.setProperty(SECTION_ID, "Reverb Bypass", reverbBypass);
	}

	@Override
	public float getReverbComb1Delay() {
		return iniReader.getPropertyFloat(SECTION_ID, "Reverb Comb1 Delay", DEFAULT_REVERB_COMB1_DELAY);
	}

	@Override
	@Parameter(names = { "--reverbComb1Delay" }, descriptionKey = "REVERB_COMB1_DELAY", order = 131)
	public void setReverbComb1Delay(float reverbComb1Delay) {
		iniReader.setProperty(SECTION_ID, "Reverb Comb1 Delay", reverbComb1Delay);
	}

	@Override
	public float getReverbComb2Delay() {
		return iniReader.getPropertyFloat(SECTION_ID, "Reverb Comb2 Delay", DEFAULT_REVERB_COMB2_DELAY);
	}

	@Override
	@Parameter(names = { "--reverbComb2Delay" }, descriptionKey = "REVERB_COMB2_DELAY", order = 132)
	public void setReverbComb2Delay(float reverbComb2Delay) {
		iniReader.setProperty(SECTION_ID, "Reverb Comb2 Delay", reverbComb2Delay);
	}

	@Override
	public float getReverbComb3Delay() {
		return iniReader.getPropertyFloat(SECTION_ID, "Reverb Comb3 Delay", DEFAULT_REVERB_COMB3_DELAY);
	}

	@Override
	@Parameter(names = { "--reverbComb3Delay" }, descriptionKey = "REVERB_COMB3_DELAY", order = 133)
	public void setReverbComb3Delay(float reverbComb3Delay) {
		iniReader.setProperty(SECTION_ID, "Reverb Comb3 Delay", reverbComb3Delay);
	}

	@Override
	public float getReverbComb4Delay() {
		return iniReader.getPropertyFloat(SECTION_ID, "Reverb Comb4 Delay", DEFAULT_REVERB_COMB4_DELAY);
	}

	@Override
	@Parameter(names = { "--reverbComb4Delay" }, descriptionKey = "REVERB_COMB4_DELAY", order = 134)
	public void setReverbComb4Delay(float reverbComb4Delay) {
		iniReader.setProperty(SECTION_ID, "Reverb Comb4 Delay", reverbComb4Delay);
	}

	@Override
	public float getReverbAllPass1Delay() {
		return iniReader.getPropertyFloat(SECTION_ID, "Reverb All Pass1 Delay", DEFAULT_REVERB_COMB1_DELAY);
	}

	@Override
	@Parameter(names = { "--reverbAllPass1Delay" }, descriptionKey = "REVERB_ALL_PASS1_DELAY", order = 135)
	public void setReverbAllPass1Delay(float reverbAllPass1Delay) {
		iniReader.setProperty(SECTION_ID, "Reverb All Pass1 Delay", reverbAllPass1Delay);
	}

	@Override
	public float getReverbAllPass2Delay() {
		return iniReader.getPropertyFloat(SECTION_ID, "Reverb All Pass2 Delay", DEFAULT_REVERB_COMB2_DELAY);
	}

	@Override
	@Parameter(names = { "--reverbAllPass2Delay" }, descriptionKey = "REVERB_ALL_PASS2_DELAY", order = 136)
	public void setReverbAllPass2Delay(float reverbAllPass2Delay) {
		iniReader.setProperty(SECTION_ID, "Reverb All Pass2 Delay", reverbAllPass2Delay);
	}

	@Override
	public float getReverbSustainDelay() {
		return iniReader.getPropertyFloat(SECTION_ID, "Reverb Sustain Delay", DEFAULT_REVERB_SUSTAIN_DELAY);
	}

	@Override
	@Parameter(names = { "--reverbSustainDelay" }, descriptionKey = "REVERB_SUSTAIN_DELAY", order = 137)
	public void setReverbSustainDelay(float reverbSustainDelay) {
		iniReader.setProperty(SECTION_ID, "Reverb Sustain Delay", reverbSustainDelay);
	}

	@Override
	public float getReverbDryWetMix() {
		return iniReader.getPropertyFloat(SECTION_ID, "Reverb Dry Wet Mix", DEFAULT_REVERB_DRY_WET_MIX);
	}

	@Override
	@Parameter(names = { "--reverbDryWetMix" }, descriptionKey = "REVERB_DRY_WET_MIX", order = 138)
	public void setReverbDryWetMix(float reverbDryWetMix) {
		iniReader.setProperty(SECTION_ID, "Reverb DryWetMix", reverbDryWetMix);
	}

	@Override
	public boolean isExsidFakeStereo() {
		return iniReader.getPropertyBool(SECTION_ID, "ExSID Fake Stereo", DEFAULT_EXSID_FAKE_STEREO);
	}

	@Override
	@Parameter(names = { "--exsidFakeStereo" }, descriptionKey = "EXSID_FAKE_STEREO", arity = 1, order = 139)
	public void setExsidFakeStereo(boolean exsidFakeStereo) {
		iniReader.setProperty(SECTION_ID, "ExSID Fake Stereo", exsidFakeStereo);
	}

	@Override
	public String toString() {
		return BeanToStringConverter.toString(this);
	}

}