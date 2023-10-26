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

import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import builder.resid.resample.Resampler;
import libsidplay.common.SamplingMethod;
import libsidplay.common.SamplingRate;

public class AudioUtils {

	private final Random RANDOM = new Random();
	private int oldRandomValue;

	public short[] convertToMonoAndRate(InputStream is, long frameMaxLength, SamplingRate sampleRate)
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
		byte[] bytes = new byte[(int) (Math.min(stream.getFrameLength(), frameMaxLength)
				* stream.getFormat().getChannels() * Short.BYTES)];

		int read = readNBytes(stream, bytes, 0, bytes.length);
		if (read < bytes.length) {
			throw new IOException("Unexpected end of audio stream");
		}

		// remove wasted audio (exceeding frameMaxLength)
		if (stream.getFrameLength() > frameMaxLength) {
			int length = (int) ((stream.getFrameLength() - frameMaxLength) * stream.getFormat().getChannels()
					* Short.BYTES);
			readNBytes(stream, new byte[length], 0, length);
		}

		// 1. stereo to mono conversion
		if (stream.getFormat().getChannels() == 2) {
			ShortBuffer stereoSamples = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
			ByteBuffer monoBuffer = ByteBuffer.allocate(bytes.length >> 1).order(ByteOrder.LITTLE_ENDIAN);
			ShortBuffer monoSamples = monoBuffer.asShortBuffer();
			while (stereoSamples.hasRemaining()) {
				monoSamples.put((short) ((stereoSamples.get() + stereoSamples.get()) / 2));
			}
			bytes = monoBuffer.array();
		} else if (stream.getFormat().getChannels() != 1) {
			throw new IOException("Number of channels must be one or two");
		}

		// 2. Sample Frequencies lower than target frequency? Duplicate samples.
		int factor = 1;
		float srcSampleRate = stream.getFormat().getSampleRate();
		int targetSampleRate = sampleRate.getFrequency();
		while (srcSampleRate < targetSampleRate) {
			srcSampleRate *= 2;
			factor <<= 1;
		}

		// 3. down sampling to target frequency
		if (srcSampleRate > targetSampleRate) {
			Resampler downSampler = Resampler.createResampler(srcSampleRate, SamplingMethod.RESAMPLE, targetSampleRate,
					sampleRate.getMiddleFrequency());

			ByteBuffer result = ByteBuffer.allocate(factor * bytes.length).order(ByteOrder.LITTLE_ENDIAN);
			ShortBuffer sb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
			while (sb.hasRemaining()) {
				short val = sb.get();

				for (int i = 0; i < factor; i++) {
					int downSamplerDither = triangularDithering();
					if (downSampler.input(val)) {
						if (!result.putShort(
								(short) Math.max(Math.min(downSampler.output() + downSamplerDither, Short.MAX_VALUE),
										Short.MIN_VALUE))
								.hasRemaining()) {
							((Buffer) result).flip();
						}
					}
				}
			}
			bytes = new byte[result.position()];
			((Buffer) result).rewind();
			result.get(bytes);
			factor = 1;
		}
		ShortBuffer sb = java.nio.ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
		ShortBuffer result = ShortBuffer.allocate(sb.capacity() * factor);
		while (sb.hasRemaining()) {
			short val = sb.get();

			for (int i = 0; i < factor; i++) {
				result.put(val);
			}
		}
		return result.array();
	}

	private int triangularDithering() {
		int prevValue = oldRandomValue;
		oldRandomValue = RANDOM.nextInt() & 0x1;
		return oldRandomValue - prevValue;
	}

}
