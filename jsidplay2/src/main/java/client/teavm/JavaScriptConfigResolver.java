package client.teavm;

public class JavaScriptConfigResolver implements IConfigResolver {

	private boolean palEmulation;
	private int bufferSize;
	private int audioBufferSize;
	private int samplingRate;
	private boolean samplingMethodResample;
	private boolean reverbBypass;
	private int defaultClockSpeed;
	private boolean defaultSidModel8580;
	private boolean jiffyDosInstalled;

	public JavaScriptConfigResolver(boolean palEmulation, int bufferSize, int audioBufferSize, int samplingRate,
			boolean samplingMethodResample, boolean reverbBypass, int defaultClockSpeed, boolean defaultSidModel8580,
			boolean jiffyDosInstalled) {
		this.palEmulation = palEmulation;
		this.bufferSize = bufferSize;
		this.audioBufferSize = audioBufferSize;
		this.samplingRate = samplingRate;
		this.samplingMethodResample = samplingMethodResample;
		this.reverbBypass = reverbBypass;
		this.defaultClockSpeed = defaultClockSpeed;
		this.defaultSidModel8580 = defaultSidModel8580;
		this.jiffyDosInstalled = jiffyDosInstalled;
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
