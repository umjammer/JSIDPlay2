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

	String getFilter6581();

	String getFilter8580();

	String getStereoFilter6581();

	String getStereoFilter8580();

	String getThirdSIDFilter6581();

	String getThirdSIDFilter8580();

	String getReSIDfpFilter6581();

	String getReSIDfpFilter8580();

	String getReSIDfpStereoFilter6581();

	String getReSIDfpStereoFilter8580();

	String getReSIDfpThirdSIDFilter6581();

	String getReSIDfpThirdSIDFilter8580();

}
