package client.teavm.config;

import java.util.Collections;
import java.util.List;

import client.teavm.IConfigResolverTeaVM;
import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Engine;
import libsidplay.common.SidReads;
import libsidplay.common.Ultimate64Mode;
import libsidplay.config.IDeviceMapping;
import libsidplay.config.IEmulationSection;

public final class EmulationSectionTeaVM implements IEmulationSection {

	private final OverrideSection overrideSection = new OverrideSection();

	private IConfigResolverTeaVM resolver;

	public EmulationSectionTeaVM(IConfigResolverTeaVM resolver) {
		this.resolver = resolver;
	}

	@Override
	public void setUserSidModel(ChipModel model) {
	}

	@Override
	public void setUserEmulation(Emulation emulation) {
	}

	@Override
	public void setUserClockSpeed(CPUClock speed) {
	}

	@Override
	public void setUltimate64SyncDelay(int syncDelay) {
	}

	@Override
	public void setUltimate64Port(int port) {
	}

	@Override
	public void setUltimate64Mode(Ultimate64Mode ultimate64Mode) {
	}

	@Override
	public void setUltimate64Host(String hostname) {
	}

	@Override
	public void setThirdSIDModel(ChipModel model) {
	}

	@Override
	public void setThirdSIDFilter8580(String filterName) {
	}

	@Override
	public void setThirdSIDFilter6581(String filterName) {
	}

	@Override
	public void setThirdSIDFilter(boolean enable) {
	}

	@Override
	public void setThirdSIDBase(int base) {
	}

	@Override
	public void setThirdEmulation(Emulation emulation) {
	}

	@Override
	public void setStereoSidModel(ChipModel model) {
	}

	@Override
	public void setStereoFilter8580(String filterName) {
	}

	@Override
	public void setStereoFilter6581(String filterName) {
	}

	@Override
	public void setStereoFilter(boolean enable) {
	}

	@Override
	public void setStereoEmulation(Emulation emulation) {
	}

	@Override
	public void setSidToRead(SidReads sidRead) {
	}

	@Override
	public void setSidBlasterWriteBufferSize(int sidBlasterWriteBufferSize) {
	}

	@Override
	public void setSidBlasterSerialNumber(String sidBlasterSerialNumber) {
	}

	@Override
	public void setSidBlasterRead(boolean sidBlasterRead) {
	}

	@Override
	public void setSidBlasterLatencyTimer(short sidBlasterLatencyTimer) {
	}

	@Override
	public void setReSIDfpThirdSIDFilter8580(String filterName) {
	}

	@Override
	public void setReSIDfpThirdSIDFilter6581(String filterName) {
	}

	@Override
	public void setReSIDfpStereoFilter8580(String filterName) {
	}

	@Override
	public void setReSIDfpStereoFilter6581(String filterName) {
	}

	@Override
	public void setReSIDfpFilter8580(String filterName) {
	}

	@Override
	public void setReSIDfpFilter6581(String filterName) {
	}

	@Override
	public void setNetSIDThirdSIDFilter8580(String filterName) {
	}

	@Override
	public void setNetSIDThirdSIDFilter6581(String filterName) {
	}

	@Override
	public void setNetSIDStereoFilter8580(String filterName) {
	}

	@Override
	public void setNetSIDStereoFilter6581(String filterName) {
	}

	@Override
	public void setNetSIDFilter8580(String filterName) {
	}

	@Override
	public void setNetSIDFilter6581(String filterName) {
	}

	@Override
	public void setNetSIDDevPort(int port) {
	}

	@Override
	public void setNetSIDDevHost(String hostname) {
	}

	@Override
	public void setMuteVoice4(boolean mute) {
	}

	@Override
	public void setMuteVoice3(boolean mute) {
	}

	@Override
	public void setMuteVoice2(boolean mute) {
	}

	@Override
	public void setMuteVoice1(boolean mute) {
	}

	@Override
	public void setMuteThirdSIDVoice4(boolean mute) {
	}

	@Override
	public void setMuteThirdSIDVoice3(boolean mute) {
	}

