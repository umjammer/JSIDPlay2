package client.teavm;

public interface IConfigResolver {

	int getSamplingRateAsInt();

	boolean getSamplingMethodResample();

	boolean getReverbBypass();

	int getBufferSize();

	int getAudioBufferSize();

	boolean isJiffyDosInstalled();

	boolean getDefaultSidModel8580();

	int getDefaultClockSpeedAsInt();

	boolean isPalEmulation();

}
