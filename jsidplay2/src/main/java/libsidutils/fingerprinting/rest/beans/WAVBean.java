package libsidutils.fingerprinting.rest.beans;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.annotation.JsonIgnore;

@XmlRootElement(name = "wav")
public class WAVBean {

	private byte[] wav;

	private long maxSeconds;

	public WAVBean() {
	}

	public WAVBean(byte[] wav) {
		this.wav = wav;
	}

	public byte[] getWav() {
		return wav;
	}

	@XmlElement(name = "wav")
	public void setWav(byte[] wav) {
		this.wav = wav;
	}

	public long getMaxSeconds() {
		return maxSeconds;
	}

	@XmlTransient
	@JsonIgnore
	public void setMaxSeconds(long maxLength) {
		this.maxSeconds = maxLength;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((wav == null) ? 0 : Arrays.hashCode(wav));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof WAVBean)) {
			return false;
		}
		WAVBean other = (WAVBean) obj;
		return Arrays.equals(wav, other.wav);
	}
}
