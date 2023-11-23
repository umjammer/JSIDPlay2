package libsidplay.config;

import static java.lang.String.valueOf;
import static java.lang.System.getProperty;

/**
 * Some system properties to fine tune JSIDPlay2 without touching the
 * configuration.
 * 
 * @author ken
 *
 */
public interface IWhatsSidSystemProperties {

	/**
	 * WhatsSID maximum number of seconds used to recognize a tune (default: 15s)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.whatssid.max.seconds
	 */
	long MAX_SECONDS = Long.valueOf(System.getProperty("jsidplay2.whatssid.max.seconds", valueOf(15/* s */)));

	/**
	 * WhatsSID maximum number of seconds used to recognize an uploaded tune
	 * (default: 45s)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.whatssid.upload.max.seconds
	 */
	long UPLOAD_MAX_SECONDS = Long
			.valueOf(getProperty("jsidplay2.whatssid.upload.max.seconds", valueOf(45/* s */)));

	/**
	 * WhatsSID query timeout in ms of tune recognition's findHashes query to
	 * prevent blocking database connections during database startup (default:
	 * 120s).
	 * 
	 * @jsidplay2.systemProperty jsidplay2.whatssid.query.timeout
	 */
	int QUERY_TIMEOUT = Integer
			.valueOf(System.getProperty("jsidplay2.whatssid.query.timeout", valueOf(120/* s */ * 1000)));

	/**
	 * RecordingTool maximum number of days to wait for all threads to complete
	 * recording of tunes in parallel (default: 30days)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.whatssid.await.termination
	 */
	int AWAIT_TERMINATION = Integer.valueOf(System.getProperty("jsidplay2.whatssid.await.termination", valueOf(30)));
}
