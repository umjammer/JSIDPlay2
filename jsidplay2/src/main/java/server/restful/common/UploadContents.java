package server.restful.common;

import java.util.Arrays;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "contents")
public class UploadContents {

	private byte[] contents;

	public UploadContents() {
		this(null);
	}

	public UploadContents(byte[] contents) {
		this.contents = contents;
	}

	public byte[] getContents() {
		return contents;
	}

	@XmlElement(name = "contents")
	public void setWav(byte[] contents) {
		this.contents = contents;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((contents == null) ? 0 : Arrays.hashCode(contents));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof UploadContents)) {
			return false;
		}
		UploadContents other = (UploadContents) obj;
		return Arrays.equals(contents, other.contents);
	}
}
