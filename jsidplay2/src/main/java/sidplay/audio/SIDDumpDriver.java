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

	/**
	 * Print stream to write the encoded MP3 to.
	 */
	protected OutputStream out;

	private ByteBuffer sampleBuffer;

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		System.out.println("Recording, file=" + recordingFilename);
		AudioConfig cfg = new AudioConfig(audioSection);

		init(cpuClock);
		setTimeInSeconds(false);

		out = getOut(recordingFilename);

		writeHeader();

		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);
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

	@Override
	public void add(SidDumpOutput putput) throws IOException {
		StringBuilder builder = new StringBuilder();
		builder.append("| ");
		builder.append(putput.getTime());
		builder.append(" | ");
		builder.append(putput.getFreq(0));
		builder.append(" ");
		builder.append(putput.getNote(0));
		builder.append(" ");
		builder.append(putput.getWf(0));
		builder.append(" ");
		builder.append(putput.getAdsr(0));
		builder.append(" ");
		builder.append(putput.getPul(0));
		builder.append(" | ");
		builder.append(putput.getFreq(1));
		builder.append(" ");
		builder.append(putput.getNote(1));
		builder.append(" ");
		builder.append(putput.getWf(1));
		builder.append(" ");
		builder.append(putput.getAdsr(1));
		builder.append(" ");
		builder.append(putput.getPul(1));
		builder.append(" | ");
		builder.append(putput.getFreq(2));
		builder.append(" ");
		builder.append(putput.getNote(2));
		builder.append(" ");
		builder.append(putput.getWf(2));
		builder.append(" ");
		builder.append(putput.getAdsr(2));
		builder.append(" ");
		builder.append(putput.getPul(2));
		builder.append(" | ");
		builder.append(putput.getFcut());
		builder.append(" ");
		builder.append(putput.getRc());
		builder.append(" ");
		builder.append(putput.getTyp());
		builder.append(" ");
		builder.append(putput.getV());
		builder.append(" |\n");
		out.write(builder.toString().getBytes(StandardCharsets.ISO_8859_1));
	}

	private void writeHeader() throws IOException {
		StringBuilder builder = new StringBuilder();
		builder.append(String.format("Middle C frequency is $%04X\n", getMiddleCFreq()));
		builder.append("\n");
		builder.append(String.format(
				"| Frame | Freq Note/Abs WF ADSR Pul | Freq Note/Abs WF ADSR Pul | Freq Note/Abs WF ADSR Pul | FCut RC Typ V |\n"));
		builder.append(String.format(
				"+-------+---------------------------+---------------------------+---------------------------+---------------+\n"));
		out.write(builder.toString().getBytes(StandardCharsets.ISO_8859_1));
	}

	protected abstract OutputStream getOut(String recordingFilename) throws IOException;
}
