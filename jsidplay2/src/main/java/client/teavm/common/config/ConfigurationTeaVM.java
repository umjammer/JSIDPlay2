package client.teavm.common.config;

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

/**
 * Default configuration to be included and used in the JavaScript and web
 * assembly version builds. Everything is fix, except where a resolver gets
 * asked for configuration values from the browser's JavaScript environment.
 */
public class ConfigurationTeaVM implements IConfig {

	private final Sidplay2SectionTeaVM sidplay2Section;
	private final C1541SectionTeaVM c1541Section;
	private final PrinterSectionTeaVM printerSection;
	private final AudioSectionTeaVM audioSection;
	private final EmulationSectionTeaVM emulationSection;
	private final WhatsSidSectionTeaVM whatsSidSection;
	private final List<IFilterSection> filterSections = Arrays.asList(new FilterAlankila6581R4AR_3789TeaVM(),
			new FilterTrurl8580R5_3691TeaVM(), new FilterAverage6581TeaVM(), new FilterAverage8580TeaVM());

	public ConfigurationTeaVM(IConfigResolverTeaVM resolver) {
		sidplay2Section = new Sidplay2SectionTeaVM(resolver);
		c1541Section = new C1541SectionTeaVM(resolver);
		printerSection = new PrinterSectionTeaVM();
		audioSection = new AudioSectionTeaVM(resolver);
		emulationSection = new EmulationSectionTeaVM(resolver);
		whatsSidSection = new WhatsSidSectionTeaVM();
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
