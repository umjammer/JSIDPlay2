package client.teavm;

import org.teavm.jso.JSBody;

public class JavascriptAudioDriver implements IAudioDriver {

	@Override
	public void processSamples(float[] resultL, float[] resultR, int length) {
		JavascriptAudioDriver.processSamplesJS(resultL, resultR, length);
	}

	@Override
	public void processPixels(int[] pixels, int length) {
		JavascriptAudioDriver.processPixelsJS(pixels, length);
	}

	@Override
	public void processSidWrite(int relTime, int addr, int value) {
		JavascriptAudioDriver.processSidWriteJS(relTime, addr, value);
	}

	@JSBody(params = { "resultL", "resultR", "length" }, script = "processSamples(resultL, resultR, length)")
	public static native void processSamplesJS(float[] resultL, float[] resultR, int length);

	@JSBody(params = { "pixels", "length" }, script = "processPixels(pixels, length)")
	public static native void processPixelsJS(int[] pixels, int length);

	@JSBody(params = { "relTime", "addr", "value" }, script = "processSidWrite(relTime, addr, value)")
	public static native void processSidWriteJS(int relTime, int addr, int value);

}
