package server.restful.common.text2speech;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.sound.sampled.UnsupportedAudioFileException;

import libsidplay.config.IAudioSection;
import libsidplay.config.ISidPlay2Section;
import libsidutils.AudioUtils;
import sidplay.Player;

public class TextToSpeech implements Consumer<Player> {

	private static final Logger LOG = Logger.getLogger(TextToSpeech.class.getName());

	private TextToSpeechType textToSpeechType;

	private File file;

	public TextToSpeech(TextToSpeechType textToSpeechType, File file) {
		this.textToSpeechType = textToSpeechType;
		this.file = file;
	}

	@Override
	public void accept(Player player) {
		File wavFile = null;
		try {
			ISidPlay2Section sidplay2Section = player.getConfig().getSidplay2Section();
			IAudioSection audioSection = player.getConfig().getAudioSection();

			wavFile = File.createTempFile("text2speech", ".wav", sidplay2Section.getTmpDir());

			String[] processArguments = textToSpeechType.getProcessArgumentsFunction()
					.apply(new SimpleImmutableEntry<>(player, file), wavFile);
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine(Arrays.asList(processArguments).stream().collect(Collectors.joining(" ")));
			}
			Process process = new ProcessBuilder(processArguments).start();
			int waitFlag = process.waitFor();
			if (waitFlag == 0) {
				int returnVal = process.exitValue();
				if (returnVal == 0) {

					try (InputStream is = new FileInputStream(wavFile)) {
						short[] samples = AudioUtils.convertToMonoWithSampleRate(is, Integer.MAX_VALUE,
								audioSection.getSamplingRate());
						for (short sample : samples) {
							player.getAudioDriver().buffer().putShort(sample);
							if (!player.getAudioDriver().buffer().putShort(sample).hasRemaining()) {
								player.getAudioDriver().write();
								((Buffer) player.getAudioDriver().buffer()).clear();
							}
						}
					} catch (UnsupportedAudioFileException | IOException e) {
						throw new IOException(e);
					}
				} else {
					throw new IOException("Process failed with exit code: " + returnVal);
				}
			} else {
				throw new IOException("Process failed with waitFlag: " + waitFlag);
			}
		} catch (IOException | InterruptedException e) {
			System.err.println(
					"Error during Text2Speech! Install or deactivate it!? (apt-get install espeak or sudo apt install libttspico-utils)\n"
							+ e.getMessage());
		} finally {
			if (wavFile != null) {
				wavFile.delete();
			}
		}
	}

}
