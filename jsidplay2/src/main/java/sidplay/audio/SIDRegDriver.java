package sidplay.audio;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ResourceBundle;

import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.CPUClock;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDListener;
import libsidplay.config.IAudioSection;
import sidplay.audio.sidreg.SidRegWrite;

public abstract class SIDRegDriver implements SIDListener, AudioDriver {

	public enum Format {
		NORMAL, APP, C64_JUKEBOX
	}

	/**
	 * File based driver to create a SID reg file.
	 *
	 * @author Ken Händel
	 *
	 */
	public static class SIDRegFileDriver extends SIDRegDriver {
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
					throw new RuntimeException("Error closing SIDRegDriver stream", e);
				} finally {
					out = null;
				}
			}
		}
	}

	/**
	 * Driver to write into an SID reg stream.<BR>
	 *
	 * <B>Note:</B> The caller is responsible of closing the output stream
	 *
	 * @author Ken Händel
	 *
	 */
	public static class SIDRegStreamDriver extends SIDRegDriver {

		/**
		 * Use several instances for parallel emulator instances, where applicable.
		 *
		 * @param out    Output stream to write the SID reg to
		 * @param format SID register writes format
		 */
		public SIDRegStreamDriver(OutputStream out, Format format) {
			this.out = out;
			this.format = format;
		}

		@Override
		protected OutputStream getOut(String recordingFilename) {
			return out;
		}

	}

	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("sidplay.audio.SIDRegDriver");

	protected OutputStream out;
	protected Format format;

	private EventScheduler context;

	private long fTime;
	private ByteBuffer sampleBuffer;

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		AudioConfig cfg = new AudioConfig(audioSection);
		this.context = context;

		out = getOut(recordingFilename);

		fTime = 0;
		if (format != Format.C64_JUKEBOX) {
			writeHeader(out);
		}
		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public void write(final int addr, final byte data) {
		final long time = context.getTime(Event.Phase.PHI2);
		if (fTime == 0) {
			fTime = time;
		}
		final long relTime = time - fTime;

		try {
			new SidRegWrite(time, relTime, addr, data).writeSidRegister(out, format, relTime == 0);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		fTime = time;
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
		return ".csv";
	}

	public static void writeHeader(OutputStream out) throws IOException {
		out.write(String.format("\"%s\", \"%s\", \"%s\", \"%s\", \"%s\"\n", BUNDLE.getString("ABSOLUTE_CYCLES"),
				BUNDLE.getString("RELATIVE_CYCLES"), BUNDLE.getString("ADDRESS"), BUNDLE.getString("VALUE"),
				BUNDLE.getString("DESCRIPTION")).getBytes(StandardCharsets.ISO_8859_1));
	}

	protected abstract OutputStream getOut(String recordingFilename) throws IOException;

}
