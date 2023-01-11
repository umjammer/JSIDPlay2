package libsidutils.fingerprinting.rest.beans;

import static libsidplay.common.SamplingRate.VERY_LOW;
import static libsidplay.config.IWhatsSidSystemProperties.FRAME_MAX_LENGTH;
import static libsidplay.config.IWhatsSidSystemProperties.UPLOAD_MAXIMUM_DURATION;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "wav")
public class WAVBean {

	private byte[] wav;

	private long frameMaxLength;

	public WAVBean() {
		this(null, false);
	}

	public WAVBean(byte[] wav) {
		this(wav, false);
	}

	public WAVBean(byte[] wav, boolean fileUpload) {
		this.wav = wav;
		if (fileUpload) {
			frameMaxLength = (long) UPLOAD_MAXIMUM_DURATION * VERY_LOW.getFrequency();
		} else {
			frameMaxLength = FRAME_MAX_LENGTH;
		}
	}

	public byte[] getWav() {
		return wav;
	}

	@XmlElement(name = "wav")
	public void setWav(byte[] wav) {
		this.wav = wav;
	}

	public long getFrameMaxLength() {
		return frameMaxLength;
	}

	@XmlElement(name = "frameMaxLength")
	public void setFrameMaxLength(long frameMaxLength) {
		this.frameMaxLength = frameMaxLength;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((wav == null) ? 0 : Arrays.hashCode(wav));
		result = prime * result + Long.hashCode(frameMaxLength);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof WAVBean)) {
			return false;
		}
		WAVBean other = (WAVBean) obj;
		return Arrays.equals(wav, other.wav) && frameMaxLength == other.frameMaxLength;
	}
}
