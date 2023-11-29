package sidplay.audio;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.ICodec.ID;

import libsidplay.config.IAudioSection;
import sidplay.audio.xuggle.XuggleAudioDriver;

/**
 * Abstract base class to output an MP3 encoded tune to an output stream.
 *
 * @author Ken Händel
 *
 */
public abstract class MP3Driver extends XuggleAudioDriver {

	/**
	 * Kbs
	 */
	private static final int KBS = 1000;
	/**
	 * CBR Default bit rate
	 */
	private static final int DEFAULT_BITRATE = 128 * KBS;
	/**
	 * VBR quality factor
	 */
	private static final int FF_QP2LAMBDA = 118;

	/**
	 * File based driver to create a MP3 file.
	 *
	 * @author Ken Händel
	 *
	 */
	public static class MP3FileDriver extends MP3Driver {

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
	public static class MP3StreamDriver extends MP3Driver {

		/**
		 * Use several instances for parallel emulator instances, where applicable.
		 *
		 * @param out Output stream to write the encoded MP3 to
		 */
		public MP3StreamDriver(OutputStream out) {
			this.out = out;
		}

		@Override
		protected OutputStream getOut(String recordingFilename) {
			return out;
		}

	}

	@Override
	protected void configureStreamCoder(IStreamCoder streamCoder, IAudioSection audioSection) {
		int bitRate = audioSection.getCbr() == -1 ? DEFAULT_BITRATE : audioSection.getCbr() * KBS;
		boolean isVbr = audioSection.isVbr();
		int vbrQuality = audioSection.getVbrQuality() * FF_QP2LAMBDA;

		streamCoder.setFlag(IStreamCoder.Flags.FLAG_QSCALE, isVbr);
		streamCoder.setBitRate(bitRate);
		streamCoder.setGlobalQuality(vbrQuality);
	}

	@Override
	protected String getOutputFormatName() {
		return "mp3";
	}

	@Override
	protected ID getAudioCodec() {
		return ID.CODEC_ID_MP3;
	}

	@Override
	public String getExtension() {
		return ".mp3";
	}
}
