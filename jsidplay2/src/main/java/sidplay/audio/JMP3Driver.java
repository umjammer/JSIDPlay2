package sidplay.audio;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.CPUClock;
import libsidplay.common.EventScheduler;
import libsidplay.config.IAudioSection;
import lowlevel.LameEncoder;
import mp3.MPEGMode;

/**
 * Abstract base class to output an MP3 encoded tune to an output stream.
 *
 * @author Ken Händel
 *
 */
public abstract class JMP3Driver implements AudioDriver {

	/**
	 * File based driver to create a MP3 file.
	 *
	 * @author Ken Händel
	 *
	 */
	public static class JMP3FileDriver extends JMP3Driver {

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
					throw new RuntimeException("Error closing MP3 audio stream", e);
				} finally {
					out = null;
				}
			}
		}
	}

	/**
	 * Driver to write into an MP3 encoded output stream.<BR>
	 *
	 * <B>Note:</B> The caller is responsible of closing the output stream
	 *
	 * @author Ken Händel
	 *
	 */
	public static class JMP3StreamDriver extends JMP3Driver {

		/**
		 * Use several instances for parallel emulator instances, where applicable.
		 *
		 * @param out Output stream to write the encoded MP3 to
		 */
		public JMP3StreamDriver(OutputStream out) {
			this.out = out;
		}

		@Override
		protected OutputStream getOut(String recordingFilename) {
			return out;
		}

	}

	/**
	 * Jump3r encoder.
	 */
	private LameEncoder jump3r;
	/**
	 * Output stream to write the encoded MP3 to.
	 */
	protected OutputStream out;
	/**
	 * Sample buffer to be encoded as MP3.
	 */
	protected ByteBuffer sampleBuffer;

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		AudioConfig cfg = new AudioConfig(audioSection);
		boolean signed = true;
		boolean bigEndian = false;
		AudioFormat audioFormat = new AudioFormat(cfg.getFrameRate(), Short.SIZE, cfg.getChannels(), signed, bigEndian);
		jump3r = new LameEncoder(audioFormat, audioSection.getCbr(), MPEGMode.STEREO, audioSection.getVbrQuality(),
				audioSection.isVbr());
		out = getOut(recordingFilename);

		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public void write() throws InterruptedException {
		try {
			byte[] encoded = new byte[jump3r.getMP3BufferSize()];
			int bytesWritten = jump3r.encodeBuffer(sampleBuffer.array(), 0, sampleBuffer.position(), encoded);
			out.write(encoded, 0, bytesWritten);
		} catch (ArrayIndexOutOfBoundsException | IOException e) {
			throw new RuntimeException("Error writing MP3 audio stream", e);
		}
	}

	@Override
	public void close() {
		if (jump3r != null) {
			jump3r.close();
		}
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
		return ".mp3";
	}

	protected abstract OutputStream getOut(String recordingFilename) throws IOException;
}
