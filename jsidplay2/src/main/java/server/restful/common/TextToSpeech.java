package server.restful.common;

import static libsidutils.IOUtils.readNBytes;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Iterator;
import java.util.function.Consumer;

import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

import libsidplay.config.ISidPlay2Section;
import libsidplay.sidtune.SidTuneInfo;
import sidplay.Player;

public class TextToSpeech implements Consumer<Player> {

	private String text;

	public TextToSpeech(SidTuneInfo info) {
		this.text = createText(info);
	}

	public String getText() {
		return text;
	}

	private String createText(SidTuneInfo info) {
		StringBuilder result = new StringBuilder("Now playing: ");
		Iterator<String> it = info.getInfoString().iterator();
		if (it.hasNext()) {
			result.append(it.next());
		}
		if (it.hasNext()) {
			String author = it.next();
			if (!"<?>".equals(author)) {
				result.append(", by: ").append(author);
			}
		}
		if (it.hasNext()) {
			String released = it.next();
			if (!"<?>".equals(released)) {
				result.append(", released at: ").append(released);
			}
		}
		return result.toString();
	}

	@Override
	public void accept(Player player) {
		try {
			ISidPlay2Section sidplay2Section = player.getConfig().getSidplay2Section();

			File speechFile = File.createTempFile("speech", ".wav", sidplay2Section.getTmpDir());
			speechFile.deleteOnExit();
			Process process = new ProcessBuilder("/usr/bin/espeak", text, "-s", "150", "-p", "20", "-k", "20", "-l",
					"5", "-w", speechFile.getAbsolutePath()).start();
			int waitFlag = process.waitFor();// Wait to finish application execution.
			if (waitFlag == 0) {
				int returnVal = process.exitValue();
				if (returnVal == 0) {
					AudioInputStream stream = AudioSystem
							.getAudioInputStream(new BufferedInputStream(new FileInputStream(speechFile)));
					if (stream.getFormat().getSampleSizeInBits() != Short.SIZE) {
						throw new IOException("Sample size in bits must be " + Short.SIZE);
					}
					if (stream.getFormat().getEncoding() != Encoding.PCM_SIGNED) {
						throw new IOException("Encoding must be " + Encoding.PCM_SIGNED);
					}
					if (stream.getFormat().isBigEndian()) {
						throw new IOException("LittleEndian expected");
					}
					if (stream.getFormat().getSampleRate() != 22050) {
						throw new IOException("Sample rate must be " + 22050);
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
						writeMono(player, val);
					}
					player.getAudioDriver().write();
					((Buffer) player.getAudioDriver().buffer()).clear();
				}
			}
			speechFile.delete();
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
