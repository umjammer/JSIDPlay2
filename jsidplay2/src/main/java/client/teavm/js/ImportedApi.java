package client.teavm.js;

import org.teavm.jso.JSBody;

import client.teavm.common.IImportedApi;

public class ImportedApi implements IImportedApi {

	private boolean palEmulation;
	private int bufferSize;
	private int audioBufferSize;
	private int samplingRateAsInt;
	private boolean samplingMethodResample;
	private boolean reverbBypass;
	private int defaultClockSpeedAsInt;
	private boolean defaultEmulationReSid;
	private boolean defaultSidModel8580;
	private boolean jiffyDosInstalled;

	public ImportedApi(String[] args) {
		this.palEmulation = Boolean.TRUE.equals(Boolean.valueOf(args[0]));
		this.bufferSize = Integer.valueOf(args[1]);
		this.audioBufferSize = Integer.valueOf(args[2]);
		this.samplingRateAsInt = Integer.valueOf(args[3]);
		this.samplingMethodResample = Boolean.TRUE.equals(Boolean.valueOf(args[4]));
		this.reverbBypass = Boolean.TRUE.equals(Boolean.valueOf(args[5]));
		this.defaultClockSpeedAsInt = Integer.valueOf(args[6]);
		this.defaultEmulationReSid = Boolean.TRUE.equals(Boolean.valueOf(args[7]));
		this.defaultSidModel8580 = Boolean.TRUE.equals(Boolean.valueOf(args[8]));
		this.jiffyDosInstalled = Boolean.TRUE.equals(Boolean.valueOf(args[9]));
	}

	@Override
	public boolean isPalEmulation() {
		return palEmulation;
	}

	@Override
	public int getSamplingRateAsInt() {
		return samplingRateAsInt;
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
	public boolean getDefaultEmulationReSid() {
		return defaultEmulationReSid;
	}

	@Override
	public boolean getDefaultSidModel8580() {
		return defaultSidModel8580;
	}

	@Override
	public int getDefaultClockSpeedAsInt() {
		return defaultClockSpeedAsInt;
	}

	@Override
	public boolean isJiffyDosInstalled() {
		return jiffyDosInstalled;
	}

	@Override
	public void processSamples(float[] resultL, float[] resultR, int length) {
		processSamplesJS(resultL, resultR, length);
	}

	@Override
	public void processPixels(byte[] pixels, int length) {
		processPixelsJS(pixels, length);
	}

	@Override
	public void processSidWrite(int relTime, int addr, int value) {
		processSidWriteJS(relTime, addr, value);
	}

	/* This methods maps to JavaScript methods in a web page. */
	@JSBody(params = { "lf", "ri", "le" }, script = "postMessage({eventType:'SAMPLES',"
			+ "eventData:{left:lf,right:ri,length:le}})")
	public static native void processSamplesJS(float[] lf, float[] ri, int le);

	@JSBody(params = { "pi", "le" }, script = "postMessage({eventType:'FRAME',"
			+ "eventData:{image:new Uint8Array(pi,0,le).slice()}})")
	public static native void processPixelsJS(byte[] pi, int le);

	@JSBody(params = { "ti", "ad", "va" }, script = "postMessage({eventType: 'SID_WRITE',"
			+ "eventData:{relTime:ti,addr:ad,value:va}})")
	public static native void processSidWriteJS(int ti, int ad, int va);

}
