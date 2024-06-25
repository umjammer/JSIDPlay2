package client.teavm.js;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSByRef;

import client.teavm.common.IImportedApi;

public class ImportedApi implements IImportedApi {

	private final boolean palEmulation;
	private final int bufferSize;
	private final int audioBufferSize;
	private final int samplingRateAsInt;
	private final boolean samplingMethodResample;
	private final boolean reverbBypass;
	private final int defaultClockSpeedAsInt;
	private final boolean jiffyDosInstalled;

	public ImportedApi(String[] args) {
		this.palEmulation = Boolean.TRUE.equals(Boolean.valueOf(args[0]));
		this.bufferSize = Integer.valueOf(args[1]);
		this.audioBufferSize = Integer.valueOf(args[2]);
		this.samplingRateAsInt = Integer.valueOf(args[3]);
		this.samplingMethodResample = Boolean.TRUE.equals(Boolean.valueOf(args[4]));
		this.reverbBypass = Boolean.TRUE.equals(Boolean.valueOf(args[5]));
		this.defaultClockSpeedAsInt = Integer.valueOf(args[6]);
		this.jiffyDosInstalled = Boolean.TRUE.equals(Boolean.valueOf(args[7]));
	}

	@Override
	public boolean isPalEmulation() {
		return palEmulation;
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

	@Override
	public void timerEnd() {
		processTimerEndJS();
	}

	/* This methods maps to JavaScript methods in a web page. */

	@JSBody(params = { "lf", "ri", "le" }, script = "postMessage({eventType:'SAMPLES',"
			+ "eventData:{left:lf,right:ri,length:le}})")
	public static native void processSamplesJS(@JSByRef float[] lf, @JSByRef float[] ri, int le);

	@JSBody(params = { "pi", "le" }, script = "postMessage({eventType:'FRAME',"
			+ "eventData:{image:new Uint8Array(pi,0,le)}})")
	public static native void processPixelsJS(@JSByRef byte[] pi, int le);

	@JSBody(params = { "ti", "ad", "va" }, script = "postMessage({eventType: 'SID_WRITE',"
			+ "eventData:{relTime:ti,addr:ad,value:va}})")
	public static native void processSidWriteJS(int ti, int ad, int va);

	@JSBody(script = "postMessage({ eventType: 'TIMER_END' })")
	public static native void processTimerEndJS();
}
