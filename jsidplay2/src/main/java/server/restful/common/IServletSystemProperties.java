package server.restful.common;

import static java.lang.Integer.valueOf;
import static java.lang.System.getProperty;

public interface IServletSystemProperties {

	//
	// JSIDPlay2Server
	//

	/**
	 * Server base (default: http://127.0.0.1:8080)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.base.url
	 */
	static final String BASE_URL = getProperty("jsidplay2.base.url", "http://127.0.0.1:8080");

	/**
	 * JSIDPlay2Server Socket connection timeout in ms of the HTTP(s) connection
	 * (default: 20s)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.connection.timeout
	 */
	int CONNECTION_TIMEOUT = valueOf(getProperty("jsidplay2.connection.timeout", "20000"));

	/**
	 * WhatsSidServlet: Asynchronous servlets default timeout in ms (0 means
	 * disable, default: 60s)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.whatssid.async.timeout
	 */
	int WHATSSID_ASYNC_TIMEOUT = valueOf(getProperty("jsidplay2.whatssid.async.timeout", "60000"));

	/**
	 * ConvertServlet: Asynchronous servlets default timeout in ms (0 means disable,
	 * default: 0s)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.convert.async.timeout
	 */
	int CONVERT_ASYNC_TIMEOUT = valueOf(getProperty("jsidplay2.convert.async.timeout", "0"));

	/**
	 * Upload: Asynchronous servlets default timeout in ms (0 means disable,
	 * default: 0s)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.upload.async.timeout
	 */
	int UPLOAD_ASYNC_TIMEOUT = valueOf(getProperty("jsidplay2.upload.async.timeout", "0"));

	/**
	 * Time in s to check for obsolete directories to delete (default: 300s)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.cleanup.directory.period
	 */
	int CLEANUP_DIRECTORY_PERIOD = valueOf(getProperty("jsidplay2.cleanup.directory.period", "300"));

	/**
	 * Time in s after obsolete temporary directories to delete (default: 2hrs)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.cleanup.directory.maximum.duration
	 */
	int MAXIMUM_DURATION_TEMP_DIRECTORIES = valueOf(
			getProperty("jsidplay2.cleanup.directory.maximum.duration", String.valueOf(2 * 60 * 60)));

	/**
	 * JSIDPlay2Server uses HTTP/2 (or HTTP/1.1) protocol (default: true)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.protocol.h2.enable
	 */
	boolean USE_HTTP2 = Boolean.valueOf(getProperty("jsidplay2.protocol.h2.enable", "true"));

	/**
	 * The time, in milliseconds, that Tomcat will wait for additional data when a
	 * partial HTTP/2 frame has been received (default: 10min). Negative values will
	 * be treated as an infinite timeout
	 * 
	 * @jsidplay2.systemProperty jsidplay2.protocol.h2.read.timeout
	 */
	int HTTP2_READ_TIMEOUT = valueOf(getProperty("jsidplay2.protocol.h2.read.timeout", String.valueOf(10 * 60 * 1000)));

	/**
	 * The time, in milliseconds, that Tomcat will wait to write additional data
	 * when an HTTP/2 frame has been partially written (default: 60min). Negative
	 * values will be treated as an infinite timeout
	 * 
	 * @jsidplay2.systemProperty jsidplay2.protocol.h2.write.timeout
	 */
	int HTTP2_WRITE_TIMEOUT = valueOf(
			getProperty("jsidplay2.protocol.h2.write.timeout", String.valueOf(60 * 60 * 1000)));

	/**
	 * The time, in milliseconds, that Tomcat will wait between HTTP/2 frames when
	 * there is no active Stream before closing the connection (default: 60min).
	 * Negative values will be treated as an infinite timeout
	 * 
	 * @jsidplay2.systemProperty jsidplay2.protocol.h2.keepalive.timeout
	 */
	int HTTP2_KEEP_ALIVE_TIMEOUT = valueOf(
			getProperty("jsidplay2.protocol.h2.keepalive.timeout", String.valueOf(60 * 60 * 1000)));

