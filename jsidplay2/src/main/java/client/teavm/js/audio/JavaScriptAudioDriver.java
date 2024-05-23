package client.teavm.js.audio;

import org.teavm.jso.JSBody;

import client.teavm.common.audio.IAudioDriverTeaVM;

public class JavaScriptAudioDriver implements IAudioDriverTeaVM {

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
