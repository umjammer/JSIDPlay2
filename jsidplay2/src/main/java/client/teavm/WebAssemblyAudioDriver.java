package client.teavm;

import org.teavm.interop.Import;

public class WebAssemblyAudioDriver implements IAudioDriver {

	@Override
	public void processSamples(float[] resultL, float[] resultR, int length) {
		WebAssemblyAudioDriver.processSamplesJS(resultL, resultR, length);
	}

	@Override
	public void processPixels(int[] pixels, int length) {
		WebAssemblyAudioDriver.processPixelsJS(pixels, length);
	}

	@Override
	public void processSidWrite(int relTime, int addr, int value) {
		WebAssemblyAudioDriver.processSidWriteJS(relTime, addr, value);
	}

	/* This methods maps to a JavaScript methods in a web page. */
	@Import(module = "audiodriver", name = "processSamples")
	public static native void processSamplesJS(float[] resultL, float[] resultR, int length);

	@Import(module = "audiodriver", name = "processPixels")
	public static native void processPixelsJS(int[] pixels, int length);

	@Import(module = "audiodriver", name = "processSidWrite")
	public static native void processSidWriteJS(int relTime, int addr, int value);
}