	/**
	 * The HTTP/2 protocol may use compression in an attempt to save server
	 * bandwidth (default: on). The acceptable values for the parameter is "off"
	 * (disable compression), "on" (allow compression, which causes text data to be
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
	 * the default conf/web.xml or in the web.xml of your web application
	 * 
	 * @jsidplay2.systemProperty jsidplay2.protocol.h2.compression
	 */
	String COMPRESSION = getProperty("jsidplay2.protocol.h2.compression", "on");

	/**
	 * Use this boolean attribute to enable or disable sendfile capability (default:
	 * false). The default value is true.
	 * 
	 * This setting is ignored, and the sendfile capability disabled, if the
	 * useAsyncIO attribute of the associated Connector is set to false.
	 * 
	 * The HTTP/2 sendfile capability uses MappedByteBuffer which is known to cause
	 * file locking on Windows
	 * 
	 * @jsidplay2.systemProperty jsidplay2.protocol.h2.sendfile
	 */
	boolean HTTP2_USE_SENDFILE = Boolean.valueOf(getProperty("jsidplay2.protocol.h2.sendfile", "false"));

	/**
	 * The factor to apply when counting overhead frames to determine if a
	 * connection has too high an overhead and should be closed (default: 10). The
	 * overhead count starts at -10 * overheadCountFactor. The count is decreased by
	 * 20 for each data frame sent or received and each headers frame received. The
	 * count is increased by the overheadCountFactor for each setting received,
	 * priority frame received and ping received. If the overhead count exceeds
	 * zero, the connection is closed. A value of less than 1 disables this
	 * protection. In normal usage a value of approximately 20 or higher will close
	 * the connection before any streams can complete. If not specified, a default
	 * value of 10 will be used
	 * 
	 * @jsidplay2.systemProperty jsidplay2.protocol.h2.overhead.count.factor
	 */
	int HTTP2_OVERHEAD_COUNT_FACTOR = valueOf(
			getProperty("jsidplay2.protocol.h2.overhead.count.factor", String.valueOf(10)));

	/**
	 * The threshold below which the average payload size of the current and
	 * previous non-final DATA frames will trigger an increase in the overhead count
	 * (see overheadCountFactor, default: 1024). The overhead count will be
	 * increased by overheadDataThreshold/average so that the smaller the average,
	 * the greater the increase in the overhead count. A value of zero or less
	 * disables the checking of non-final DATA frames. If not specified, a default
	 * value of 1024 will be used
	 * 
	 * @jsidplay2.systemProperty jsidplay2.protocol.h2.overhead.data.threshold
	 */
	int HTTP2_OVERHEAD_DATA_THRESHOLD = valueOf(
			getProperty("jsidplay2.protocol.h2.overhead.data.threshold", String.valueOf(1024)));

	/**
	 * The threshold below which the average size of current and previous
	 * WINDOW_UPDATE frame will trigger an increase in the overhead count (see
	 * overheadCountFactor, default: 1024). The overhead count will be increased by
	 * overheadWindowUpdateThreshold/average so that the smaller the average, the
	 * greater the increase in the overhead count. A value of zero or less disables
	 * the checking of WINDOW_UPDATE frames. If not specified, a default value of
	 * 1024 will be used
	 * 
	 * @jsidplay2.systemProperty jsidplay2.protocol.h2.overhead.window.update.threshold
	 */
	int HTTP2_OVERHEAD_WINDOW_UPDATE_THRESHOLD = valueOf(
			getProperty("jsidplay2.protocol.h2.overhead.window.update.threshold", String.valueOf(1024)));

	/**
	 * Servlet response header to set the maximum age of cacheable resources in s
	 * (default: public, max-age=86400)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.whatssid.cache_control.response.header.cached
	 */
	String CACHE_CONTROL_RESPONSE_HEADER_CACHED = getProperty("jsidplay2.whatssid.cache_control.response.header.cached",
			"public, max-age=86400");
	/**
	 * Servlet response header to disable caching for non-cacheable resources
	 * (default: private, no-store, no-cache, must-revalidate)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.whatssid.cache_control.response.header.uncached
	 */
	String CACHE_CONTROL_RESPONSE_HEADER_UNCACHED = getProperty(
			"jsidplay2.whatssid.cache_control.response.header.uncached",
			"private, no-store, no-cache, must-revalidate");

