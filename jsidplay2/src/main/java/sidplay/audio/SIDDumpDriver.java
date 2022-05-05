package sidplay.audio;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.CPUClock;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDListener;
import libsidplay.config.IAudioSection;
import sidplay.audio.siddump.SIDDumpExtension;
import sidplay.audio.siddump.SidDumpOutput;

public abstract class SIDDumpDriver extends SIDDumpExtension implements AudioDriver, SIDListener {

	/**
	 * File based driver to create a SID dump file.
	 *
	 * @author Ken Händel
	 *
	 */
	public static class SIDDumpFileDriver extends SIDDumpDriver {
		@Override
		protected OutputStream getOut(String recordingFilename) throws IOException {
			System.out.println("Recording, file=" + recordingFilename);
			return new FileOutputStream(recordingFilename);
		}

		@Override
		public void close() {
			super.close();
			if (out != null) {
				try {
					out.close();
				} catch (IOException e) {
					throw new RuntimeException("Error closing SIDDumpDriver stream", e);
				} finally {
					out = null;
				}
			}
		}
	}

	/**
	 * Driver to write into an SID dump stream.<BR>
	 *
	 * <B>Note:</B> The caller is responsible of closing the output stream
	 *
	 * @author Ken Händel
	 *
	 */
	public static class SIDDumpStreamDriver extends SIDDumpDriver {

		/**
		 * Use several instances for parallel emulator instances, where applicable.
		 *
		 * @param out Output stream to write the SID dump to
		 */
		public SIDDumpStreamDriver(OutputStream out) {
			this.out = out;
		}

		@Override
		protected OutputStream getOut(String recordingFilename) {
			return out;
		}

	}

	protected OutputStream out;

	private ByteBuffer sampleBuffer;

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		AudioConfig cfg = new AudioConfig(audioSection);

		init(cpuClock);
		setTimeInSeconds(false);

		out = getOut(recordingFilename);

		out.write(toHeaderString().getBytes(StandardCharsets.ISO_8859_1));

		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public void add(SidDumpOutput output) throws IOException {
		out.write(output.toString().getBytes(StandardCharsets.ISO_8859_1));
	}

	@Override
	public boolean isAborted() {
		return false;
	}

	@Override
	public void write() throws InterruptedException {
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
		return ".txt";
	}

	protected abstract OutputStream getOut(String recordingFilename) throws IOException;
}
