package client.teavm.common.config;

import java.util.Arrays;
import java.util.List;

import client.teavm.common.IImportedApi;
import client.teavm.common.config.filter.resid.ReSIDFilter6581TeaVM.FilterAverage6581;
import client.teavm.common.config.filter.resid.ReSIDFilter6581TeaVM.FilterDark6581;
import client.teavm.common.config.filter.resid.ReSIDFilter6581TeaVM.FilterDarker6581;
import client.teavm.common.config.filter.resid.ReSIDFilter6581TeaVM.FilterDarkest6581;
import client.teavm.common.config.filter.resid.ReSIDFilter6581TeaVM.FilterLight6581;
import client.teavm.common.config.filter.resid.ReSIDFilter6581TeaVM.FilterLighter6581;
import client.teavm.common.config.filter.resid.ReSIDFilter6581TeaVM.FilterLightest6581;
import client.teavm.common.config.filter.resid.ReSIDFilter8580TeaVM.FilterAverage8580;
import client.teavm.common.config.filter.resid.ReSIDFilter8580TeaVM.FilterDark8580;
import client.teavm.common.config.filter.resid.ReSIDFilter8580TeaVM.FilterLight8580;
import client.teavm.common.config.filter.residfp.ReSIDfpFilter6581TeaVM.FilterAlankila6581R3_3984_1;
import client.teavm.common.config.filter.residfp.ReSIDfpFilter6581TeaVM.FilterAlankila6581R3_3984_2;
import client.teavm.common.config.filter.residfp.ReSIDfpFilter6581TeaVM.FilterAlankila6581R4AR_3789;
import client.teavm.common.config.filter.residfp.ReSIDfpFilter6581TeaVM.FilterEnigma6581R3_1585;
import client.teavm.common.config.filter.residfp.ReSIDfpFilter6581TeaVM.FilterEnigma6581R3_4885;
import client.teavm.common.config.filter.residfp.ReSIDfpFilter6581TeaVM.FilterGrue6581R4AR_3488;
import client.teavm.common.config.filter.residfp.ReSIDfpFilter6581TeaVM.FilterKrullo;
import client.teavm.common.config.filter.residfp.ReSIDfpFilter6581TeaVM.FilterLordNightmare6581R3_4285;
import client.teavm.common.config.filter.residfp.ReSIDfpFilter6581TeaVM.FilterLordNightmare6581R3_4485;
import client.teavm.common.config.filter.residfp.ReSIDfpFilter6581TeaVM.FilterLordNightmare6581R4_1986S;
import client.teavm.common.config.filter.residfp.ReSIDfpFilter6581TeaVM.FilterNata6581R3_2083;
import client.teavm.common.config.filter.residfp.ReSIDfpFilter6581TeaVM.FilterReSID6581;
import client.teavm.common.config.filter.residfp.ReSIDfpFilter6581TeaVM.FilterTrurl6581R3_0486S;
import client.teavm.common.config.filter.residfp.ReSIDfpFilter6581TeaVM.FilterTrurl6581R3_0784;
import client.teavm.common.config.filter.residfp.ReSIDfpFilter6581TeaVM.FilterTrurl6581R3_3384;
import client.teavm.common.config.filter.residfp.ReSIDfpFilter6581TeaVM.FilterTrurl6581R3_4885;
import client.teavm.common.config.filter.residfp.ReSIDfpFilter6581TeaVM.FilterTrurl6581R4AR_3789;
import client.teavm.common.config.filter.residfp.ReSIDfpFilter6581TeaVM.FilterTrurl6581R4AR_4486;
import client.teavm.common.config.filter.residfp.ReSIDfpFilter6581TeaVM.FilterZrX6581R3_0384;
import client.teavm.common.config.filter.residfp.ReSIDfpFilter6581TeaVM.FilterZrX6581R3_1984;
import client.teavm.common.config.filter.residfp.ReSIDfpFilter6581TeaVM.FilterZrX6581R3_3684;
import client.teavm.common.config.filter.residfp.ReSIDfpFilter6581TeaVM.FilterZrX6581R3_3985;
import client.teavm.common.config.filter.residfp.ReSIDfpFilter6581TeaVM.FilterZrX6581R4AR_2286;
import client.teavm.common.config.filter.residfp.ReSIDfpFilter8580TeaVM.FilterTrurl8580R5_1489;
import client.teavm.common.config.filter.residfp.ReSIDfpFilter8580TeaVM.FilterTrurl8580R5_3691;
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
	private final List<IFilterSection> filterSections = Arrays.asList(new FilterLight8580(), new FilterAverage8580(),
			new FilterDark8580(), new FilterLightest6581(), new FilterLighter6581(), new FilterLight6581(),
			new FilterAverage6581(), new FilterDark6581(), new FilterDarker6581(), new FilterDarkest6581(),
			new FilterTrurl8580R5_1489(), new FilterTrurl8580R5_3691(), new FilterReSID6581(),
			new FilterAlankila6581R4AR_3789(), new FilterAlankila6581R3_3984_1(), new FilterAlankila6581R3_3984_2(),
			new FilterLordNightmare6581R3_4285(), new FilterLordNightmare6581R3_4485(),
			new FilterLordNightmare6581R4_1986S(), new FilterZrX6581R3_0384(), new FilterZrX6581R3_1984(),
			new FilterZrX6581R3_3684(), new FilterZrX6581R3_3985(), new FilterZrX6581R4AR_2286(),
			new FilterTrurl6581R3_0784(), new FilterTrurl6581R3_0486S(), new FilterTrurl6581R3_0486S(),
			new FilterTrurl6581R3_4885(), new FilterTrurl6581R3_3384(), new FilterTrurl6581R4AR_3789(),
			new FilterTrurl6581R4AR_4486(), new FilterNata6581R3_2083(), new FilterGrue6581R4AR_3488(),
			new FilterKrullo(), new FilterEnigma6581R3_4885(), new FilterEnigma6581R3_1585());

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
