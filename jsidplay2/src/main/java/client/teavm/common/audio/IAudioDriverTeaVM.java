package client.teavm.common.audio;

/**
 * Imports from JavaScript
 */
public interface IAudioDriverTeaVM {

	void processSamples(float[] resultL, float[] resultR, int length);

	void processPixels(byte[] array, int length);

	void processSidWrite(int relTime, int addr, int value);

}
