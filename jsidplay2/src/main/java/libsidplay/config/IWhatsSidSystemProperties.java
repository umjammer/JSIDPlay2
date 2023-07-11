package libsidplay.config;

import static java.lang.String.valueOf;
import static java.lang.System.getProperty;
import static libsidplay.common.SamplingRate.VERY_LOW;

/**
 * Some system properties to fine tune JSIDPlay2 without touching the
 * configuration.
 * 
 * @author ken
 *
 */
public interface IWhatsSidSystemProperties {

	/**
	 * WhatsSID? Maximum number of audio frames used to recognize a tune.
	 */
	long FRAME_MAX_LENGTH = Long.valueOf(
			System.getProperty("jsidplay2.whatssid.frame.max.length", valueOf(15/* s */ * VERY_LOW.getFrequency())));

	/**
	 * WhatsSID? Maximum duration in seconds used to recognize a tune for file
	 * upload.
	 */
	int UPLOAD_MAXIMUM_DURATION = Integer.valueOf(getProperty("jsidplay2.whatssid.upload.max.duration", "120"));

	/**
	 * WhatsSID? Query timeout in ms of tune recognition's findHashes query to
	 * prevent blocking database connections during database startup.
	 */
	int QUERY_TIMEOUT = Integer
			.valueOf(System.getProperty("jsidplay2.whatssid.query.timeout", valueOf(30/* s */ * 1000)));

	/**
	 * RecordingTool: maximum number of days to wait for all threads to complete
	 * recording of tunes in parallel.
	 */
	int AWAIT_TERMINATION = Integer.valueOf(System.getProperty("jsidplay2.whatssid.await.termination", valueOf(30)));
}
