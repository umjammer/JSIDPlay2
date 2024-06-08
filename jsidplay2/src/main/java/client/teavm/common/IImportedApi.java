package client.teavm.common;

/**
 * Imports from JavaScript
 */
public interface IImportedApi {

	int getSamplingRateAsInt();

	boolean getSamplingMethodResample();

	boolean getReverbBypass();

	int getBufferSize();

	int getAudioBufferSize();

	boolean isJiffyDosInstalled();

	boolean getDefaultEmulationReSid();

	boolean getDefaultSidModel8580();

	int getDefaultClockSpeedAsInt();

	boolean isPalEmulation();

	void processSamples(float[] resultL, float[] resultR, int length);

	void processPixels(byte[] array, int length);

	void processSidWrite(int relTime, int addr, int value);

}
