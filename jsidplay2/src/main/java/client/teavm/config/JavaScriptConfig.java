package client.teavm.config;

import java.util.ArrayList;
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

	private JavaScriptSidplay2Section sidplay2Section = new JavaScriptSidplay2Section();
	private JavaScriptC1541Section c1541Section = new JavaScriptC1541Section();
	private JavaScriptPrinterSection printerSection = new JavaScriptPrinterSection();
	private JavaScriptAudioSection audioSection = new JavaScriptAudioSection();
	private JavaScriptEmulationSection emulationSection = new JavaScriptEmulationSection();
	private JavaScriptWhatsSidSection whatsSidSection = new JavaScriptWhatsSidSection();

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
		ArrayList<IFilterSection> arrayList = new ArrayList<IFilterSection>();
		IFilterSection fs1 = new IFilterSection() {

			@Override
			public void setVoiceNonlinearity(float voiceNonlinearity) {
			}

			@Override
			public void setSteepness(float steepness) {
			}

			@Override
			public void setResonanceFactor(float resonanceFactor) {
			}

			@Override
			public void setOffset(float offset) {
			}

			@Override
			public void setNonlinearity(float nonlinearity) {
			}

			@Override
			public void setName(String name) {
			}

			@Override
			public void setMinimumfetresistance(float minimumfetresistance) {
			}

			@Override
			public void setK(float k) {
			}

			@Override
			public void setFilter8580CurvePosition(float filter8580CurvePosition) {
			}

			@Override
			public void setFilter6581CurvePosition(float filter6581CurvePosition) {
			}

			@Override
			public void setBaseresistance(float baseresistance) {
			}

			@Override
			public void setB(float b) {
			}

			@Override
			public void setAttenuation(float attenuation) {
			}

			@Override
			public float getVoiceNonlinearity() {
				return 0;
			}

			@Override
			public float getSteepness() {
				return 0;
			}

			@Override
			public float getResonanceFactor() {
				return 0;
			}

			@Override
			public float getOffset() {
				return 0;
			}

			@Override
			public float getNonlinearity() {
				return 0;
			}

			@Override
			public String getName() {
				return "FilterAverage6581";
			}

			@Override
			public float getMinimumfetresistance() {
				return 0;
			}

			@Override
			public float getK() {
				return 0;
			}

			@Override
			public float getFilter8580CurvePosition() {
				return 0;
			}

			@Override
			public float getFilter6581CurvePosition() {
				return 0.5f;
			}

			@Override
			public float getBaseresistance() {
				return 0;
			}

			@Override
			public float getB() {
				return 0;
			}

			@Override
			public float getAttenuation() {
				return 0;
			}
		};
		arrayList.add(fs1);
		IFilterSection fs2 = new IFilterSection() {

			@Override
			public void setVoiceNonlinearity(float voiceNonlinearity) {
			}

			@Override
			public void setSteepness(float steepness) {
			}

			@Override
			public void setResonanceFactor(float resonanceFactor) {
			}

			@Override
			public void setOffset(float offset) {
			}

			@Override
			public void setNonlinearity(float nonlinearity) {
			}

			@Override
			public void setName(String name) {
			}

			@Override
			public void setMinimumfetresistance(float minimumfetresistance) {
			}

			@Override
			public void setK(float k) {
			}

			@Override
			public void setFilter8580CurvePosition(float filter8580CurvePosition) {
			}

			@Override
			public void setFilter6581CurvePosition(float filter6581CurvePosition) {
			}

			@Override
			public void setBaseresistance(float baseresistance) {
			}

			@Override
			public void setB(float b) {
			}

			@Override
			public void setAttenuation(float attenuation) {
			}

			@Override
			public float getVoiceNonlinearity() {
				return 0;
			}

			@Override
			public float getSteepness() {
				return 0;
			}

			@Override
			public float getResonanceFactor() {
				return 0;
			}

			@Override
			public float getOffset() {
				return 0;
			}

			@Override
			public float getNonlinearity() {
				return 0;
			}

			@Override
			public String getName() {
				return "FilterAverage8580";
			}

			@Override
			public float getMinimumfetresistance() {
				return 0;
			}

			@Override
			public float getK() {
				return 0;
			}

			@Override
			public float getFilter8580CurvePosition() {
				return 12500;
			}

			@Override
			public float getFilter6581CurvePosition() {
				return 0;
			}

			@Override
			public float getBaseresistance() {
				return 0;
			}

			@Override
			public float getB() {
				return 0;
			}

			@Override
			public float getAttenuation() {
				return 0;
			}
		};
		arrayList.add(fs2);
		IFilterSection fs3 = new IFilterSection() {

			@Override
			public void setVoiceNonlinearity(float voiceNonlinearity) {
			}

			@Override
			public void setSteepness(float steepness) {
			}

			@Override
			public void setResonanceFactor(float resonanceFactor) {
			}

			@Override
			public void setOffset(float offset) {
			}

			@Override
			public void setNonlinearity(float nonlinearity) {
			}

			@Override
			public void setName(String name) {
			}

			@Override
			public void setMinimumfetresistance(float minimumfetresistance) {
			}

			@Override
			public void setK(float k) {
			}

			@Override
			public void setFilter8580CurvePosition(float filter8580CurvePosition) {
			}

			@Override
			public void setFilter6581CurvePosition(float filter6581CurvePosition) {
			}

			@Override
			public void setBaseresistance(float baseresistance) {
			}

			@Override
			public void setB(float b) {
			}

			@Override
			public void setAttenuation(float attenuation) {
			}

			@Override
			public float getVoiceNonlinearity() {
				return 03.3e6f;
			}

			@Override
			public float getSteepness() {
				return 1.0066634233403395f;
			}

			@Override
			public float getResonanceFactor() {
				return 1.0f;
			}

			@Override
			public float getOffset() {
				return 274228796.97550374f;
			}

			@Override
			public float getNonlinearity() {
				return 0.9613160610660189f;
			}

			@Override
			public String getName() {
				return "FilterAlankila6581R4AR_3789";
			}

			@Override
			public float getMinimumfetresistance() {
				return 16125.154840564108f;
			}

			@Override
			public float getK() {
				return 0;
			}

			@Override
			public float getFilter8580CurvePosition() {
				return 0;
			}

			@Override
			public float getFilter6581CurvePosition() {
				return 0;
			}

			@Override
			public float getBaseresistance() {
				return 1147036.4394268463f;
			}

			@Override
			public float getB() {
				return 0;
			}

			@Override
			public float getAttenuation() {
				return 0.5f;
			}
		};
		arrayList.add(fs3);
		IFilterSection fs4 = new IFilterSection() {

			@Override
			public void setVoiceNonlinearity(float voiceNonlinearity) {
			}

			@Override
			public void setSteepness(float steepness) {
			}

			@Override
			public void setResonanceFactor(float resonanceFactor) {
			}

			@Override
			public void setOffset(float offset) {
			}

			@Override
			public void setNonlinearity(float nonlinearity) {
			}

			@Override
			public void setName(String name) {
			}

			@Override
			public void setMinimumfetresistance(float minimumfetresistance) {
			}

			@Override
			public void setK(float k) {
			}

			@Override
			public void setFilter8580CurvePosition(float filter8580CurvePosition) {
			}

			@Override
			public void setFilter6581CurvePosition(float filter6581CurvePosition) {
			}

			@Override
			public void setBaseresistance(float baseresistance) {
			}

			@Override
			public void setB(float b) {
			}

			@Override
			public void setAttenuation(float attenuation) {
			}

			@Override
			public float getVoiceNonlinearity() {
				return 0;
			}

			@Override
			public float getSteepness() {
				return 0;
			}

			@Override
			public float getResonanceFactor() {
				return 1.0f;
			}

			@Override
			public float getOffset() {
				return 0;
			}

			@Override
			public float getNonlinearity() {
				return 1.0f;
			}

			@Override
			public String getName() {
				return "FilterTrurl8580R5_3691";
			}

			@Override
			public float getMinimumfetresistance() {
				return 0;
			}

			@Override
			public float getK() {
				return 6.55f;
			}

			@Override
			public float getFilter8580CurvePosition() {
				return 0;
			}

			@Override
			public float getFilter6581CurvePosition() {
				return 0;
			}

			@Override
			public float getBaseresistance() {
				return 0;
			}

			@Override
			public float getB() {
				return 20;
			}

			@Override
			public float getAttenuation() {
				return 0;
			}
		};
		arrayList.add(fs4);
		return arrayList;
	}

}
