package builder.resid.resample;

/**
 * Compose a more efficient SINC from chaining two other SINCs.
 *
 * @author Antti Lankila
 */
public class TwoPassSincResampler implements Resampler {
	private final SincResampler s1, s2;

	public TwoPassSincResampler(double clockFrequency, double samplingFrequency, double highestAccurateFrequency) {
		/*
		 * Calculation according to Laurent Ganier. It evaluates to about 120 kHz at
		 * typical settings. Some testing around the chosen value seems to confirm that
		 * this does work.
		 */
		double intermediateFrequency = 2 * highestAccurateFrequency + Math.sqrt(2 * highestAccurateFrequency
				* clockFrequency * (samplingFrequency - 2 * highestAccurateFrequency) / samplingFrequency);
		s1 = new SincResampler(clockFrequency, intermediateFrequency, highestAccurateFrequency);
		s2 = new SincResampler(intermediateFrequency, samplingFrequency, highestAccurateFrequency);
		reset();
	}

	@Override
	public boolean input(int sample) {
		return s1.input(sample) && s2.input(s1.output());
	}

	@Override
	public int output() {
		return s2.output();
	}

	@Override
	public void reset() {
		s1.reset();
		s2.reset();
	}
}
