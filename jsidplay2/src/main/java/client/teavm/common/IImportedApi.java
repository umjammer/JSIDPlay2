package client.teavm.common;

/**
 * Imports from JavaScript
 */
public interface IImportedApi {

	boolean isPalEmulation();

	int getBufferSize();

	int getAudioBufferSize();

	int getSamplingRateAsInt();

	boolean getSamplingMethodResample();

	boolean getReverbBypass();

	int getDefaultClockSpeedAsInt();

	boolean isJiffyDosInstalled();

	void timerEnd();

	void processSamples(float[] resultL, float[] resultR, int length);

	void processPixels(byte[] array, int length);

	void processSidWrite(double absTime, int relTime, int addr, int value);

}
