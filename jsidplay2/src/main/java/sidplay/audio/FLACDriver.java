package sidplay.audio;

import static com.xuggle.xuggler.ICodec.ID.CODEC_ID_FLAC;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import com.xuggle.xuggler.ICodec.ID;

import libsidplay.common.SamplingRate;
import sidplay.audio.xuggle.XuggleAudioDriver;

public abstract class FLACDriver extends XuggleAudioDriver {

	/**
	 * File based driver to create a FLAC file.
	 *
	 * @author Ken Händel
	 *
	 */
	public static class FLACFileDriver extends FLACDriver {

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
					((FileOutputStream) out).close();
				} catch (IOException e) {
					throw new RuntimeException("Error closing FLAC audio stream", e);
				} finally {
					out = null;
				}
			}
		}
	}

	/**
	 * Driver to write into an FLAC output stream.<BR>
	 *
	 * <B>Note:</B> The caller is responsible of closing the output stream
	 *
	 * @author Ken Händel
	 *
	 */
	public static class FLACStreamDriver extends FLACDriver {

		/**
		 * Use several instances for parallel emulator instances, where applicable.
		 *
		 * @param out
		 *            Output stream to write the encoded FLAC to
		 */
		public FLACStreamDriver(OutputStream out) {
			this.out = out;
		}

		@Override
		protected OutputStream getOut(String recordingFilename) {
			return out;
		}

	}

	@Override
	protected List<SamplingRate> getSupportedSamplingRates() {
		return Arrays.asList(SamplingRate.VERY_LOW, SamplingRate.LOW, SamplingRate.MEDIUM, SamplingRate.HIGH);
	}

	@Override
	protected SamplingRate getDefaultSamplingRate() {
		return SamplingRate.LOW;
	}

	@Override
	protected String getOutputFormatName() {
		return "flac";
	}

	@Override
	protected ID getAudioCodec() {
		return CODEC_ID_FLAC;
	}

	@Override
	public String getExtension() {
		return ".flac";
	}
}