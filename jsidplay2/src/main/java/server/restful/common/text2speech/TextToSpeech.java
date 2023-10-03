package server.restful.common.text2speech;

import static libsidutils.IOUtils.readNBytes;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Random;
import java.util.function.Consumer;

import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import builder.resid.resample.Resampler;
import libsidplay.common.SamplingMethod;
import libsidplay.common.SamplingRate;
import libsidplay.config.IAudioSection;
import libsidplay.config.ISidPlay2Section;
import sidplay.Player;

public class TextToSpeech implements Consumer<Player> {

	private TextToSpeechType textToSpeechType;

	private final Random RANDOM = new Random();
	private int oldRandomValue;

	public TextToSpeech(TextToSpeechType textToSpeechType) {
		this.textToSpeechType = textToSpeechType;
	}

	@Override
	public void accept(Player player) {
		try {
			ISidPlay2Section sidplay2Section = player.getConfig().getSidplay2Section();
			IAudioSection audioSection = player.getConfig().getAudioSection();
			SamplingRate sampleRate = audioSection.getSamplingRate();

			File wavFile = File.createTempFile("text2speech", ".wav", sidplay2Section.getTmpDir());
			wavFile.deleteOnExit();

			String[] processArguments = textToSpeechType.getProcessArgumentsFunction().apply(player.getTune().getInfo(),
					wavFile.getAbsolutePath());
			Process process = new ProcessBuilder(processArguments).start();
			int waitFlag = process.waitFor();
			if (waitFlag == 0) {
				int returnVal = process.exitValue();
				if (returnVal == 0) {
					AudioInputStream stream = AudioSystem
							.getAudioInputStream(new BufferedInputStream(new FileInputStream(wavFile)));
					if (stream.getFormat().getSampleSizeInBits() != Short.SIZE) {
						throw new IOException("Sample size in bits must be " + Short.SIZE);
					}
					if (stream.getFormat().getEncoding() != Encoding.PCM_SIGNED) {
						throw new IOException("Encoding must be " + Encoding.PCM_SIGNED);
					}
					if (stream.getFormat().isBigEndian()) {
						throw new IOException("LittleEndian expected");
					}
					byte[] bytes = new byte[(int) (stream.getFrameLength() * stream.getFormat().getChannels()
							* Short.BYTES)];

					int read = readNBytes(stream, bytes, 0, bytes.length);
					if (read < bytes.length) {
						throw new IOException("Unexpected end of audio stream");
					}

					// 1. stereo to mono conversion
					if (stream.getFormat().getChannels() == 2) {
						ShortBuffer stereoSamples = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
								.asShortBuffer();
						ByteBuffer monoBuffer = ByteBuffer.allocate(bytes.length >> 1).order(ByteOrder.LITTLE_ENDIAN);
						ShortBuffer monoSamples = monoBuffer.asShortBuffer();
						while (stereoSamples.hasRemaining()) {
							monoSamples.put((short) ((stereoSamples.get() + stereoSamples.get()) / 2));
						}
						bytes = monoBuffer.array();
					} else if (stream.getFormat().getChannels() != 1) {
						throw new IOException("Number of channels must be one or two");
					}

					// 2 Sample Frequencies lower than target frequency? Duplicate samples.
					int factor = 1;
					int srcSampleRate = (int) stream.getFormat().getSampleRate();
					int targetSampleRate = sampleRate.getFrequency();
					while (srcSampleRate < targetSampleRate) {
						srcSampleRate <<= 1;
						factor <<= 1;
					}

					// 3. down sampling to target frequency
					if (srcSampleRate > targetSampleRate) {
						Resampler downSampler = Resampler.createResampler(srcSampleRate, SamplingMethod.RESAMPLE,
								targetSampleRate, sampleRate.getMiddleFrequency());

						ByteBuffer result = ByteBuffer.allocate(factor * bytes.length).order(ByteOrder.LITTLE_ENDIAN);
						ShortBuffer sb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
						while (sb.hasRemaining()) {
							short val = sb.get();

							for (int i = 0; i < factor; i++) {
								int downSamplerDither = triangularDithering();
								if (downSampler.input(val)) {
									if (!result.putShort((short) Math.max(
											Math.min(downSampler.output() + downSamplerDither, Short.MAX_VALUE),
											Short.MIN_VALUE)).hasRemaining()) {
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
					while (sb.hasRemaining()) {
						short val = sb.get();

						for (int i = 0; i < factor; i++) {
							writeMonoToStereo(player, val);
						}
					}
				}
			}
			wavFile.delete();
		} catch (IOException | InterruptedException | UnsupportedAudioFileException e) {
			System.err
					.println("Error during Text2Speech! Install or deactivate it!? (https://espeak.sourceforge.net/)\n"
							+ e.getMessage());
		}
	}

	private int triangularDithering() {
		int prevValue = oldRandomValue;
		oldRandomValue = RANDOM.nextInt() & 0x1;
		return oldRandomValue - prevValue;
	}

	private void writeMonoToStereo(Player player, short val) throws InterruptedException {
		player.getAudioDriver().buffer().putShort((short) Math.max(Math.min(val, Short.MAX_VALUE), Short.MIN_VALUE));
		if (!player.getAudioDriver().buffer()
				.putShort((short) Math.max(Math.min(val, Short.MAX_VALUE), Short.MIN_VALUE)).hasRemaining()) {
			player.getAudioDriver().write();
			((Buffer) player.getAudioDriver().buffer()).clear();
		}
	}

}
