package client.teavm;

public interface IConfigResolverTeaVM {

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