	//
	// ConvertServlet
	//

	/**
	 * Minimum time in ms between requests of StartPageServlet (default: 500ms)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.start_page.min.time.between.requests
	 */
	int MIN_TIME_BETWEEN_REQUESTS = valueOf(getProperty("jsidplay2.start_page.min.time.between.requests", "500"));

	/**
	 * Maximum requests per minute of StartPageServlet (default: 30)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.start_page.max.requests.per.minute
	 */
	int MAX_REQUESTS_PER_MINUTE = valueOf(getProperty("jsidplay2.start_page.max.requests.per.minute", "30"));

	/**
	 * Maximum number of ConvertServlet requests in parallel (default: 7)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.convert.max.parallel
	 */
	int MAX_CONVERT_IN_PARALLEL = valueOf(getProperty("jsidplay2.convert.max.parallel", "7"));

	/**
	 * Video streaming: Interval between simulated key presses of the space key in s
	 * required to watch some demos (0 means no key presses, default: 90s)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.rtmp.press_space_intervall
	 */
	int PRESS_SPACE_INTERVALL = valueOf(getProperty("jsidplay2.rtmp.press_space_intervall", "90"));

	/**
	 * Retry count to wait for the video generation being started (default: 40)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.video.wait.for.retry.count
	 */
	int WAIT_FOR_VIDEO_AVAILABLE_RETRY_COUNT = valueOf(getProperty("jsidplay2.video.wait.for.retry.count", "40"));

	/**
	 * Time span to wait between HLS keep alive notifications in milliseconds
	 * (default: 3s)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.hls.notify.for
	 */
	int NOTIFY_FOR_HLS = valueOf(getProperty("jsidplay2.hls.notify.for", "3000"));

	/**
	 * Video streaming: RTMP Live stream created but not yet played will be quit
	 * after timeout in s (default: 30s)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.rtmp.not_yet_played.timeout
	 */
	int RTMP_NOT_YET_PLAYED_TIMEOUT = valueOf(getProperty("jsidplay2.rtmp.not_yet_played.timeout", "30"));

	/**
	 * Video streaming: HLS Live stream created but not yet received keep-alive
	 * notification will be quit after timeout in s (default: 15s)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.hls.not_yet_played.timeout
	 */
	int HLS_NOT_YET_PLAYED_TIMEOUT = valueOf(getProperty("jsidplay2.hls.not_yet_played.timeout", "15"));

	/**
	 * Video streaming: Live stream played and exceeds maximum duration will be quit
	 * after timeout in s (default: 60min). This is to prevent endless video
	 * generation.
	 * 
	 * @jsidplay2.systemProperty jsidplay2.rtmp.exceeds_maximum.duration
	 */
	int RTMP_EXCEEDS_MAXIMUM_DURATION = valueOf(getProperty("jsidplay2.rtmp.exceeds_maximum.duration", "3600"));

	/**
	 * Video streaming: Time in s to print out currently generated RTMP video
	 * streams (default: 30s)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.rtmp.print.player.period
	 */
	int RTMP_PRINT_PLAYER_PERIOD = valueOf(getProperty("jsidplay2.rtmp.print.player.period", "30"));

	/**
	 * Video streaming: Upload for the video creation process for example an nginx
	 * endpoint (default: rtmp://haendel.ddns.net/live)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.rtmp.upload.url
	 */
	String RTMP_UPLOAD_URL = getProperty("jsidplay2.rtmp.upload.url", "rtmp://haendel.ddns.net/live");

	/**
	 * Video streaming: RTMP Download for the currently generated video live stream
	 * via RTMP protocol (default: rtmp://haendel.ddns.net/live)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.rtmp.external.download.url
	 */
	String RTMP_DOWNLOAD_URL = getProperty("jsidplay2.rtmp.external.download.url", "rtmp://haendel.ddns.net/live");

	/**
	 * Video streaming: HLS Download for the currently generated video live stream
	 * via HLS protocol (default: http://haendel.ddns.net:90/hls)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.hls.external.download.url
	 */
	String HLS_DOWNLOAD_URL = getProperty("jsidplay2.hls.external.download.url", "http://haendel.ddns.net:90/hls");

