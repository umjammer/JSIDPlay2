package server.restful.common.text2speech;

import static libsidutils.IOUtils.readNBytes;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.function.Consumer;

import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import libsidplay.config.ISidPlay2Section;
import sidplay.Player;

public class TextToSpeech implements Consumer<Player> {

	private TextToSpeechType textToSpeechType;

	public TextToSpeech(TextToSpeechType textToSpeechType) {
		this.textToSpeechType = textToSpeechType;
	}

	@Override
	public void accept(Player player) {
		try {
			ISidPlay2Section sidplay2Section = player.getConfig().getSidplay2Section();

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

					ShortBuffer sb = java.nio.ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
					while (sb.hasRemaining()) {
						short val = sb.get();

						writeMono(player, val);
						if (stream.getFormat().getSampleRate() == 22050) {
							writeMono(player, val);
						}
					}
					player.getAudioDriver().write();
					((Buffer) player.getAudioDriver().buffer()).clear();
				}
			}
			wavFile.delete();
		} catch (IOException | InterruptedException | UnsupportedAudioFileException e) {
			e.printStackTrace();
		}
	}

	private void writeMono(Player player, short val) throws InterruptedException {
		player.getAudioDriver().buffer().putShort((short) Math.max(Math.min(val, Short.MAX_VALUE), Short.MIN_VALUE));
		if (!player.getAudioDriver().buffer()
				.putShort((short) Math.max(Math.min(val, Short.MAX_VALUE), Short.MIN_VALUE)).hasRemaining()) {
			player.getAudioDriver().write();
			((Buffer) player.getAudioDriver().buffer()).clear();
		}
	}

}
