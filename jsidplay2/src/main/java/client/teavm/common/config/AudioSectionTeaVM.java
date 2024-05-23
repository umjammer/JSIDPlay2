package client.teavm.common.config;

import java.io.File;

import libsidplay.common.SamplingMethod;
import libsidplay.common.SamplingRate;
import libsidplay.common.VideoCoderPreset;
import libsidplay.config.IAudioSection;
import sidplay.audio.Audio;

public final class AudioSectionTeaVM implements IAudioSection {

	private IConfigResolverTeaVM resolver;

	public AudioSectionTeaVM(IConfigResolverTeaVM resolver) {
		this.resolver = resolver;
	}

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
		switch (resolver.getSamplingRateAsInt()) {
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

	@Override
	public SamplingMethod getSampling() {
		if (resolver.getSamplingMethodResample()) {
			return SamplingMethod.RESAMPLE;
		} else {
			return SamplingMethod.DECIMATE;
		}
	}

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
	public boolean getReverbBypass() {
		return resolver.getReverbBypass();
	}

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
	public int getBufferSize() {
		return resolver.getBufferSize();
	}

	@Override
	public int getAudioCoderBitRateTolerance() {
		return 64000;
	}

	@Override
	public int getAudioCoderBitRate() {
		return 128000;
	}

	@Override
	public int getAudioBufferSize() {
		return resolver.getAudioBufferSize();
	}

	@Override
	public Audio getAudio() {
		return Audio.SOUNDCARD;
	}
}