	/**
	 * Video download: Maximum length in seconds the video stream can run (default:
	 * 15min)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.video.max_seconds
	 */
	int MAX_VID_DOWNLOAD_LENGTH = valueOf(getProperty("jsidplay2.video.max_seconds", "900"));

	/**
	 * Audio download: Maximum length in seconds the audio stream can run (default:
	 * 5min)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.audio.max_seconds
	 */
	int MAX_AUD_DOWNLOAD_LENGTH = valueOf(getProperty("jsidplay2.audio.max_seconds", "300"));

	//
	// WhatsSIDServlet
	//

	/**
	 * WhatsSID first serve RTMP requests, disable WhatsSID requests meanwhile
	 * (default: true)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.whatssid.low_prio
	 */
	boolean WHATSID_LOW_PRIO = Boolean.valueOf(getProperty("jsidplay2.whatssid.low_prio", "true"));

	/**
	 * WhatsSID maximum number of requests in parallel (default: 3)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.whatssid.max.parallel
	 */
	int MAX_WHATSIDS_IN_PARALLEL = valueOf(getProperty("jsidplay2.whatssid.max.parallel", "3"));

	/**
	 * Uploads maximum number of requests in parallel (default: 7)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.upload.max.parallel
	 */
	int MAX_UPLOADS_IN_PARALLEL = valueOf(getProperty("jsidplay2.upload.max.parallel", "7"));

	/**
	 * WhatsSID cache size (default: 60000). Recognized audio is cached for repeated
	 * requests for performance reasons
	 * 
	 * @jsidplay2.systemProperty jsidplay2.whatssid.cache.size
	 */
	int CACHE_SIZE = valueOf(getProperty("jsidplay2.whatssid.cache.size", "60000"));

	/**
	 * Show uncaught exceptions with full exception stack trace (default: false)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.uncaught.exceptionhandler.exceptions
	 */
	boolean UNCAUGHT_EXCEPTION_HANDLER_EXCEPTIONS = Boolean
			.valueOf(getProperty("jsidplay2.uncaught.exceptionhandler.exceptions", "false"));

	/**
	 * Experimental: Text to speech to announce tunes (default: true)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.audio.text2speech
	 */
	boolean TEXT_TO_SPEECH = Boolean.valueOf(getProperty("jsidplay2.audio.text2speech", "true"));

	/**
	 * SpeechToText Maximum number of requests in parallel (default: 7)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.speech2text.max.parallel
	 */
	int MAX_SPEECH_TO_TEXT = valueOf(getProperty("jsidplay2.speech2text.max.parallel", "7"));

	/**
	 * Upload servlet: Maximum request size in bytes (default: 17mb)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.uploadservlet.max.request.size
	 */
	long UPLOADSERVLET_MAX_REQUEST_SIZE = 17 << 10 << 10;

	/**
	 * Upload servlet: Maximum file size in bytes (default: 17mb)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.uploadservlet.max.file.size
	 */
	long UPLOADSERVLET_MAX_FILE_SIZE = 17 << 10 << 10;

	/**
	 * Upload servlet: File size threshold at which the file will be written to the
	 * disk (default: 0)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.uploadservlet.file.size.threshold
	 */
	int UPLOADSERVLET_FILE_SIZE_THRESHOLD = 0;

	/**
	 * WhatsSID servlet: Maximum request size in bytes (default: unlimited)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.whatssidservlet.max.request.size
	 */
	long WHATSSIDSERVLET_MAX_REQUEST_SIZE = -1L;

	/**
	 * WhatsSID servlet: Maximum file size in bytes (default: unlimited)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.whatssidservlet.max.file.size
	 */
	long WHATSSIDSERVLET_MAX_FILE_SIZE = -1L;

	/**
	 * WhatsSID servlet: File size threshold at which the file will be written to
	 * the disk (default: 0)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.whatssidservlet.file.size.threshold
	 */
	int WHATSSIDSERVLET_FILE_SIZE_THRESHOLD = 0;

}