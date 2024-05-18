package client.teavm.js;

import org.teavm.jso.JSBody;

import client.teavm.common.IAudioDriverTeaVM;
import libsidplay.components.mos656x.VIC;

public class JavaScriptAudioDriver implements IAudioDriverTeaVM {

	private final byte[] byteArray;

	public JavaScriptAudioDriver() {
		byteArray = new byte[VIC.MAX_WIDTH * VIC.MAX_HEIGHT << 2];
	}

	@Override
	public void processSamples(float[] resultL, float[] resultR, int length) {
		JavaScriptAudioDriver.processSamplesJS(resultL, resultR, length);
	}

	@Override
	public void processPixels(int[] pixels, int length) {
		// Unfortunately we must convert here, however the JavaScript version is fast as
		// hell :-)
		int byteArrayLength = length << 2;
		for (int i = 0; i < byteArrayLength; i += 4) {
			int pixel = pixels[i >> 2];
			byteArray[i] = (byte) ((pixel >> 24) & 0xff);
			byteArray[i + 1] = (byte) ((pixel >> 16) & 0xff);
			byteArray[i + 2] = (byte) ((pixel >> 8) & 0xff);
			byteArray[i + 3] = (byte) (pixel & 0xff);
		}
		JavaScriptAudioDriver.processPixelsJS(byteArray, byteArrayLength);
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
