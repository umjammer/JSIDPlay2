package client.teavm.common.config.filter.resid;

import libsidplay.config.IFilterSection;

public abstract class ReSIDFilter8580TeaVM implements IFilterSection {

	public static final class FilterLight8580 extends ReSIDFilter8580TeaVM {

		@Override
		public String getName() {
			return "FilterLight8580";
		}

		@Override
		public float getFilter8580CurvePosition() {
			return 13400f;
		}
	}

	public static final class FilterAverage8580 extends ReSIDFilter8580TeaVM {

		@Override
		public String getName() {
			return "FilterAverage8580";
		}

		@Override
		public float getFilter8580CurvePosition() {
			return 12500f;
		}
	}

	public static final class FilterDark8580 extends ReSIDFilter8580TeaVM {

		@Override
		public String getName() {
			return "FilterDark8580";
		}

		@Override
		public float getFilter8580CurvePosition() {
			return 11700f;
		}
	}

	@Override
	public final void setVoiceNonlinearity(float voiceNonlinearity) {
	}

	@Override
	public final void setSteepness(float steepness) {
	}

	@Override
	public final void setResonanceFactor(float resonanceFactor) {
	}

	@Override
	public final void setOffset(float offset) {
	}

	@Override
	public final void setNonlinearity(float nonlinearity) {
	}

	@Override
	public final void setName(String name) {
	}

	@Override
	public final void setMinimumfetresistance(float minimumfetresistance) {
	}

	@Override
	public final void setK(float k) {
	}

	@Override
	public final void setFilter8580CurvePosition(float filter8580CurvePosition) {
	}

	@Override
	public final void setFilter6581CurvePosition(float filter6581CurvePosition) {
	}

	@Override
	public final void setBaseresistance(float baseresistance) {
	}

	@Override
	public final void setB(float b) {
	}

	@Override
	public final void setAttenuation(float attenuation) {
	}

	@Override
	public final float getVoiceNonlinearity() {
		return 0;
	}

	@Override
	public final float getSteepness() {
		return 0;
	}

	@Override
	public final float getResonanceFactor() {
		return 0;
	}

	@Override
	public final float getOffset() {
		return 0;
	}

	@Override
	public final float getNonlinearity() {
		return 0;
	}

	@Override
	public final float getMinimumfetresistance() {
		return 0;
	}

	@Override
	public final float getK() {
		return 0;
	}

	@Override
	public final float getFilter6581CurvePosition() {
		return 0;
	}

	@Override
	public final float getBaseresistance() {
		return 0;
	}

	@Override
	public final float getB() {
		return 0;
	}

	@Override
	public final float getAttenuation() {
		return 0;
	}

	@Override
	public abstract String getName();

	@Override
	public abstract float getFilter8580CurvePosition();

}
