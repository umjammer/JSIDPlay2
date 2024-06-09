package client.teavm.common.config.filter.residfp;

import libsidplay.config.IFilterSection;

public abstract class ReSIDfpFilter8580TeaVM implements IFilterSection {

	public static final class FilterTrurl8580R5_1489 extends ReSIDfpFilter8580TeaVM {

		@Override
		public String getName() {
			return "FilterTrurl8580R5_1489";
		}

		@Override
		public float getK() {
			return 5.7f;
		}

		@Override
		public float getB() {
			return 20f;
		}

		@Override
		public float getVoiceNonlinearity() {
			return 1.0f;
		}

		@Override
		public float getResonanceFactor() {
			return 1.0f;
		}
	}

	public static final class FilterTrurl8580R5_3691 extends ReSIDfpFilter8580TeaVM {

		@Override
		public String getName() {
			return "FilterTrurl8580R5_3691";
		}

		@Override
		public float getK() {
			return 6.55f;
		}

		@Override
		public float getB() {
			return 20f;
		}

		@Override
		public float getVoiceNonlinearity() {
			return 1.0f;
		}

		@Override
		public float getResonanceFactor() {
			return 1.0f;
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
	public final float getSteepness() {
		return 0f;
	}

	@Override
	public final float getOffset() {
		return 0f;
	}

	@Override
	public final float getNonlinearity() {
		return 0f;
	}

	@Override
	public final float getMinimumfetresistance() {
		return 0f;
	}

	@Override
	public final float getFilter8580CurvePosition() {
		return 0;
	}

	@Override
	public final float getFilter6581CurvePosition() {
		return 0;
	}

	@Override
	public final float getBaseresistance() {
		return 0f;
	}

	@Override
	public final float getAttenuation() {
		return 0f;
	}

	@Override
	public abstract float getResonanceFactor();

	@Override
	public abstract String getName();

	@Override
	public abstract float getK();

	@Override
	public abstract float getB();

}
