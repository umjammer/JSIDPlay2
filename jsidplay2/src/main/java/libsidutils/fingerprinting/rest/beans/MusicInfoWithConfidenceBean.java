package libsidutils.fingerprinting.rest.beans;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement(name = "musicInfoWithConfidence")
@XmlType(propOrder = { "musicInfo", "confidence", "relativeConfidence", "offsetSeconds", "offset" })
public class MusicInfoWithConfidenceBean {

	private MusicInfoBean musicInfo;
	private double relativeConfidence, offsetSeconds;
	private int confidence, offset;

	public MusicInfoBean getMusicInfo() {
		return musicInfo;
	}

	@XmlElement(name = "musicInfo")
	public void setMusicInfo(MusicInfoBean musicInfoBean) {
		this.musicInfo = musicInfoBean;
	}

	public int getConfidence() {
		return confidence;
	}

	@XmlElement(name = "confidence")
	public void setConfidence(int confidence) {
		this.confidence = confidence;
	}

	public double getRelativeConfidence() {
		return relativeConfidence;
	}

	@XmlElement(name = "relativeConfidence")
	public void setRelativeConfidence(double relativeConfidence) {
		this.relativeConfidence = relativeConfidence;
	}

	public int getOffset() {
		return offset;
	}

	@XmlElement(name = "offset")
	public void setOffset(int offset) {
		this.offset = offset;
	}

	public double getOffsetSeconds() {
		return offsetSeconds;
	}

	@XmlElement(name = "offsetSeconds")
	public void setOffsetSeconds(double offsetSeconds) {
		this.offsetSeconds = offsetSeconds;
	}

	@Override
	public int hashCode() {
		assert false : "hashCode not designed";
		return 42;
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof MusicInfoWithConfidenceBean)) {
			return false;
		}
		MusicInfoWithConfidenceBean otherMusicInfoWithConfidence = (MusicInfoWithConfidenceBean) obj;

		// No matter the confidence, if metadata matches then the tune matches
		return musicInfo.equals(otherMusicInfoWithConfidence.getMusicInfo());
	}

	@Override
	public String toString() {
		return String.format("WhatsSID? %s\n\t%.2f%% - %d# [%d - %.2fs]\n", musicInfo, relativeConfidence, confidence,
				offset, offsetSeconds);
	}

}
