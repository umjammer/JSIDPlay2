package client.teavm.common.config;

/**
 * Imports from JavaScript
 */
public interface IConfigResolverTeaVM {

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

}
