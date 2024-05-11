package client.teavm.config;

import java.util.Arrays;
import java.util.List;

import libsidplay.config.IAudioSection;
import libsidplay.config.IC1541Section;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;
import libsidplay.config.IFilterSection;
import libsidplay.config.IPrinterSection;
import libsidplay.config.ISidPlay2Section;
import libsidplay.config.IWhatsSidSection;

public class JavaScriptConfig implements IConfig {

	private final JavaScriptSidplay2Section sidplay2Section = new JavaScriptSidplay2Section();
	private final JavaScriptC1541Section c1541Section = new JavaScriptC1541Section();
	private final JavaScriptPrinterSection printerSection = new JavaScriptPrinterSection();
	private final JavaScriptAudioSection audioSection = new JavaScriptAudioSection();
	private final JavaScriptEmulationSection emulationSection = new JavaScriptEmulationSection();
	private final JavaScriptWhatsSidSection whatsSidSection = new JavaScriptWhatsSidSection();
	private final List<IFilterSection> filterSections = Arrays.asList(new JavaScriptFilterAlankila6581R4AR_3789(),
			new JavaScriptFilterTrurl8580R5_3691(), new JavaScriptFilterAverage6581(), new JavaScriptFilterAverage8580());

	@Override
	public ISidPlay2Section getSidplay2Section() {
		return sidplay2Section;
	}

	@Override
	public IC1541Section getC1541Section() {
		return c1541Section;
	}

	@Override
	public IPrinterSection getPrinterSection() {
		return printerSection;
	}

	@Override
	public IAudioSection getAudioSection() {
		return audioSection;
	}

	@Override
	public IEmulationSection getEmulationSection() {
		return emulationSection;
	}

	@Override
	public IWhatsSidSection getWhatsSidSection() {
		return whatsSidSection;
	}

	@Override
	public List<? extends IFilterSection> getFilterSection() {
		return filterSections;
	}

}
