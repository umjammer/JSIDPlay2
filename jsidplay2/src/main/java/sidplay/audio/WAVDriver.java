package sidplay.audio;

import static com.xuggle.xuggler.ICodec.ID.CODEC_ID_PCM_S16LE;
import static libsidplay.common.SamplingRate.HIGH;
import static libsidplay.common.SamplingRate.LOW;
import static libsidplay.common.SamplingRate.MEDIUM;
import static libsidplay.common.SamplingRate.VERY_LOW;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.List;

import javax.sound.sampled.LineUnavailableException;

import com.xuggle.xuggler.ICodec.ID;

import libsidplay.common.CPUClock;
import libsidplay.common.EventScheduler;
import libsidplay.common.SamplingRate;
import libsidplay.config.IAudioSection;
import sidplay.audio.wav.WAVHeader;
import sidplay.audio.xuggle.XuggleAudioDriver;

/**
 * Abstract base class to output a WAV to an output stream.
 *
 * @author Ken Händel
 *
 */
public abstract class WAVDriver extends XuggleAudioDriver {

	/**
	 * File based driver to create a WAV file.
	 *
	 * @author Ken Händel
	 *
	 */
	public static class WAVFileDriver extends WAVDriver {

		private RandomAccessFile file;

		@Override
		protected OutputStream getOut(String recordingFilename) throws IOException {
			System.out.println("Recording, file=" + recordingFilename);
			file = new RandomAccessFile(recordingFilename, "rw");
			return new FileOutputStream(file.getFD());
		}

		@Override
		public void close() {
			super.close();
			if (out != null && file != null) {
				try {
					file.seek(0);
					out.write(wavHeader.getBytes());
					out.close();

					file.close();
				} catch (IOException e) {
					throw new RuntimeException("Error closing WAV audio stream", e);
				} finally {
					out = null;
					file = null;
				}
			}
		}
	}

	/**
	 * Driver to write into an WAV output stream.<BR>
	 *
	 * <B>Note:</B> The caller is responsible of closing the output stream
	 * <B>Note:</B> WAV header is missing using the XuggleAudioDriver, therefore
	 * kind of useless
	 *
	 * @author Ken Händel
	 *
	 */
	public static class WAVStreamDriver extends WAVDriver {

		/**
		 * Use several instances for parallel emulator instances, where applicable.
		 *
		 * @param out Output stream to write the encoded WAV to
		 */
		public WAVStreamDriver(OutputStream out) {
			this.out = out;
		}

		@Override
		protected OutputStream getOut(String recordingFilename) {
			return out;
		}

	}

	protected WAVHeader wavHeader;

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		AudioConfig cfg = new AudioConfig(audioSection);
		wavHeader = new WAVHeader(cfg.getChannels(), cfg.getFrameRate());

		super.open(audioSection, recordingFilename, cpuClock, context);
	}

	@Override
	public void write() throws InterruptedException {
		wavHeader.advance(sampleBuffer.position());

		super.write();
	}

	@Override
	protected List<SamplingRate> getSupportedSamplingRates() {
		return Arrays.asList(VERY_LOW, LOW, MEDIUM, HIGH);
	}

	@Override
	protected SamplingRate getDefaultSamplingRate() {
		return LOW;
	}

	@Override
	protected ID getAudioCodec() {
		return CODEC_ID_PCM_S16LE;
	}

	@Override
	protected String getOutputFormatName() {
		return "wav";
	}
	
	@Override
	public String getExtension() {
		return ".wav";
	}

}
