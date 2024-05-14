package client.teavm;

import org.teavm.interop.Import;

public class WebAssemblyAudioDriver implements IAudioDriver {

	/* This methods maps to a JavaScript methods in a web page. */
	@Import(module = "audiodriver", name = "processSamples")
	public native void processSamples(float[] resultL, float[] resultR, int length);

	@Import(module = "audiodriver", name = "processPixels")
	public native void processPixels(int[] pixels, int length);

	@Import(module = "audiodriver", name = "processSidWrite")
	public native void processSidWrite(int relTime, int addr, int value);
}
