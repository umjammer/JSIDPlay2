package sidplay.audio;

import static com.xuggle.xuggler.ICodec.ID.CODEC_ID_H264;
import static com.xuggle.xuggler.ICodec.ID.CODEC_ID_MP3;
import static libsidplay.common.SamplingRate.LOW;
import static libsidplay.common.SamplingRate.MEDIUM;
import static libsidplay.common.SamplingRate.VERY_LOW;

import java.util.Arrays;
import java.util.List;

import com.xuggle.xuggler.ICodec.ID;

import libsidplay.common.SamplingRate;
import libsidplay.config.IAudioSection;
import sidplay.audio.xuggle.XuggleVideoDriver;

/**
 * File based driver to create a AVI file.
 *
 * @author Ken Händel
 *
 */
public abstract class AVIDriver extends XuggleVideoDriver {

	/**
	 * File based driver to create a AVI file.
	 *
	 * @author Ken Händel
	 *
	 */
	public static class AVIFileDriver extends AVIDriver {

		@Override
		protected String getUrl(IAudioSection audioSection, String recordingFilename) {
			System.out.println("Recording, file=" + recordingFilename);
			return recordingFilename;
		}

	}

	@Override
	protected List<SamplingRate> getSupportedSamplingRates() {
		return Arrays.asList(VERY_LOW, LOW, MEDIUM);
	}

	@Override
	protected SamplingRate getDefaultSamplingRate() {
		return LOW;
	}

	@Override
	protected ID getVideoCodec() {
		return CODEC_ID_H264;
	}

	@Override
	protected ID getAudioCodec() {
		return CODEC_ID_MP3;
	}

	@Override
	protected String getOutputFormatName() {
		return "avi";
	}
	
	@Override
	public String getExtension() {
		return ".avi";
	}

}
