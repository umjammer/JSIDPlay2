package client.teavm.wasm;

import org.teavm.interop.Import;

import client.teavm.common.IImportedApi;

public class ImportedApi implements IImportedApi {

	private static final String AUDIO_DRIVER = "audiodriver";

	private static final String SIDPLAY2_SECTION = "sidplay2section";

	private static final String AUDIO_SECTION = "audiosection";

	private static final String EMULATION_SECTION = "emulationsection";

	private static final String C1541_SECTION = "c1541section";

	private static final String FILTER_SECTION = "filtersection";

	public ImportedApi(String[] args) {
	}

	@Override
	public boolean isPalEmulation() {
		return isPalEmulationJS();
	}

	@Override
	public int getSamplingRateAsInt() {
		return getSamplingRateAsIntJS();
	}

	@Override
	public boolean getSamplingMethodResample() {
		return getSamplingMethodResampleJS();
	}

	@Override
	public boolean getReverbBypass() {
		return getReverbBypassJS();
	}

	@Override
	public int getBufferSize() {
		return getBufferSizeJS();
	}

	@Override
	public int getAudioBufferSize() {
		return getAudioBufferSizeJS();
	}

	@Override
	public boolean getDefaultSidModel8580() {
		return getDefaultSidModel8580JS();
	}

	@Override
	public int getDefaultClockSpeedAsInt() {
		return getDefaultClockSpeedAsIntJS();
	}

	@Override
	public boolean isJiffyDosInstalled() {
		return isJiffyDosInstalledJS();
	}

	@Override
	public boolean getDefaultEmulationReSid() {
		return getDefaultEmulationReSidJS();
	}

	@Override
	public String getFilter6581() {
		return getFilter6581JS();
	}

	@Override
	public String getFilter8580() {
		return getFilter8580JS();
	}

	@Override
	public String getStereoFilter6581() {
		return getStereoFilter6581JS();
	}

	@Override
	public String getStereoFilter8580() {
		return getStereoFilter8580JS();
	}

	@Override
	public String getThirdSIDFilter6581() {
		return getThirdSIDFilter6581JS();
	}

	@Override
	public String getThirdSIDFilter8580() {
		return getThirdSIDFilter8580JS();
	}

	@Override
	public String getReSIDfpFilter6581() {
		return getReSIDfpFilter6581JS();
	}

	@Override
	public String getReSIDfpFilter8580() {
		return getReSIDfpFilter8580JS();
	}

	@Override
	public String getReSIDfpStereoFilter6581() {
		return getReSIDfpStereoFilter6581JS();
	}

	@Override
	public String getReSIDfpStereoFilter8580() {
		return getReSIDfpStereoFilter8580JS();
	}

	@Override
	public String getReSIDfpThirdSIDFilter6581() {
		return getReSIDfpThirdSIDFilter6581JS();
	}

	@Override
	public String getReSIDfpThirdSIDFilter8580() {
		return getReSIDfpThirdSIDFilter8580JS();
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

	/* This methods maps to a JavaScript methods in a web page. */
	@Import(module = SIDPLAY2_SECTION, name = "getPalEmulation")
	public static native boolean isPalEmulationJS();

	@Import(module = AUDIO_SECTION, name = "getSamplingRate")
	public static native int getSamplingRateAsIntJS();

	@Import(module = AUDIO_SECTION, name = "getSamplingMethodResample")
	public static native boolean getSamplingMethodResampleJS();

	@Import(module = AUDIO_SECTION, name = "getReverbBypass")
	public static native boolean getReverbBypassJS();

	@Import(module = AUDIO_SECTION, name = "getBufferSize")
	public static native int getBufferSizeJS();

	@Import(module = AUDIO_SECTION, name = "getAudioBufferSize")
	public static native int getAudioBufferSizeJS();

	@Import(module = EMULATION_SECTION, name = "getDefaultEmulationReSid")
	public static native boolean getDefaultEmulationReSidJS();

	@Import(module = EMULATION_SECTION, name = "getDefaultSidModel8580")
	public static native boolean getDefaultSidModel8580JS();

	@Import(module = EMULATION_SECTION, name = "getDefaultClockSpeed")
	public static native int getDefaultClockSpeedAsIntJS();

	@Import(module = C1541_SECTION, name = "isJiffyDosInstalled")
	public static native boolean isJiffyDosInstalledJS();

	@Import(module = FILTER_SECTION, name = "getFilter6581")
	public static native String getFilter6581JS();

	@Import(module = FILTER_SECTION, name = "getFilter8580")
	public static native String getFilter8580JS();

	@Import(module = FILTER_SECTION, name = "getStereoFilter6581")
	public static native String getStereoFilter6581JS();

	@Import(module = FILTER_SECTION, name = "getStereoFilter8580")
	public static native String getStereoFilter8580JS();

	@Import(module = FILTER_SECTION, name = "getThirdSIDFilter6581")
	public static native String getThirdSIDFilter6581JS();

	@Import(module = FILTER_SECTION, name = "getThirdSIDFilter8580")
	public static native String getThirdSIDFilter8580JS();

	@Import(module = FILTER_SECTION, name = "getReSIDfpFilter6581")
	public static native String getReSIDfpFilter6581JS();

	@Import(module = FILTER_SECTION, name = "getReSIDfpFilter8580")
	public static native String getReSIDfpFilter8580JS();

	@Import(module = FILTER_SECTION, name = "getReSIDfpStereoFilter6581")
	public static native String getReSIDfpStereoFilter6581JS();

	@Import(module = FILTER_SECTION, name = "getReSIDfpStereoFilter8580")
	public static native String getReSIDfpStereoFilter8580JS();

	@Import(module = FILTER_SECTION, name = "getReSIDfpThirdSIDFilter6581")
	public static native String getReSIDfpThirdSIDFilter6581JS();

	@Import(module = FILTER_SECTION, name = "getReSIDfpThirdSIDFilter8580")
	public static native String getReSIDfpThirdSIDFilter8580JS();

	@Import(module = AUDIO_DRIVER, name = "processSamples")
	public static native void processSamplesJS(float[] resultL, float[] resultR, int length);

	@Import(module = AUDIO_DRIVER, name = "processPixels")
	public static native void processPixelsJS(byte[] pixels, int length);

	@Import(module = AUDIO_DRIVER, name = "processSidWrite")
	public static native void processSidWriteJS(int relTime, int addr, int value);

}
