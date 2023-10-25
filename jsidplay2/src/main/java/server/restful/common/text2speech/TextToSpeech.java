package server.restful.common.text2speech;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.util.Arrays;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.sound.sampled.UnsupportedAudioFileException;

import libsidplay.common.SamplingRate;
import libsidplay.config.IAudioSection;
import libsidplay.config.ISidPlay2Section;
import libsidutils.AudioUtils;
import sidplay.Player;

public class TextToSpeech implements Consumer<Player> {

	private static final Logger LOG = Logger.getLogger(TextToSpeech.class.getName());

	private TextToSpeechType textToSpeechType;

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
			if (LOG.isLoggable(Level.FINE)) {
				LOG.fine(Arrays.asList(processArguments).stream().collect(Collectors.joining(" ")));
			}
			Process process = new ProcessBuilder(processArguments).start();
			int waitFlag = process.waitFor();
			if (waitFlag == 0) {
				int returnVal = process.exitValue();
				if (returnVal == 0) {
					try (InputStream is = new FileInputStream(wavFile)) {
						short[] samples = new AudioUtils().convertToMonoAndRate(is, Integer.MAX_VALUE, sampleRate);
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
				}
			}
			wavFile.delete();
		} catch (IOException | InterruptedException e) {
			System.err.println(
					"Error during Text2Speech! Install or deactivate it!? (apt-get install espeak or sudo apt install libttspico-utils)\n"
							+ e.getMessage());
		}
	}

}
