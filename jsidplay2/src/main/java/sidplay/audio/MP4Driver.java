package sidplay.audio;

import static com.xuggle.xuggler.ICodec.ID.CODEC_ID_AAC;
import static com.xuggle.xuggler.ICodec.ID.CODEC_ID_H264;
import static libsidplay.common.SamplingRate.LOW;
import static libsidplay.common.SamplingRate.MEDIUM;
import static libsidplay.common.SamplingRate.VERY_LOW;

import java.util.Arrays;
import java.util.List;

import com.xuggle.xuggler.ICodec.ID;

import libsidplay.common.SamplingRate;
import libsidplay.config.IAudioSection;
import sidplay.audio.xuggle.XuggleVideoDriver;

public abstract class MP4Driver extends XuggleVideoDriver {

	/**
	 * File based driver to create a MP4 file.
	 *
	 * @author Ken Händel
	 *
	 */
	public static class MP4FileDriver extends MP4Driver {

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
		return CODEC_ID_AAC;
	}

	@Override
	protected String getOutputFormatName() {
		return "mpeg4";
	}
	
	@Override
	public String getExtension() {
		return ".mp4";
	}

}
