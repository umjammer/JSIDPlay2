package sidplay.audio;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.CPUClock;
import libsidplay.common.EventScheduler;
import libsidplay.config.IAudioSection;
import sidplay.audio.wav.WAVHeader;

/**
 * Abstract base class to output a WAV to an output stream.
 *
 * @author Ken Händel
 *
 */
public abstract class JWAVDriver implements AudioDriver {

	/**
	 * File based driver to create a WAV file.
	 *
	 * @author Ken Händel
	 *
	 */
	public static class JWAVFileDriver extends JWAVDriver {

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
	 *
	 * @author Ken Händel
	 *
	 */
	public static class JWAVStreamDriver extends JWAVDriver {

		/**
		 * Use several instances for parallel emulator instances, where applicable.
		 *
		 * @param out Output stream to write the encoded WAV to
		 */
		public void setOut(OutputStream out) {
			this.out = out;
		}

		@Override
		protected OutputStream getOut(String recordingFilename) {
			return out;
		}

	}

	/**
	 * Output stream to write the encoded WAV to.
	 */
	protected OutputStream out;

	protected WAVHeader wavHeader;

	private ByteBuffer sampleBuffer;

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		AudioConfig cfg = new AudioConfig(audioSection);
		wavHeader = new WAVHeader(cfg.getChannels(), cfg.getFrameRate());

		out = getOut(recordingFilename);
		out.write(wavHeader.getBytes());

		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public void write() throws InterruptedException {
		try {
			out.write(sampleBuffer.array(), 0, sampleBuffer.position());
			wavHeader.advance(sampleBuffer.position());
		} catch (final IOException e) {
			throw new RuntimeException("Error writing WAV audio stream", e);
		}
	}

	@Override
	public void close() {
	}

	@Override
	public ByteBuffer buffer() {
		return sampleBuffer;
	}

	@Override
	public boolean isRecording() {
		return true;
	}

	@Override
	public String getExtension() {
		return ".wav";
	}

	protected abstract OutputStream getOut(String recordingFilename) throws IOException;
}