	@Override
	public void setMuteThirdSIDVoice2(boolean mute) {
	}

	@Override
	public void setMuteThirdSIDVoice1(boolean mute) {
	}

	@Override
	public void setMuteStereoVoice4(boolean mute) {
	}

	@Override
	public void setMuteStereoVoice3(boolean mute) {
	}

	@Override
	public void setMuteStereoVoice2(boolean mute) {
	}

	@Override
	public void setMuteStereoVoice1(boolean mute) {
	}

	@Override
	public void setHardsid8580(int chip) {
	}

	@Override
	public void setHardsid6581(int chip) {
	}

	@Override
	public void setForceStereoTune(boolean force) {
	}

	@Override
	public void setForce3SIDTune(boolean force) {
	}

	@Override
	public void setFilter8580(String filterName) {
	}

	@Override
	public void setFilter6581(String filterName) {
	}

	@Override
	public void setFilter(boolean enable) {
	}

	@Override
	public void setFakeStereo(boolean fakeStereo) {
	}

	@Override
	public void setExsidFakeStereo(boolean exsidFakeStereo) {
	}

	@Override
	public void setEngine(Engine engine) {
	}

	@Override
	public void setDualSidBase(int base) {
	}

	@Override
	public void setDigiBoosted8580(boolean boost) {
	}

	@Override
	public void setDetectPSID64ChipModel(boolean detectPSID64ChipModel) {
	}

	@Override
	public void setDefaultSidModel(ChipModel model) {
	}

	@Override
	public void setDefaultEmulation(Emulation emulation) {
	}

	@Override
	public void setDefaultClockSpeed(CPUClock speed) {
	}

	@Override
	public boolean isThirdSIDFilter() {
		return true;
	}

	@Override
	public boolean isStereoFilter() {
		return true;
	}

	@Override
	public boolean isSidBlasterRead() {
		return false;
	}

	@Override
	public boolean isMuteVoice4() {
		return false;
	}

	@Override
	public boolean isMuteVoice3() {
		return false;
	}

	@Override
	public boolean isMuteVoice2() {
		return false;
	}

	@Override
	public boolean isMuteVoice1() {
		return false;
	}

	@Override
	public boolean isMuteThirdSIDVoice4() {
		return false;
	}

	@Override
	public boolean isMuteThirdSIDVoice3() {
		return false;
	}

	@Override
	public boolean isMuteThirdSIDVoice2() {
		return false;
	}

	@Override
	public boolean isMuteThirdSIDVoice1() {
		return false;
	}

	@Override
	public boolean isMuteStereoVoice4() {
		return false;
	}

	@Override
	public boolean isMuteStereoVoice3() {
		return false;
	}

	@Override
	public boolean isMuteStereoVoice2() {
		return false;
	}

	@Override
	public boolean isMuteStereoVoice1() {
		return false;
	}

	@Override
	public boolean isForceStereoTune() {
		return false;
	}

	@Override
	public boolean isForce3SIDTune() {
		return false;
	}

	@Override
	public boolean isFilter() {
		return true;
	}

	@Override
	public boolean isFakeStereo() {
		return false;
	}

	@Override
	public boolean isExsidFakeStereo() {
		return false;
	}

	@Override
	public boolean isDigiBoosted8580() {
		return false;
	}

	@Override
	public boolean isDetectPSID64ChipModel() {
		return true;
	}

	@Override
	public ChipModel getUserSidModel() {
		return ChipModel.AUTO;
	}

	@Override
	public Emulation getUserEmulation() {
		return Emulation.DEFAULT;
	}

	@Override
	public CPUClock getUserClockSpeed() {
		return CPUClock.AUTO;
	}

	@Override
	public int getUltimate64SyncDelay() {
		return 0;
	}

	@Override
	public int getUltimate64Port() {
		return 0;
	}

	@Override
	public Ultimate64Mode getUltimate64Mode() {
		return null;
	}

	@Override
	public String getUltimate64Host() {
		return null;
	}

