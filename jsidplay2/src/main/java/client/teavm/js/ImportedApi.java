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
	private final boolean defaultEmulationReSid;
	private final boolean defaultSidModel8580;
	private final boolean jiffyDosInstalled;
	private final String filter6581;
	private final String filter8580;
	private final String stereoFilter6581;
	private final String stereoFilter8580;
	private final String thirdSIDFilter6581;
	private final String thirdSIDFilter8580;
	private final String reSIDfpFilter6581;
	private final String reSIDfpFilter8580;
	private final String reSIDfpStereoFilter6581;
	private final String reSIDfpStereoFilter8580;
	private final String reSIDfpThirdFilter6581;
	private final String reSIDfpThirdFilter8580;

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
		this.filter6581 = "" + args[10];
		this.filter8580 = "" + args[11];
		this.stereoFilter6581 = "" + args[12];
		this.stereoFilter8580 = "" + args[13];
		this.thirdSIDFilter6581 = "" + args[14];
		this.thirdSIDFilter8580 = "" + args[15];
		this.reSIDfpFilter6581 = "" + args[16];
		this.reSIDfpFilter8580 = "" + args[17];
		this.reSIDfpStereoFilter6581 = "" + args[18];
		this.reSIDfpStereoFilter8580 = "" + args[19];
		this.reSIDfpThirdFilter6581 = "" + args[20];
		this.reSIDfpThirdFilter8580 = "" + args[21];
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
	public String getFilter6581() {
		return filter6581;
	}

	@Override
	public String getFilter8580() {
		return filter8580;
	}

	@Override
	public String getStereoFilter6581() {
		return stereoFilter6581;
	}

	@Override
	public String getStereoFilter8580() {
		return stereoFilter8580;
	}

	@Override
	public String getThirdSIDFilter6581() {
		return thirdSIDFilter6581;
	}

	@Override
	public String getThirdSIDFilter8580() {
		return thirdSIDFilter8580;
	}

	@Override
	public String getReSIDfpFilter6581() {
		return reSIDfpFilter6581;
	}

	@Override
	public String getReSIDfpFilter8580() {
		return reSIDfpFilter8580;
	}

	@Override
	public String getReSIDfpStereoFilter6581() {
		return reSIDfpStereoFilter6581;
	}

	@Override
	public String getReSIDfpStereoFilter8580() {
		return reSIDfpStereoFilter8580;
	}

	@Override
	public String getReSIDfpThirdSIDFilter6581() {
		return reSIDfpThirdFilter6581;
	}

	@Override
	public String getReSIDfpThirdSIDFilter8580() {
		return reSIDfpThirdFilter8580;
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
	public static native void processSamplesJS(@JSByRef float[] lf, @JSByRef float[] ri, int le);

	@JSBody(params = { "pi", "le" }, script = "postMessage({eventType:'FRAME',"
			+ "eventData:{image:new Uint8Array(pi,0,le)}})")
	public static native void processPixelsJS(@JSByRef byte[] pi, int le);

	@JSBody(params = { "ti", "ad", "va" }, script = "postMessage({eventType: 'SID_WRITE',"
			+ "eventData:{relTime:ti,addr:ad,value:va}})")
	public static native void processSidWriteJS(int ti, int ad, int va);

}
