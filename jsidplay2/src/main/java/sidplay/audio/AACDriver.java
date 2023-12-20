package sidplay.audio;

import static com.xuggle.xuggler.ICodec.ID.CODEC_ID_AAC;
import static libsidplay.common.SamplingRate.HIGH;
import static libsidplay.common.SamplingRate.LOW;
import static libsidplay.common.SamplingRate.MEDIUM;
import static libsidplay.common.SamplingRate.VERY_LOW;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import com.xuggle.xuggler.ICodec.ID;

import libsidplay.common.SamplingRate;
import sidplay.audio.xuggle.XuggleAudioDriver;

/**
 * File based driver to create a AAC file.
 *
 * @author Ken Händel
 *
 */
public abstract class AACDriver extends XuggleAudioDriver {

	/**
	 * File based driver to create a AAC file.
	 *
	 * @author Ken Händel
	 *
	 */
	public static class AACFileDriver extends AACDriver {

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
					throw new RuntimeException("Error closing AAC audio stream", e);
				} finally {
					out = null;
				}
			}
		}
	}

	/**
	 * Driver to write into an AAC output stream.<BR>
	 *
	 * <B>Note:</B> The caller is responsible of closing the output stream
	 *
	 * @author Ken Händel
	 *
	 */
	public static class AACStreamDriver extends AACDriver {

		/**
		 * Use several instances for parallel emulator instances, where applicable.
		 *
		 * @param out Output stream to write the encoded AAC to
		 */
		public AACStreamDriver(OutputStream out) {
			this.out = out;
		}

		@Override
		protected OutputStream getOut(String recordingFilename) {
			return out;
		}

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
		return CODEC_ID_AAC;
	}

	@Override
	protected String getOutputFormatName() {
		return "adts";
	}
	
	@Override
	public String getExtension() {
		return ".aac";
	}

}