	@Override
	public ChipModel getThirdSIDModel() {
		return ChipModel.AUTO;
	}

	@Override
	public String getThirdSIDFilter8580() {
		return "FilterAverage8580";
	}

	@Override
	public String getThirdSIDFilter6581() {
		return "FilterAverage6581";
	}

	@Override
	public int getThirdSIDBase() {
		return 0xd440;
	}

	@Override
	public Emulation getThirdEmulation() {
		return Emulation.DEFAULT;
	}

	@Override
	public ChipModel getStereoSidModel() {
		return ChipModel.AUTO;
	}

	@Override
	public String getStereoFilter8580() {
		return "FilterAverage8580";
	}

	@Override
	public String getStereoFilter6581() {
		return "FilterAverage6581";
	}

	@Override
	public Emulation getStereoEmulation() {
		return Emulation.DEFAULT;
	}

	@Override
	public SidReads getSidToRead() {
		return SidReads.FIRST_SID;
	}

	@Override
	public int getSidBlasterWriteBufferSize() {
		return 0;
	}

	@Override
	public String getSidBlasterSerialNumber() {
		return null;
	}

	@Override
	public short getSidBlasterLatencyTimer() {
		return 0;
	}

	@Override
	public List<? extends IDeviceMapping> getSidBlasterDeviceList() {
		return Collections.emptyList();
	}

	@Override
	public String getReSIDfpThirdSIDFilter8580() {
		return "FilterTrurl8580R5_3691";
	}

	@Override
	public String getReSIDfpThirdSIDFilter6581() {
		return "FilterAlankila6581R4AR_3789";
	}

	@Override
	public String getReSIDfpStereoFilter8580() {
		return "FilterTrurl8580R5_3691";
	}

	@Override
	public String getReSIDfpStereoFilter6581() {
		return "FilterAlankila6581R4AR_3789";
	}

	@Override
	public String getReSIDfpFilter8580() {
		return "FilterTrurl8580R5_3691";
	}

	@Override
	public String getReSIDfpFilter6581() {
		return "FilterAlankila6581R4AR_3789";
	}

	@Override
	public OverrideSection getOverrideSection() {
		return overrideSection;
	}

	@Override
	public String getNetSIDThirdSIDFilter8580() {
		return null;
	}

	@Override
	public String getNetSIDThirdSIDFilter6581() {
		return null;
	}

	@Override
	public String getNetSIDStereoFilter8580() {
		return null;
	}

	@Override
	public String getNetSIDStereoFilter6581() {
		return null;
	}

	@Override
	public String getNetSIDFilter8580() {
		return null;
	}

	@Override
	public String getNetSIDFilter6581() {
		return null;
	}

	@Override
	public int getNetSIDDevPort() {
		return 0;
	}

	@Override
	public String getNetSIDDevHost() {
		return null;
	}

	@Override
	public int getHardsid8580() {
		return 0;
	}

	@Override
	public int getHardsid6581() {
		return 0;
	}

	@Override
	public String getFilter8580() {
		return "FilterAverage8580";
	}

	@Override
	public String getFilter6581() {
		return "FilterAverage6581";
	}

	@Override
	public Engine getEngine() {
		return Engine.EMULATION;
	}

	@Override
	public int getDualSidBase() {
		return 0xd420;
	}

	@Override
	public ChipModel getDefaultSidModel() {
		if (getDefaultSidModel8580()) {
			return ChipModel.MOS8580;
		} else {
			return ChipModel.MOS6581;
		}
	}

	public boolean getDefaultSidModel8580() {
		return resolver.getDefaultSidModel8580();
	}

	@Override
	public Emulation getDefaultEmulation() {
		return Emulation.RESID;
	}

	@Override
	public CPUClock getDefaultClockSpeed() {
		switch (getDefaultClockSpeedAsInt()) {
		case 50:
		default:
			return CPUClock.PAL;
		case 60:
			return CPUClock.NTSC;
		}
	}

	public int getDefaultClockSpeedAsInt() {
		return resolver.getDefaultClockSpeedAsInt();
	}
}