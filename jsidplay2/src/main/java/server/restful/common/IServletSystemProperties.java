package server.restful.common;

import static java.lang.Integer.valueOf;
import static java.lang.System.getProperty;

public interface IServletSystemProperties {

	//
	// JSIDPlay2Server
	//

	/**
	 * Server base
	 */
	String BASE_URL = getProperty("jsidplay2.base.url", "http://127.0.0.1:8080");

	/**
	 * JSIDPlay2Server Socket connection timeout in ms of the HTTP(s) connection.
	 */
	int CONNECTION_TIMEOUT = valueOf(getProperty("jsidplay2.connection.timeout", "20000"));

	/**
	 * JSIDPlay2Server cache control max age of static resources in s.
	 */
	int STATIC_RES_MAX_AGE = valueOf(getProperty("jsidplay2.cache.max.age", String.valueOf(30 * 24 * 60 * 60)));

	//
	// ConvertServlet
	//

	/**
	 * Maximum number of requests in parallel.
	 */
	int MAX_CONVERT_IN_PARALLEL = valueOf(getProperty("jsidplay2.convert.max.parallel", "7"));

	/**
	 * Video streaming: Maximum number of RTMP threads in parallel.
	 */
	int MAX_RTMP_IN_PARALLEL = valueOf(getProperty("jsidplay2.rtmp.max.parallel", "7"));

	/**
	 * Video streaming: Interval between simulated key presses of the space key in s
	 * (required to watch some demos). 0 means no key presses.
	 */
	int PRESS_SPACE_INTERVALL = valueOf(getProperty("jsidplay2.rtmp.press_space_intervall", "90"));

	/**
	 * Time span to wait until RTMP is available after requesting it in
	 * milliseconds.
	 */
	int WAIT_FOR_RTMP = valueOf(getProperty("jsidplay2.rtmp.wait.for", "1000"));

	/**
	 * Time span to wait until HLS is available after requesting it in milliseconds.
	 */
	int WAIT_FOR_HLS = valueOf(getProperty("jsidplay2.hls.wait.for", "4000"));

	/**
	 * Time span to wait between HLS keep alive notifications in milliseconds.
	 */
	int NOTIFY_FOR_HLS = valueOf(getProperty("jsidplay2.hls.notify.for", "3000"));

	/**
	 * Video streaming: RTMP Live stream created but not yet played will be quit
	 * after timeout in s.
	 */
	int RTMP_NOT_YET_PLAYED_TIMEOUT = valueOf(getProperty("jsidplay2.rtmp.not_yet_played.timeout", "30"));

	/**
	 * Video streaming: HLS Live stream created but not yet received keep-alive
	 * notification will be quit after timeout in s.
	 */
	int HLS_NOT_YET_PLAYED_TIMEOUT = valueOf(getProperty("jsidplay2.rtmp.not_yet_played.timeout", "10"));

	/**
	 * Video streaming: Live stream played and exceeds maximum duration will be quit
	 * after timeout in s. This is to prevent endless video generation.
	 */
	int RTMP_EXCEEDS_MAXIMUM_DURATION = valueOf(getProperty("jsidplay2.rtmp.exceeds_maximum.duration", "3600"));

	/**
	 * Video streaming: Time in s to print out currently generated RTMP video
	 * streams.
	 */
	int RTMP_CLEANUP_PLAYER_COUNTER = valueOf(getProperty("jsidplay2.rtmp.cleanup.player.period", "30"));

	/**
	 * Video streaming: Upload for the video creation process (for example an nginx
	 * endpoint).
	 */
	String RTMP_UPLOAD_URL = getProperty("jsidplay2.rtmp.upload.url", "rtmp://haendel.ddns.net/live");

	/**
	 * Video streaming: RTMP Download for the currently generated video live stream
	 * via RTMP protocol.
	 */
	String RTMP_DOWNLOAD_URL = getProperty("jsidplay2.rtmp.external.download.url", "rtmp://haendel.ddns.net/live");

	/**
	 * Video streaming: HLS Download for the currently generated video live stream
	 * via HLS protocol.
	 * 
	 */
	String HLS_DOWNLOAD_URL = getProperty("jsidplay2.hls.external.download.url", "http://haendel.ddns.net:90/hls");

	/**
	 * Video download: Maximum length in seconds the video stream can run.
	 */
	int MAX_VID_DOWNLOAD_LENGTH = valueOf(getProperty("jsidplay2.video.max_seconds", "900"));

	/**
	 * Audio download: Maximum length in seconds the audio stream can run.
	 */
	int MAX_AUD_DOWNLOAD_LENGTH = valueOf(getProperty("jsidplay2.audio.max_seconds", "300"));

	//
	// WhatsSIDServlet
	//

	/**
	 * WhatsSID? First serve RTMP requests, disable WhatsSID requests meanwhile.
	 */
	boolean WHATSID_LOW_PRIO = Boolean.valueOf(getProperty("jsidplay2.whatssid.low_prio", "false"));

	/**
	 * WhatsSID? Maximum number of requests in parallel.
	 */
	int MAX_WHATSIDS_IN_PARALLEL = valueOf(getProperty("jsidplay2.whatssid.max.parallel", "7"));

	/**
	 * WhatsSID? Maximum duration in seconds used to recognize a tune for file
	 * upload.
	 */
	int UPLOAD_MAXIMUM_DURATION = valueOf(getProperty("jsidplay2.whatssid.upload.max.duration", "120"));

	/**
	 * WhatsSID? Cache size. Recognized audio is cached for repeated requests for
	 * performance reasons.
	 */
	int CACHE_SIZE = valueOf(getProperty("jsidplay2.whatssid.cache.size", "60000"));

}