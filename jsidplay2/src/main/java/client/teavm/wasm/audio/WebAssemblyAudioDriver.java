package client.teavm.wasm.audio;

import org.teavm.interop.Import;

import client.teavm.common.IAudioDriverTeaVM;

public class WebAssemblyAudioDriver implements IAudioDriverTeaVM {

	@Override
	public void processSamples(float[] resultL, float[] resultR, int length) {
		WebAssemblyAudioDriver.processSamplesJS(resultL, resultR, length);
	}

	@Override
	public void processPixels(byte[] pixels, int length) {
		// since WASM version will interpret int array as byte array from heap, we just
		// report the byte length here. Works only for ABGR color data on litte-endian
		// platforms, though.
		WebAssemblyAudioDriver.processPixelsJS(pixels, length);
	}

	@Override
	public void processSidWrite(int relTime, int addr, int value) {
		WebAssemblyAudioDriver.processSidWriteJS(relTime, addr, value);
	}

	/* This methods maps to JavaScript methods in a web page. */
	@Import(module = "audiodriver", name = "processSamples")
	public static native void processSamplesJS(float[] resultL, float[] resultR, int length);

	@Import(module = "audiodriver", name = "processPixels")
	public static native void processPixelsJS(byte[] pixels, int length);

	@Import(module = "audiodriver", name = "processSidWrite")
	public static native void processSidWriteJS(int relTime, int addr, int value);
}
