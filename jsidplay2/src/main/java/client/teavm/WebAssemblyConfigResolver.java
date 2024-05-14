package client.teavm;

import org.teavm.interop.Import;

public class WebAssemblyConfigResolver implements IConfigResolver {

	private static final String SIDPLAY2_SECTION = "sidplay2section";

	private static final String AUDIO_SECTION = "audiosection";

	private static final String EMULATION_SECTION = "emulationsection";

	private static final String C1541_SECTION = "c1541section";
	
	@Import(module = SIDPLAY2_SECTION, name = "getPalEmulation")
	public native boolean isPalEmulation();

	@Import(module = AUDIO_SECTION, name = "getSamplingRate")
	public native int getSamplingRateAsInt();

	@Import(module = AUDIO_SECTION, name = "getSamplingMethodResample")
	public native boolean getSamplingMethodResample();

	@Import(module = AUDIO_SECTION, name = "getReverbBypass")
	public native boolean getReverbBypass();

	@Import(module = AUDIO_SECTION, name = "getBufferSize")
	public native int getBufferSize();

	@Import(module = AUDIO_SECTION, name = "getAudioBufferSize")
	public native int getAudioBufferSize();

	@Import(module = EMULATION_SECTION, name = "getDefaultSidModel8580")
	public native boolean getDefaultSidModel8580();

	@Import(module = EMULATION_SECTION, name = "getDefaultClockSpeed")
	public native int getDefaultClockSpeedAsInt();

	@Import(module = C1541_SECTION, name = "isJiffyDosInstalled")
	public native boolean isJiffyDosInstalled();
	
}
