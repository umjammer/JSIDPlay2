package client.teavm;

public interface IAudioDriver {

	void processSamples(float[] resultL, float[] resultR, int length);

	void processPixels(int[] pixels, int length);

	void processSidWrite(int relTime, int addr, int value);

}
