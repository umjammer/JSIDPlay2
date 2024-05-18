package client.teavm.common;

/**
 * Interface to get configuration data from JavaScript
 */
public interface IConfigResolverTeaVM {

	int getSamplingRateAsInt();

	boolean getSamplingMethodResample();

	boolean getReverbBypass();

	int getBufferSize();

	int getAudioBufferSize();

	boolean isJiffyDosInstalled();

	String getDefaultEmulationAsString();

	boolean getDefaultSidModel8580();

	int getDefaultClockSpeedAsInt();

	boolean isPalEmulation();

}
