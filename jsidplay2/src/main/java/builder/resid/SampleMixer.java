package builder.resid;

import static builder.resid.SIDMixer.VOLUME_SCALER;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.function.IntConsumer;

/**
 * Sound sample consumer consuming sample data while a SID is being clock'ed. A
 * sample value is added to the audio buffer to mix the output of several SIDs
 * together.<BR>
 * <B>Note:</B> To mix several SIDs, all SampleMixer's IntBuffers must wrap the
 * same audio buffer. Additionally, the buffer must be cleared, before the next
 * mixing starts.
 *
 * @author ken
 *
 */
public interface SampleMixer extends IntConsumer {
	/**
	 *
	 * Extends SampleMixer with linear fade-in/fade-out feature to smoothly
	 * increase/decrease volume. TODO implement logarithmic type?
	 *
	 * @author ken
	 *
	 */
	static class LinearFadingSampleMixer extends DefaultSampleMixer {
		/**
		 * Fade-in/fade-out time in clock ticks.
		 */
		private long fadeInClocks, fadeOutClocks;

		/**
		 * Currently configured volume level.
		 */
		private int maxVolL, maxVolR;

		/**
		 * Fade-in/fade-out clock steps until next volume change and current fade-in and
		 * fade-out counters for left and right speaker.
		 */
		private long fadeInStepL, fadeInStepR, fadeOutStepL, fadeOutStepR, fadeInValL, fadeInValR, fadeOutValL,
				fadeOutValR;

		LinearFadingSampleMixer(IntBuffer audioBufferL, IntBuffer audioBufferR) {
			super(audioBufferL, audioBufferR);
		}

		/**
		 * Set fade-in time. Increase volume from zero to the maximum.
		 *
		 * @param fadeIn fade-in time in clock ticks
		 */
		public void setFadeIn(long fadeIn) {
			this.fadeInClocks = fadeIn;
			super.setVolume(0, 0);
			fadeInValL = fadeInStepL = maxVolL != 0 ? fadeInClocks / maxVolL : 0;
			fadeInValR = fadeInStepR = maxVolR != 0 ? fadeInClocks / maxVolR : 0;
		}

		/**
		 * Set fade-out time. Decrease volume from the maximum to zero.
		 *
		 * @param fadeOut fade-out time in clock ticks
		 */
		public void setFadeOut(long fadeOut) {
			this.fadeOutClocks = fadeOut;
			super.setVolume(maxVolL, maxVolR);
			fadeOutValL = fadeOutStepL = maxVolL != 0 ? fadeOutClocks / maxVolL : 0;
			fadeOutValR = fadeOutStepR = maxVolR != 0 ? fadeOutClocks / maxVolR : 0;
		}

		@Override
		public void setVolume(int volumeL, int volumeR) {
			super.setVolume(volumeL, volumeR);
			this.maxVolL = volumeL;
			this.maxVolR = volumeR;
		}

		@Override
		public void accept(int sample) {
			if (fadeInClocks > 0) {
				fadeInClocks--;
				if (--fadeInValL == 0) {
					fadeInValL = fadeInStepL;
					volumeL++;
				}
				if (--fadeInValR == 0) {
					fadeInValR = fadeInStepR;
					volumeR++;
				}
			} else if (fadeOutClocks > 0) {
				fadeOutClocks--;
				if (--fadeOutValL == 0) {
					fadeOutValL = fadeOutStepL;
					volumeL--;
				}
				if (--fadeOutValR == 0) {
					fadeOutValR = fadeOutStepR;
					volumeR--;
				}
			}
			super.accept(sample);
		}

	}

	static class DefaultSampleMixer implements SampleMixer {

		/**
		 * Buffers of mixed sample values for left/right speaker.
		 */
		private IntBuffer bufferL, bufferR;

		/**
		 * Audibility of mixed sample values for left/right speaker.
		 */
		protected int volumeL, volumeR;

		/**
		 * Sample buffer for delay effect.
		 */
		private IntBuffer delayedSamples;

		private boolean delayedSamplesEnabled;

		DefaultSampleMixer(IntBuffer audioBufferL, IntBuffer audioBufferR) {
			this.bufferL = audioBufferL;
			this.bufferR = audioBufferR;
			this.delayedSamplesEnabled = false;
			setVolume(1 << VOLUME_SCALER, 1 << VOLUME_SCALER);
			setDelay(0);
		}

		@Override
		public void setVolume(int volumeL, int volumeR) {
			this.volumeL = volumeL;
			this.volumeR = volumeR;
		}

		@Override
		public void setDelay(int delayedSamples) {
			this.delayedSamplesEnabled = delayedSamples != 0;
			this.delayedSamples = ByteBuffer.allocateDirect(Integer.BYTES * (delayedSamples + 1))
					.order(ByteOrder.nativeOrder()).asIntBuffer().put(new int[delayedSamples + 1]);
			((Buffer) this.delayedSamples).flip();
		}

		@Override
		public void accept(int sample) {
			if (this.delayedSamplesEnabled) {
				if (!delayedSamples.put(sample).hasRemaining()) {
					((Buffer) this.delayedSamples).flip();
				}
				sample = delayedSamples.get(delayedSamples.position());
			}
			bufferL.put(bufferL.get(bufferL.position()) + sample * volumeL);
			bufferR.put(bufferR.get(bufferR.position()) + sample * volumeR);
		}

		@Override
		public void clear() {
			((Buffer) bufferL).clear();
			((Buffer) bufferR).clear();
		}
	}

	public static class NoOpSampleMixer implements SampleMixer {

		@Override
		public void setVolume(int volumeL, int volumeR) {
		}

		@Override
		public void setDelay(int delayedSamples) {
		}

		@Override
		public void accept(int sample) {
		}

		@Override
		public void clear() {
		}
	}

	void setVolume(int volumeL, int volumeR);

	void setDelay(int delayedSamples);

	void clear();

}