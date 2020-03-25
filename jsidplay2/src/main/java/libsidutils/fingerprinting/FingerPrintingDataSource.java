package libsidutils.fingerprinting;

import java.io.IOException;

import libsidutils.fingerprinting.rest.beans.HashBeans;
import libsidutils.fingerprinting.rest.beans.IdBean;
import libsidutils.fingerprinting.rest.beans.IntArrayBean;
import libsidutils.fingerprinting.rest.beans.MusicInfoBean;
import libsidutils.fingerprinting.rest.beans.MusicInfoWithConfidenceBean;
import libsidutils.fingerprinting.rest.beans.SongNoBean;
import libsidutils.fingerprinting.rest.beans.WavBean;

public interface FingerPrintingDataSource {

	IdBean insertTune(MusicInfoBean musicInfoBean);

	void insertHashes(HashBeans hashBeans);

	HashBeans findHashes(IntArrayBean intArray);

	MusicInfoBean findTune(SongNoBean songNoBean);

	MusicInfoWithConfidenceBean whatsSid(WavBean wavBean) throws IOException;

	boolean tuneExists(MusicInfoBean musicInfoBean);

}
