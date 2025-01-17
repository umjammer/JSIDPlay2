package ui.entities.whatssid;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.Transient;

import libsidutils.fingerprinting.rest.beans.MusicInfoBean;
import sidplay.ini.converter.BeanToStringConverter;

@Entity
@Access(AccessType.PROPERTY)
@Table(name = "MusicInfo", indexes = { @Index(columnList = "songNo", name = "songNo", unique = false) })
public class MusicInfo {

	private int idMusicInfo;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "idMusicInfo", nullable = false)
	public int getIdMusicInfo() {
		return idMusicInfo;
	}

	public void setIdMusicInfo(int idMusicInfo) {
		this.idMusicInfo = idMusicInfo;
	}

	private Integer songNo;

	@Column(name = "songNo")
	public Integer getSongNo() {
		return songNo;
	}

	public void setSongNo(Integer songNo) {
		this.songNo = songNo;
	}

	private String title;

	@Column(name = "Title", nullable = false)
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	private String artist;

	@Column(name = "Artist", nullable = false)
	public String getArtist() {
		return artist;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	private String album;

	@Column(name = "Album", nullable = false)
	public String getAlbum() {
		return album;
	}

	public void setAlbum(String album) {
		this.album = album;
	}

	private String fileDir;

	@Column(name = "FileDir")
	public String getFileDir() {
		return fileDir;
	}

	public void setFileDir(String fileDir) {
		this.fileDir = fileDir;
	}

	private String infoDir;

	@Column(name = "InfoDir")
	public String getInfoDir() {
		return infoDir;
	}

	public void setInfoDir(String infoDir) {
		this.infoDir = infoDir;
	}

	private double audioLength;

	@Column(name = "audio_length")
	public double getAudioLength() {
		return audioLength;
	}

	public void setAudioLength(double audioLength) {
		this.audioLength = audioLength;
	}

	@Transient
	public final MusicInfoBean toBean() {
		MusicInfoBean result = new MusicInfoBean();
		result.setSongNo(songNo);
		result.setTitle(title);
		result.setArtist(artist);
		result.setAlbum(album);
		result.setFileDir(fileDir);
		result.setInfoDir(infoDir);
		result.setAudioLength(audioLength);
		return result;
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}
}
