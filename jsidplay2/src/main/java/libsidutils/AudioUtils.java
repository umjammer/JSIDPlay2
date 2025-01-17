package libsidutils;

import static libsidutils.IOUtils.readNBytes;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import builder.resid.resample.Resampler;
import libsidplay.common.SamplingMethod;
import libsidplay.common.SamplingRate;

public class AudioUtils {

	private static final Logger LOG = Logger.getLogger(AudioUtils.class.getName());

	private static class ResamplingState {
		private final Random RANDOM = new Random();
		private int oldRandomValue;

		private int triangularDithering() {
			int prevValue = oldRandomValue;
			oldRandomValue = RANDOM.nextInt() & 0x1;
			return oldRandomValue - prevValue;
		}

	}

	/**
	 * Convert an audio input stream into mono using the target sample rate. Sample
	 * size must be 16-bit signed and little-endian.
	 * 
	 * <OL>
	 * <LI>Stereo is converted to mono.
	 * <LI>If the audio does not match the target sample rate, resampling takes
	 * place (downward or upward). Upward resampling is just a simple duplication of
	 * sample data.
	 * </OL>
	 * 
	 * @param is         audio input stream
	 * @param maxSeconds maximum number of seconds to use (rest of the available
	 *                   audio input samples are discarded)
	 * @param sampleRate target sample rate
	 * @return samples in the resulting format
	 * @throws IOException                   I/O error
	 * @throws UnsupportedAudioFileException audio input format not supported
	 */
	public static short[] convertToMonoWithSampleRate(InputStream is, long maxSeconds, SamplingRate sampleRate)
			throws IOException, UnsupportedAudioFileException {
		AudioInputStream stream = AudioSystem.getAudioInputStream(new BufferedInputStream(is));
		if (stream.getFormat().getSampleSizeInBits() != Short.SIZE) {
			throw new IOException("Sample size in bits must be " + Short.SIZE);
		}
		if (stream.getFormat().getEncoding() != Encoding.PCM_SIGNED) {
			throw new IOException("Encoding must be " + Encoding.PCM_SIGNED);
		}
		if (stream.getFormat().isBigEndian()) {
			throw new IOException("LittleEndian expected");
		}
		byte[] bytes = new byte[(int) (Math.min(
				stream.getFrameLength() * stream.getFormat().getChannels() * Short.BYTES,
				maxSeconds * stream.getFormat().getSampleRate() * stream.getFormat().getChannels() * Short.BYTES))];

		int read = readNBytes(stream, bytes, 0, bytes.length);
		if (read < bytes.length) {
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine(String.format("Unexpected end of audio stream: read=%d, expected=%d", read, bytes.length));
			}
		}

		// 1. stereo to mono conversion
		if (stream.getFormat().getChannels() == 2) {
			ByteBuffer stereoBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
			ByteBuffer monoBuffer = ByteBuffer.allocate(bytes.length >> 1).order(ByteOrder.LITTLE_ENDIAN);
			while (stereoBuffer.hasRemaining()) {
				monoBuffer.putShort((short) ((stereoBuffer.getShort() + stereoBuffer.getShort()) >> 1));
			}
			bytes = monoBuffer.array();
		} else if (stream.getFormat().getChannels() != 1) {
			throw new IOException("Number of channels must be one or two");
		}

		// 2. Sample Frequencies lower than target frequency? Duplicate samples.
		int factor = 1;
		float srcSampleRate = stream.getFormat().getSampleRate();
		int targetSampleRate = sampleRate.getFrequency();
		// difference must be big enough to work with our SincResampler!
		if (srcSampleRate != targetSampleRate) {
			while (srcSampleRate / targetSampleRate < 2) {
				srcSampleRate *= 2;
				factor <<= 1;
			}
		}

		// 3. down sampling to target frequency
		if (srcSampleRate > targetSampleRate) {
			ShortBuffer sourceBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
			ByteBuffer resampledBuffer = ByteBuffer.allocateDirect(factor * bytes.length)
					.order(ByteOrder.LITTLE_ENDIAN);

			ResamplingState resamplingState = new ResamplingState();
			Resampler downSampler = Resampler.createResampler(srcSampleRate, SamplingMethod.RESAMPLE, targetSampleRate,
					sampleRate.getMiddleFrequency());

			while (sourceBuffer.hasRemaining()) {
				short val = sourceBuffer.get();

				for (int i = 0; i < factor; i++) {
					int dither = resamplingState.triangularDithering();
					if (downSampler.input(val)) {
						if (!resampledBuffer.putShort((short) Math
								.max(Math.min(downSampler.output() + dither, Short.MAX_VALUE), Short.MIN_VALUE))
								.hasRemaining()) {
							((Buffer) resampledBuffer).flip();
						}
					}
				}
			}
			bytes = new byte[resampledBuffer.position()];
			((Buffer) resampledBuffer).rewind();
			resampledBuffer.get(bytes);
			factor = 1;
		}
		ByteBuffer sourceBuffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
		ShortBuffer resultBuffer = ShortBuffer.allocate(bytes.length * factor << 1);
		while (sourceBuffer.hasRemaining()) {
			short val = sourceBuffer.getShort();

			for (int i = 0; i < factor; i++) {
				resultBuffer.put(val);
			}
		}
		short[] shorts = new short[resultBuffer.position()];
		((Buffer) resultBuffer).rewind();
		resultBuffer.get(shorts);
		return shorts;
	}

}
