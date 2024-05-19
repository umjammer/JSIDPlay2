package client.teavm.js.audio;

import org.teavm.jso.JSBody;

import client.teavm.common.IAudioDriverTeaVM;

public class JavaScriptAudioDriver implements IAudioDriverTeaVM {

	@Override
	public void processSamples(float[] resultL, float[] resultR, int length) {
		JavaScriptAudioDriver.processSamplesJS(resultL, resultR, length);
	}

	@Override
	public void processPixels(byte[] pixels, int length) {
		JavaScriptAudioDriver.processPixelsJS(pixels, length);
	}

	@Override
	public void processSidWrite(int relTime, int addr, int value) {
		JavaScriptAudioDriver.processSidWriteJS(relTime, addr, value);
	}

	/* This methods maps to JavaScript methods in a web page. */
	@JSBody(params = { "resultL", "resultR", "length" }, script = "processSamples(resultL, resultR, length)")
	public static native void processSamplesJS(float[] resultL, float[] resultR, int length);

	@JSBody(params = { "pixels", "length" }, script = "processPixels(pixels, length)")
	public static native void processPixelsJS(byte[] pixels, int length);

	@JSBody(params = { "relTime", "addr", "value" }, script = "processSidWrite(relTime, addr, value)")
	public static native void processSidWriteJS(int relTime, int addr, int value);

}
