package client.teavm.wasm.config;

import org.teavm.interop.Import;

import client.teavm.common.config.IConfigResolverTeaVM;

/**
 * Web assembly version's configuration is handled reading methods from
 * JavaScript modules corresponding their INI section names.
 */
public class WebAssemblyConfigResolver implements IConfigResolverTeaVM {

	private static final String SIDPLAY2_SECTION = "sidplay2section";

	private static final String AUDIO_SECTION = "audiosection";

	private static final String EMULATION_SECTION = "emulationsection";

	private static final String C1541_SECTION = "c1541section";

	@Override
	public boolean isPalEmulation() {
		return WebAssemblyConfigResolver.isPalEmulationJS();
	}

	@Override
	public int getSamplingRateAsInt() {
		return WebAssemblyConfigResolver.getSamplingRateAsIntJS();
	}

	@Override
	public boolean getSamplingMethodResample() {
		return WebAssemblyConfigResolver.getSamplingMethodResampleJS();
	}

	@Override
	public boolean getReverbBypass() {
		return WebAssemblyConfigResolver.getReverbBypassJS();
	}

	@Override
	public int getBufferSize() {
		return WebAssemblyConfigResolver.getBufferSizeJS();
	}

	@Override
	public int getAudioBufferSize() {
		return WebAssemblyConfigResolver.getAudioBufferSizeJS();
	}

	@Override
	public boolean getDefaultSidModel8580() {
		return WebAssemblyConfigResolver.getDefaultSidModel8580JS();
	}

	@Override
	public int getDefaultClockSpeedAsInt() {
		return WebAssemblyConfigResolver.getDefaultClockSpeedAsIntJS();
	}

	@Override
	public boolean isJiffyDosInstalled() {
		return WebAssemblyConfigResolver.isJiffyDosInstalledJS();
	}

	@Override
	public boolean getDefaultEmulationReSid() {
		return WebAssemblyConfigResolver.getDefaultEmulationReSidJS();
	}

	/* This methods maps to a JavaScript methods in a web page. */
	@Import(module = SIDPLAY2_SECTION, name = "getPalEmulation")
	public static native boolean isPalEmulationJS();

	@Import(module = AUDIO_SECTION, name = "getSamplingRate")
	public static native int getSamplingRateAsIntJS();

	@Import(module = AUDIO_SECTION, name = "getSamplingMethodResample")
	public static native boolean getSamplingMethodResampleJS();

	@Import(module = AUDIO_SECTION, name = "getReverbBypass")
	public static native boolean getReverbBypassJS();

	@Import(module = AUDIO_SECTION, name = "getBufferSize")
	public static native int getBufferSizeJS();

	@Import(module = AUDIO_SECTION, name = "getAudioBufferSize")
	public static native int getAudioBufferSizeJS();

	@Import(module = EMULATION_SECTION, name = "getDefaultEmulationReSid")
	public static native boolean getDefaultEmulationReSidJS();

	@Import(module = EMULATION_SECTION, name = "getDefaultSidModel8580")
	public static native boolean getDefaultSidModel8580JS();

	@Import(module = EMULATION_SECTION, name = "getDefaultClockSpeed")
	public static native int getDefaultClockSpeedAsIntJS();

	@Import(module = C1541_SECTION, name = "isJiffyDosInstalled")
	public static native boolean isJiffyDosInstalledJS();

}
