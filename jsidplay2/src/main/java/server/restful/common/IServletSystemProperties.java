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
	 * Time in s to check for obsolete directories to delete
	 */
	int CLEANUP_DIRECTORY_PERIOD = valueOf(getProperty("jsidplay2.cleanup.directory.period", "300"));

	/**
	 * Time in s after obsolete temporary directories to delete
	 */
	int MAXIMUM_DURATION_TEMP_DIRECTORIES = valueOf(
			getProperty("jsidplay2.cleanup.directory.maximum.duration", String.valueOf(2 * 60 * 60)));

	/**
	 * Prefix for HTTP2 settings
	 */
	String H2 = "jsidplay2.protocol.h2";

	/**
	 * JSIDPlay2Server uses HTTP/2 (or HTTP/1.1) protocol.
	 */
	boolean USE_HTTP2 = Boolean.valueOf(getProperty(H2 + ".enable", "true"));

	/**
	 * The time, in milliseconds, that Tomcat will wait for additional data when a
	 * partial HTTP/2 frame has been received. Negative values will be treated as an
	 * infinite timeout.
	 */
	int HTTP2_READ_TIMEOUT = valueOf(getProperty(H2 + ".read.timeout", String.valueOf(10 * 60 * 1000)));

	/**
	 * The time, in milliseconds, that Tomcat will wait to write additional data
	 * when an HTTP/2 frame has been partially written. Negative values will be
	 * treated as an infinite timeout.
	 */
	int HTTP2_WRITE_TIMEOUT = valueOf(getProperty(H2 + ".write.timeout", String.valueOf(60 * 60 * 1000)));

	/**
	 * The time, in milliseconds, that Tomcat will wait between HTTP/2 frames when
	 * there is no active Stream before closing the connection. Negative values will
	 * be treated as an infinite timeout.
	 */
	int HTTP2_KEEP_ALIVE_TIMEOUT = valueOf(getProperty(H2 + ".keepalive.timeout", String.valueOf(60 * 60 * 1000)));

	/**
	 * The HTTP/2 protocol may use compression in an attempt to save server
	 * bandwidth. The acceptable values for the parameter is "off" (disable
	 * compression), "on" (allow compression, which causes text data to be
	 * compressed), "force" (forces compression in all cases), or a numerical
	 * integer value (which is equivalent to "on", but specifies the minimum amount
	 * of data before the output is compressed). If the content-length is not known
	 * and compression is set to "on" or more aggressive, the output will also be
	 * compressed. If not specified, this attribute is set to "off".
	 * 
	 * Note: There is a tradeoff between using compression (saving your bandwidth)
	 * and using the sendfile feature (saving your CPU cycles). If the connector
	 * supports the sendfile feature, e.g. the NIO2 connector, using sendfile will
	 * take precedence over compression. The symptoms will be that static files
	 * greater that 48 Kb will be sent uncompressed. You can turn off sendfile by
	 * setting useSendfile attribute of the protocol, as documented below, or change
	 * the sendfile usage threshold in the configuration of the DefaultServlet in
	 * the default conf/web.xml or in the web.xml of your web application.
	 */
	String COMPRESSION = getProperty(H2 + ".compression", "on");

	/**
	 * Use this boolean attribute to enable or disable sendfile capability. The
	 * default value is true.
	 * 
	 * This setting is ignored, and the sendfile capability disabled, if the
	 * useAsyncIO attribute of the associated Connector is set to false.
	 * 
	 * The HTTP/2 sendfile capability uses MappedByteBuffer which is known to cause
	 * file locking on Windows.
	 */
	boolean HTTP2_USE_SENDFILE = Boolean.valueOf(getProperty(H2 + ".sendfile", "false"));

	/**
	 * The factor to apply when counting overhead frames to determine if a
	 * connection has too high an overhead and should be closed. The overhead count
	 * starts at -10 * overheadCountFactor. The count is decreased by 20 for each
	 * data frame sent or received and each headers frame received. The count is
	 * increased by the overheadCountFactor for each setting received, priority
	 * frame received and ping received. If the overhead count exceeds zero, the
	 * connection is closed. A value of less than 1 disables this protection. In
	 * normal usage a value of approximately 20 or higher will close the connection
	 * before any streams can complete. If not specified, a default value of 10 will
	 * be used.
	 */
	int HTTP2_OVERHEAD_COUNT_FACTOR = valueOf(getProperty(H2 + ".overhead.count.factor", String.valueOf(10)));

	/**
	 * The threshold below which the average payload size of the current and
	 * previous non-final DATA frames will trigger an increase in the overhead count
	 * (see overheadCountFactor). The overhead count will be increased by
	 * overheadDataThreshold/average so that the smaller the average, the greater
	 * the increase in the overhead count. A value of zero or less disables the
	 * checking of non-final DATA frames. If not specified, a default value of 1024
	 * will be used.
	 */
	int HTTP2_OVERHEAD_DATA_THRESHOLD = valueOf(getProperty(H2 + ".overhead.data.threshold", String.valueOf(1024)));

	/**
	 * The threshold below which the average size of current and previous
	 * WINDOW_UPDATE frame will trigger an increase in the overhead count (see
	 * overheadCountFactor). The overhead count will be increased by
	 * overheadWindowUpdateThreshold/average so that the smaller the average, the
	 * greater the increase in the overhead count. A value of zero or less disables
	 * the checking of WINDOW_UPDATE frames. If not specified, a default value of
	 * 1024 will be used.
	 */
	int HTTP2_OVERHEAD_WINDOW_UPDATE_THRESHOLD = valueOf(
			getProperty(H2 + ".overhead.window.update.threshold", String.valueOf(1024)));

	/**
	 * JSIDPlay2Server cache control max age of static resources in s.
	 */
	int STATIC_RES_MAX_AGE = valueOf(getProperty("jsidplay2.cache.max.age", String.valueOf(30 * 24 * 60 * 60)));

	//
	// ConvertServlet
	//

	/**
	 * Minimum time in ms between requests of StartPageServlet.
	 */
	int MIN_TIME_BETWEEN_REQUESTS = valueOf(getProperty("jsidplay2.start_page.min.time.between.requests", "200"));

	/**
	 * Maximum number of ConvertServlet requests in parallel.
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
	int WAIT_FOR_HLS = valueOf(getProperty("jsidplay2.hls.wait.for", "5000"));

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
	int HLS_NOT_YET_PLAYED_TIMEOUT = valueOf(getProperty("jsidplay2.hls.not_yet_played.timeout", "10"));

	/**
	 * Video streaming: Live stream played and exceeds maximum duration will be quit
	 * after timeout in s. This is to prevent endless video generation.
	 */
	int RTMP_EXCEEDS_MAXIMUM_DURATION = valueOf(getProperty("jsidplay2.rtmp.exceeds_maximum.duration", "3600"));

	/**
	 * Video streaming: Time in s to print out currently generated RTMP video
	 * streams.
	 */
	int RTMP_PRINT_PLAYER_PERIOD = valueOf(getProperty("jsidplay2.rtmp.print.player.period", "30"));

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
	boolean WHATSID_LOW_PRIO = Boolean.valueOf(getProperty("jsidplay2.whatssid.low_prio", "true"));

	/**
	 * WhatsSID? Maximum number of requests in parallel.
	 */
	int MAX_WHATSIDS_IN_PARALLEL = valueOf(getProperty("jsidplay2.whatssid.max.parallel", "7"));

	/**
	 * WhatsSID? Cache size. Recognized audio is cached for repeated requests for
	 * performance reasons.
	 */
	int CACHE_SIZE = valueOf(getProperty("jsidplay2.whatssid.cache.size", "60000"));

}