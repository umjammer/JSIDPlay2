package client.teavm.config;

import java.util.Arrays;
import java.util.List;

import client.teavm.IConfigResolver;
import libsidplay.config.IAudioSection;
import libsidplay.config.IC1541Section;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;
import libsidplay.config.IFilterSection;
import libsidplay.config.IPrinterSection;
import libsidplay.config.ISidPlay2Section;
import libsidplay.config.IWhatsSidSection;

public class JavaScriptConfig implements IConfig {

	private final JavaScriptSidplay2Section sidplay2Section;
	private final JavaScriptC1541Section c1541Section;
	private final JavaScriptPrinterSection printerSection;
	private final JavaScriptAudioSection audioSection;
	private final JavaScriptEmulationSection emulationSection;
	private final JavaScriptWhatsSidSection whatsSidSection;
	private final List<IFilterSection> filterSections = Arrays.asList(new JavaScriptFilterAlankila6581R4AR_3789(),
			new JavaScriptFilterTrurl8580R5_3691(), new JavaScriptFilterAverage6581(), new JavaScriptFilterAverage8580());

	public JavaScriptConfig(IConfigResolver resolver) {
		sidplay2Section = new JavaScriptSidplay2Section(resolver);
		c1541Section = new JavaScriptC1541Section(resolver);
		printerSection = new JavaScriptPrinterSection();
		audioSection = new JavaScriptAudioSection(resolver);
		emulationSection = new JavaScriptEmulationSection(resolver);
		whatsSidSection = new JavaScriptWhatsSidSection();
	}
	
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
