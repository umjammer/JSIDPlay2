package client.teavm.common.config;

import java.util.Arrays;
import java.util.List;

import client.teavm.common.IImportedApi;
import client.teavm.common.config.filter.resid.FilterAverage6581TeaVM;
import client.teavm.common.config.filter.resid.FilterAverage8580TeaVM;
import client.teavm.common.config.filter.resid.FilterDark6581TeaVM;
import client.teavm.common.config.filter.resid.FilterDark8580TeaVM;
import client.teavm.common.config.filter.resid.FilterDarker6581TeaVM;
import client.teavm.common.config.filter.resid.FilterDarkest6581TeaVM;
import client.teavm.common.config.filter.resid.FilterLight6581TeaVM;
import client.teavm.common.config.filter.resid.FilterLight8580TeaVM;
import client.teavm.common.config.filter.resid.FilterLighter6581TeaVM;
import client.teavm.common.config.filter.resid.FilterLightest6581TeaVM;
import client.teavm.common.config.filter.residfp.FilterAlankila6581R3_3984_1TeaVM;
import client.teavm.common.config.filter.residfp.FilterAlankila6581R3_3984_2TeaVM;
import client.teavm.common.config.filter.residfp.FilterAlankila6581R4AR_3789TeaVM;
import client.teavm.common.config.filter.residfp.FilterEnigma6581R3_1585TeaVM;
import client.teavm.common.config.filter.residfp.FilterEnigma6581R3_4885TeaVM;
import client.teavm.common.config.filter.residfp.FilterGrue6581R4AR_3488TeaVM;
import client.teavm.common.config.filter.residfp.FilterKrulloTeaVM;
import client.teavm.common.config.filter.residfp.FilterLordNightmare6581R3_4285TeaVM;
import client.teavm.common.config.filter.residfp.FilterLordNightmare6581R3_4485TeaVM;
import client.teavm.common.config.filter.residfp.FilterLordNightmare6581R4_1986STeaVM;
import client.teavm.common.config.filter.residfp.FilterNata6581R3_2083TeaVM;
import client.teavm.common.config.filter.residfp.FilterReSID6581TeaVM;
import client.teavm.common.config.filter.residfp.FilterTrurl6581R3_0486STeaVM;
import client.teavm.common.config.filter.residfp.FilterTrurl6581R3_0784TeaVM;
import client.teavm.common.config.filter.residfp.FilterTrurl6581R3_4885TeaVM;
import client.teavm.common.config.filter.residfp.FilterTrurl6581R4AR_3789TeaVM;
import client.teavm.common.config.filter.residfp.FilterTrurl6581R4AR_4486TeaVM;
import client.teavm.common.config.filter.residfp.FilterTrurl8580R5_1489TeaVM;
import client.teavm.common.config.filter.residfp.FilterTrurl8580R5_3691TeaVM;
import client.teavm.common.config.filter.residfp.FilterZrX6581R3_0384TeaVM;
import client.teavm.common.config.filter.residfp.FilterZrX6581R3_1984TeaVM;
import client.teavm.common.config.filter.residfp.FilterZrX6581R3_3684TeaVM;
import client.teavm.common.config.filter.residfp.FilterZrX6581R3_3985TeaVM;
import client.teavm.common.config.filter.residfp.FilterZrX6581R4AR_2286TeaVM;
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
 * assembly version builds. Everything is fix, except where imported API gets
 * asked for configuration values from the browser's JavaScript environment.
 */
public class ConfigurationTeaVM implements IConfig {

	private final Sidplay2SectionTeaVM sidplay2Section;
	private final C1541SectionTeaVM c1541Section;
	private final PrinterSectionTeaVM printerSection;
	private final AudioSectionTeaVM audioSection;
	private final EmulationSectionTeaVM emulationSection;
	private final WhatsSidSectionTeaVM whatsSidSection;
	private final List<IFilterSection> filterSections = Arrays.asList(new FilterLight8580TeaVM(),
			new FilterAverage8580TeaVM(), new FilterDark8580TeaVM(), new FilterLightest6581TeaVM(),
			new FilterLighter6581TeaVM(), new FilterLight6581TeaVM(), new FilterAverage6581TeaVM(),
			new FilterDark6581TeaVM(), new FilterDarker6581TeaVM(), new FilterDarkest6581TeaVM(),
			new FilterTrurl8580R5_1489TeaVM(), new FilterTrurl8580R5_3691TeaVM(), new FilterReSID6581TeaVM(),
			new FilterAlankila6581R4AR_3789TeaVM(), new FilterAlankila6581R3_3984_1TeaVM(),
			new FilterAlankila6581R3_3984_2TeaVM(), new FilterLordNightmare6581R3_4285TeaVM(),
			new FilterLordNightmare6581R3_4485TeaVM(), new FilterLordNightmare6581R4_1986STeaVM(),
			new FilterZrX6581R3_0384TeaVM(), new FilterZrX6581R3_1984TeaVM(), new FilterZrX6581R3_3684TeaVM(),
			new FilterZrX6581R3_3985TeaVM(), new FilterZrX6581R4AR_2286TeaVM(), new FilterTrurl6581R3_0784TeaVM(),
			new FilterTrurl6581R3_0486STeaVM(), new FilterTrurl6581R3_0486STeaVM(), new FilterTrurl6581R3_4885TeaVM(),
			new FilterTrurl6581R4AR_3789TeaVM(), new FilterTrurl6581R4AR_4486TeaVM(), new FilterNata6581R3_2083TeaVM(),
			new FilterGrue6581R4AR_3488TeaVM(), new FilterKrulloTeaVM(), new FilterEnigma6581R3_4885TeaVM(),
			new FilterEnigma6581R3_1585TeaVM());

	public ConfigurationTeaVM(IImportedApi importedApi) {
		sidplay2Section = new Sidplay2SectionTeaVM(importedApi);
		c1541Section = new C1541SectionTeaVM(importedApi);
		printerSection = new PrinterSectionTeaVM();
		audioSection = new AudioSectionTeaVM(importedApi);
		emulationSection = new EmulationSectionTeaVM(importedApi);
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
