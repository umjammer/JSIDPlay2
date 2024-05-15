package client.teavm.js;

import client.teavm.IConfigResolverTeaVM;

public class JavaScriptConfigResolver implements IConfigResolverTeaVM {

	private boolean palEmulation;
	private int bufferSize;
	private int audioBufferSize;
	private int samplingRate;
	private boolean samplingMethodResample;
	private boolean reverbBypass;
	private int defaultClockSpeed;
	private boolean defaultSidModel8580;
	private boolean jiffyDosInstalled;

	public JavaScriptConfigResolver(String[] args) {
		this.palEmulation = Boolean.TRUE.equals(Boolean.valueOf(args[0]));
		this.bufferSize = Integer.valueOf(args[1]);
		this.audioBufferSize = Integer.valueOf(args[2]);
		this.samplingRate = Integer.valueOf(args[3]);
		this.samplingMethodResample = Boolean.TRUE.equals(Boolean.valueOf(args[4]));
		this.reverbBypass = Boolean.TRUE.equals(Boolean.valueOf(args[5]));
		this.defaultClockSpeed = Integer.valueOf(args[6]);
		this.defaultSidModel8580 = Boolean.TRUE.equals(Boolean.valueOf(args[7]));
		this.jiffyDosInstalled = Boolean.TRUE.equals(Boolean.valueOf(args[8]));
	}

	@Override
	public boolean isPalEmulation() {
		return palEmulation;
	}

	@Override
	public int getSamplingRateAsInt() {
		return samplingRate;
	}

	@Override
	public boolean getSamplingMethodResample() {
		return samplingMethodResample;
	}

	@Override
	public boolean getReverbBypass() {
		return reverbBypass;
	}

	@Override
	public int getBufferSize() {
		return bufferSize;
	}

	@Override
	public int getAudioBufferSize() {
		return audioBufferSize;
	}

	@Override
	public boolean getDefaultSidModel8580() {
		return defaultSidModel8580;
	}

	@Override
	public int getDefaultClockSpeedAsInt() {
		return defaultClockSpeed;
	}

	@Override
	public boolean isJiffyDosInstalled() {
		return jiffyDosInstalled;
	}

}
