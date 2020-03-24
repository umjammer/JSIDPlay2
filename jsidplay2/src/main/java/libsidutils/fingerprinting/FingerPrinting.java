package libsidutils.fingerprinting;

import libsidplay.sidtune.SidTune;
import libsidutils.fingerprinting.model.FingerprintedSampleData;
import libsidutils.fingerprinting.model.SongMatch;
import libsidutils.fingerprinting.rest.beans.IdBean;
import libsidutils.fingerprinting.rest.beans.MusicInfoBean;
import libsidutils.fingerprinting.rest.beans.MusicInfoWithConfidenceBean;
import libsidutils.fingerprinting.rest.beans.SongNoBean;
import libsidutils.fingerprinting.rest.beans.WavBean;

public class FingerPrinting {

	private static final int MIN_HIT = 15;

	private FingerPrintingDataSource fingerPrintingDataSource;

	
	public FingerPrinting(FingerPrintingDataSource fingerPrintingDataSource) {
		this.fingerPrintingDataSource = fingerPrintingDataSource;
	}

	public void insert(WavBean wavBean, SidTune tune, String collectionFilename, String recordingFilename) {
		if (wavBean.getWav().length > 0) {
			FingerprintedSampleData fingerprintedSampleData = new FingerprintedSampleData(wavBean);
			fingerprintedSampleData.setMetaInfo(tune, recordingFilename, collectionFilename);
			MusicInfoBean musicInfoBean = fingerprintedSampleData.toMusicInfoBean();

			if (!fingerPrintingDataSource.tuneExists(musicInfoBean)) {
				IdBean id = fingerPrintingDataSource.insertTune(musicInfoBean);
				fingerPrintingDataSource.insertHashes(fingerprintedSampleData.getFingerprint().toHashBeans(id));
			}
		}
	}

	public MusicInfoWithConfidenceBean match(WavBean wavBean) {
		if (wavBean != null && wavBean.getWav().length > 0) {
			FingerprintedSampleData fingerprintedSampleData = new FingerprintedSampleData(wavBean);

			Index index = new Index();
			index.setFingerPrintingClient(fingerPrintingDataSource);
			SongMatch songMatch = index.search(fingerprintedSampleData.getFingerprint(), MIN_HIT);

			if (songMatch != null && songMatch.getIdSong() != -1) {
				SongNoBean songNoBean = new SongNoBean();
				songNoBean.setSongNo(songMatch.getIdSong());
				MusicInfoBean musicInfoBean = fingerPrintingDataSource.findTune(songNoBean);

				MusicInfoWithConfidenceBean result = new MusicInfoWithConfidenceBean();
				result.setMusicInfo(musicInfoBean);
				result.setSongMatch(fingerprintedSampleData, songMatch);

				return result;
			}
		}
		return null;
	}
}
