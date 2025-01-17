package libsidutils.fingerprinting.rest.beans;

import java.util.Objects;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "musicInfo")
@XmlType(propOrder = { "songNo", "title", "artist", "album", "fileDir", "infoDir", "audioLength" })
public class MusicInfoBean {

	private Integer songNo;
	private String title, artist, album, fileDir, infoDir;
	private double audioLength;

	public Integer getSongNo() {
		return songNo;
	}

	@XmlElement(name = "song")
	public void setSongNo(Integer songNo) {
		this.songNo = songNo;
	}

	public String getTitle() {
		return title;
	}

	@XmlElement(name = "title")
	public void setTitle(String title) {
		this.title = title;
	}

	public String getArtist() {
		return artist;
	}

	@XmlElement(name = "artist")
	public void setArtist(String artist) {
		this.artist = artist;
	}

	public String getAlbum() {
		return album;
	}

	@XmlElement(name = "album")
	public void setAlbum(String album) {
		this.album = album;
	}

	public String getFileDir() {
		return fileDir;
	}

	@XmlElement(name = "fileDir")
	public void setFileDir(String fileDir) {
		this.fileDir = fileDir;
	}

	public String getInfoDir() {
		return infoDir;
	}

	@XmlElement(name = "infoDir")
	public void setInfoDir(String infoDir) {
		this.infoDir = infoDir;
	}

	public double getAudioLength() {
		return audioLength;
	}

	@XmlElement(name = "audioLength")
	public void setAudioLength(double audioLength) {
		this.audioLength = audioLength;
	}

	@Override
	public int hashCode() {
		assert false : "hashCode not designed";
		return 42;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MusicInfoBean)) {
			return false;
		}
		MusicInfoBean otherMusicInfo = (MusicInfoBean) obj;

		return Objects.equals(getSongNo(), otherMusicInfo.getSongNo())
				&& Objects.equals(getTitle(), otherMusicInfo.getTitle())
				&& Objects.equals(getArtist(), otherMusicInfo.getArtist())
				&& Objects.equals(getAlbum(), otherMusicInfo.getAlbum())
				&& Objects.equals(getFileDir(), otherMusicInfo.getFileDir())
				&& Objects.equals(getInfoDir(), otherMusicInfo.getInfoDir());
	}

	@Override
	public String toString() {
		return String.format("%s / %s / %s\n\t%s (%d)", title, artist, album, infoDir, songNo);
	}

}
