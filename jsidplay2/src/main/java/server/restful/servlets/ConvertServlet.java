package server.restful.servlets;

import static java.lang.String.format;
import static java.lang.Thread.getAllStackTraces;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.xml.bind.DatatypeConverter.printBase64Binary;
import static libsidutils.PathUtils.getFilenameSuffix;
import static libsidutils.PathUtils.getFilenameWithoutSuffix;
import static libsidutils.ZipFileUtils.convertStreamToString;
import static libsidutils.ZipFileUtils.copy;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.ATTACHMENT;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.CONTENT_DISPOSITION;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_HTML;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.ContentTypeAndFileExtensions.getMimeType;
import static server.restful.common.IServletSystemProperties.HLS_DOWNLOAD_URL;
import static server.restful.common.IServletSystemProperties.MAX_CONVERT_IN_PARALLEL;
import static server.restful.common.IServletSystemProperties.MAX_DOWNLOAD_LENGTH;
import static server.restful.common.IServletSystemProperties.MAX_RTMP_IN_PARALLEL;
import static server.restful.common.IServletSystemProperties.NOTIFY_FOR_HLS;
import static server.restful.common.IServletSystemProperties.PRESS_SPACE_INTERVALL;
import static server.restful.common.IServletSystemProperties.RTMP_DOWNLOAD_URL;
import static server.restful.common.IServletSystemProperties.RTMP_NOT_YET_PLAYED_TIMEOUT;
import static server.restful.common.IServletSystemProperties.RTMP_UPLOAD_URL;
import static server.restful.common.IServletSystemProperties.WAIT_FOR_HLS;
import static server.restful.common.IServletSystemProperties.WAIT_FOR_RTMP;
import static server.restful.common.PlayerCleanupTimerTask.create;
import static server.restful.common.QrCode.createBarCodeImage;
import static sidplay.audio.Audio.AAC;
import static sidplay.audio.Audio.AVI;
import static sidplay.audio.Audio.FLAC;
import static sidplay.audio.Audio.FLV;
import static sidplay.audio.Audio.MP3;
import static sidplay.audio.Audio.MP4;
import static sidplay.audio.Audio.SID_DUMP;
import static sidplay.audio.Audio.SID_REG;
import static sidplay.audio.Audio.WAV;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.http.HttpHeaders;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.google.zxing.WriterException;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidplay.components.cart.CartridgeType;
import libsidplay.config.IC1541Section;
import libsidplay.config.IConfig;
import libsidplay.config.ISidPlay2Section;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import libsidutils.siddatabase.SidDatabase;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.filters.LimitRequestServletFilter;
import sidplay.Player;
import sidplay.audio.AACDriver.AACStreamDriver;
import sidplay.audio.AVIDriver.AVIFileDriver;
import sidplay.audio.Audio;
import sidplay.audio.AudioDriver;
import sidplay.audio.FLACDriver.FLACStreamDriver;
import sidplay.audio.FLVDriver.FLVFileDriver;
import sidplay.audio.FLVDriver.FLVStreamDriver;
import sidplay.audio.MP3Driver.MP3StreamDriver;
import sidplay.audio.MP4Driver.MP4FileDriver;
import sidplay.audio.ProxyDriver;
import sidplay.audio.SIDDumpDriver.SIDDumpStreamDriver;
import sidplay.audio.SIDRegDriver.SIDRegStreamDriver;
import sidplay.audio.SleepDriver;
import sidplay.audio.WAVDriver.WAVStreamDriver;
import sidplay.ini.IniConfig;
import ui.common.Convenience;
import ui.common.filefilter.AudioTuneFileFilter;
import ui.common.filefilter.CartFileFilter;
import ui.common.filefilter.DiskFileFilter;
import ui.common.filefilter.TapeFileFilter;
import ui.common.filefilter.VideoTuneFileFilter;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class ConvertServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.ConvertServletParameters")
	public static class ServletParameters {

		@Parameter(names = { "--startSong" }, descriptionKey = "START_SONG", order = -8)
		private Integer startSong;

		@Parameter(names = "--download", arity = 1, descriptionKey = "DOWNLOAD", order = -7)
		private Boolean download = Boolean.FALSE;

		@Parameter(names = "--jiffydos", arity = 1, descriptionKey = "JIFFYDOS", order = -6)
		private Boolean jiffydos = Boolean.FALSE;

		@Parameter(names = { "--reuSize" }, descriptionKey = "REU_SIZE", order = -5)
		private Integer reuSize;

		@Parameter(names = { "--pressSpaceInterval" }, descriptionKey = "PRESS_SPACE_INTERVAL", order = -4)
		private Integer pressSpaceInterval = PRESS_SPACE_INTERVALL;

		@Parameter(names = "--status", arity = 1, descriptionKey = "STATUS", order = -3)
		private Boolean status = Boolean.TRUE;

		@Parameter(names = "--rtmp", arity = 1, descriptionKey = "RTMP", order = -2)
		private Boolean rtmp = Boolean.TRUE;

		@ParametersDelegate
		private IniConfig config = new IniConfig();

		@Parameter(descriptionKey = "FILE_PATH")
		private String filePath;

		private volatile boolean started;

		public Integer getStartSong() {
			return startSong;
		}

		public Boolean getDownload() {
			return download;
		}

		public Boolean getJiffydos() {
			return jiffydos;
		}

		public Integer getReuSize() {
			return reuSize;
		}

		public Integer getPressSpaceInterval() {
			return pressSpaceInterval;
		}

		public Boolean getStatus() {
			return status;
		}

		public Boolean getRtmp() {
			return rtmp;
		}

		public IniConfig getConfig() {
			return config;
		}
	}

	public static final String CONVERT_PATH = "/convert";

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(ConvertServlet.class.getName());
	private static final AudioTuneFileFilter audioTuneFileFilter = new AudioTuneFileFilter();
	private static final VideoTuneFileFilter videoTuneFileFilter = new VideoTuneFileFilter();
	private static final DiskFileFilter diskFileFilter = new DiskFileFilter();
	private static final TapeFileFilter tapeFileFilter = new TapeFileFilter();
	private static final CartFileFilter cartFileFilter = new CartFileFilter();

	public ConvertServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + CONVERT_PATH;
	}

	@Override
	public Filter createServletFilter() {
		return new LimitRequestServletFilter(MAX_CONVERT_IN_PARALLEL);
	}

	/**
	 * Stream e.g. SID as MP3 or D64 as RTMP video stream.
	 *
	 * <BR>
	 * E.g. stream audio<BR>
	 * {@code
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/convert/C64Music/MUSICIANS/D/DRAX/Worktunes/Outro.sid
	 * } <BR>
	 * E.g. stream video<BR>
	 * {@code
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/convert/Assembly64/Demos/CSDB/Year/2011/Algotech/Algodancer%202/algodancer2.d64?defaultLength=00:30&enableSidDatabase=true&single=true&loop=false&bufferSize=65536&sampling=RESAMPLE&frequency=MEDIUM&defaultEmulation=RESIDFP&defaultModel=MOS8580&filter6581=FilterAlankila6581R4AR_3789&stereoFilter6581=FilterAlankila6581R4AR_3789&thirdFilter6581=FilterAlankila6581R4AR_3789&filter8580=FilterAlankila6581R4AR_3789&stereoFilter8580=FilterAlankila6581R4AR_3789&thirdFilter8580=FilterAlankila6581R4AR_3789&reSIDfpFilter6581=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter6581=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter6581=FilterAlankila6581R4AR_3789&reSIDfpFilter8580=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter8580=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter8580=FilterAlankila6581R4AR_3789&digiBoosted8580=true
	 * }
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doGet(request);
		try {
			final ServletParameters servletParameters = new ServletParameters();

			JCommander commander = parseRequestParameters(request, response, servletParameters, getServletPath());
			if (servletParameters.filePath == null) {
				commander.usage();
				return;
			}
			final IniConfig config = servletParameters.config;
			final File file = getAbsoluteFile(servletParameters.filePath, true/* request.isUserInRole(ROLE_ADMIN) */);

			if (audioTuneFileFilter.accept(file)) {

				Audio audio = getAudioFormat(config);
				AudioDriver driver = getAudioDriverOfAudioFormat(audio, response.getOutputStream());

				if (Boolean.TRUE.equals(servletParameters.download)) {
					response.addHeader(CONTENT_DISPOSITION, ATTACHMENT + "; filename="
							+ getFilenameWithoutSuffix(file.getName()) + driver.getExtension());
				}
				response.setContentType(getMimeType(driver.getExtension()).toString());
				convert2audio(config, file, driver, servletParameters);

			} else if (videoTuneFileFilter.accept(file) || cartFileFilter.accept(file) || diskFileFilter.accept(file)
					|| tapeFileFilter.accept(file)) {

				UUID uuid = UUID.randomUUID();

				Audio audio = getVideoFormat(config);
				AudioDriver driver = getAudioDriverOfVideoFormat(audio, uuid, servletParameters.download);

				if (Boolean.FALSE.equals(servletParameters.download) && audio == FLV) {
					if (getAllStackTraces().keySet().stream().map(Thread::getName).filter("RTMP"::equals)
							.count() < MAX_RTMP_IN_PARALLEL) {
						new Thread(() -> {
							try {
								info("START RTMP stream of: " + uuid);
								convert2video(config, file, driver, servletParameters, uuid);
								info("END RTMP stream of: " + uuid);
							} catch (IOException | SidTuneError e) {
								log("ERROR RTMP stream of: " + uuid, e);
							} finally {
								servletParameters.started = true;
							}
						}, "RTMP").start();
						while (!servletParameters.started) {
							Thread.yield();
						}
						response.setHeader(HttpHeaders.PRAGMA, "no-cache");
						response.setHeader(HttpHeaders.CACHE_CONTROL, "private, no-store, no-cache, must-revalidate");

						Map<String, String> replacements = createReplacements(servletParameters, request, file, uuid);
						try (InputStream is = ConvertServlet.class
								.getResourceAsStream("/server/restful/webapp/convert.html")) {
							setOutput(response, MIME_TYPE_HTML, convertStreamToString(is, UTF_8.name(), replacements));
						}
					} else {
						response.setContentType(MIME_TYPE_TEXT.toString());
						response.sendError(429, "Too Many Requests");
						return;
					}
				} else {

					if (Boolean.TRUE.equals(servletParameters.download)) {
						response.addHeader(CONTENT_DISPOSITION, ATTACHMENT + "; filename="
								+ getFilenameWithoutSuffix(file.getName()) + driver.getExtension());
					}
					response.setContentType(getMimeType(driver.getExtension()).toString());
					File videoFile = convert2video(config, file, driver, servletParameters, null);
					copy(videoFile, response.getOutputStream());
					videoFile.delete();

				}
			} else {
				response.setContentType(getMimeType(getFilenameSuffix(file.getName())).toString());
				response.addHeader(CONTENT_DISPOSITION, ATTACHMENT + "; filename=" + file.getName());
				copy(file, response.getOutputStream());

			}
		} catch (Throwable t) {
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}

	private Audio getAudioFormat(IConfig config) {
		switch (Optional.ofNullable(config.getAudioSection().getAudio()).orElse(MP3)) {
		case WAV:
			return WAV;
		case FLAC:
			return FLAC;
		case AAC:
			return AAC;
		case MP3:
		default:
			return MP3;
		case SID_DUMP:
			return SID_DUMP;
		case SID_REG:
			return SID_REG;
		}
	}

	private AudioDriver getAudioDriverOfAudioFormat(Audio audio, OutputStream outputstream) {
		switch (audio) {
		case WAV:
			return new WAVStreamDriver(outputstream);
		case FLAC:
			return new FLACStreamDriver(outputstream);
		case AAC:
			return new AACStreamDriver(outputstream);
		case MP3:
		default:
			return new MP3StreamDriver(outputstream);
		case SID_DUMP:
			return new SIDDumpStreamDriver(outputstream);
		case SID_REG:
			return new SIDRegStreamDriver(outputstream, true);
		}
	}

	private void convert2audio(IConfig config, File file, AudioDriver driver, ServletParameters servletParameters)
			throws IOException, SidTuneError {
		Player player = new Player(config);
		File root = configuration.getSidplay2Section().getHvsc();
		if (root != null) {
			player.setSidDatabase(new SidDatabase(root));
		}
		player.setAudioDriver(driver);
		player.setUncaughtExceptionHandler((thread, throwable) -> uncaughtExceptionHandler(thread, throwable));
		player.setDefaultLengthInRecordMode(Boolean.TRUE.equals(servletParameters.download));
		player.setCheckLoopOffInRecordMode(Boolean.TRUE.equals(servletParameters.download));
		player.setForceCheckSongLength(true);

		SidTune tune = SidTune.load(file);
		tune.getInfo().setSelectedSong(servletParameters.startSong);
		player.play(tune);
		player.stopC64(false);
	}

	private Audio getVideoFormat(IConfig config) {
		switch (Optional.ofNullable(config.getAudioSection().getAudio()).orElse(FLV)) {
		case FLV:
		default:
			return FLV;
		case AVI:
			return AVI;
		case MP4:
			return MP4;
		}
	}

	private AudioDriver getAudioDriverOfVideoFormat(Audio audio, UUID uuid, Boolean download) {
		switch (audio) {
		case FLV:
		default:
			if (Boolean.TRUE.equals(download)) {
				return new FLVFileDriver();
			} else {
				return new ProxyDriver(new SleepDriver(), new FLVStreamDriver(RTMP_UPLOAD_URL + "/" + uuid));
			}
		case AVI:
			return new AVIFileDriver();
		case MP4:
			return new MP4FileDriver();
		}
	}

	private File convert2video(IConfig config, File file, AudioDriver driver, ServletParameters servletParameters,
			UUID uuid) throws IOException, SidTuneError {
		File videoFile = null;

		ISidPlay2Section sidplay2Section = config.getSidplay2Section();
		IC1541Section c1541Section = config.getC1541Section();

		c1541Section.setJiffyDosInstalled(Boolean.TRUE.equals(servletParameters.jiffydos));

		Player player = new Player(config);
		if (Boolean.TRUE.equals(servletParameters.download)) {
			sidplay2Section.setDefaultPlayLength(Math.min(sidplay2Section.getDefaultPlayLength(), MAX_DOWNLOAD_LENGTH));
			videoFile = createVideoFile(player, driver);
		}
		player.setAudioDriver(driver);
		player.setUncaughtExceptionHandler((thread, throwable) -> uncaughtExceptionHandler(thread, throwable));
		player.setDefaultLengthInRecordMode(Boolean.TRUE.equals(servletParameters.download));
		player.setCheckLoopOffInRecordMode(Boolean.TRUE.equals(servletParameters.download));
		player.setForceCheckSongLength(true);

		new Convenience(player).autostart(file, Convenience.LEXICALLY_FIRST_MEDIA, null);

		if (servletParameters.reuSize != null) {
			player.insertCartridge(CartridgeType.REU, servletParameters.reuSize);
		}
		if (uuid != null) {
			create(uuid, player, file, servletParameters, RESOURCE_BUNDLE);
			servletParameters.started = true;
		}
		player.stopC64(false);
		return videoFile;
	}

	private File createVideoFile(Player player, AudioDriver driver) throws IOException {
		ISidPlay2Section sidplay2Section = player.getConfig().getSidplay2Section();

		File videoFile = File.createTempFile("jsidplay2video", driver.getExtension(), sidplay2Section.getTmpDir());
		videoFile.deleteOnExit();
		player.setRecordingFilenameProvider(tune -> PathUtils.getFilenameWithoutSuffix(videoFile.getAbsolutePath()));
		return videoFile;
	}

	private Map<String, String> createReplacements(ServletParameters servletParameters, HttpServletRequest request,
			File file, UUID uuid) throws IOException, WriterException {
		String rtmpUrl = getRTMPUrl(servletParameters, request.getRemoteAddr(), uuid);
		String createQrCodeImgTag = createQrCodeImgTag(rtmpUrl, "UTF-8", "png", 320, 320);

		Map<String, String> result = new HashMap<>();
		result.put("$uuid", uuid.toString());
		result.put("$barcodeImg", createQrCodeImgTag);
		result.put("$rtmp", rtmpUrl);
		result.put("$hls", String.valueOf(!Boolean.TRUE.equals(servletParameters.rtmp)));
		result.put("$waitForVideo", String.valueOf(getWaitForVideo(servletParameters)));
		result.put("$notYetPlayedTimeout", String.valueOf(RTMP_NOT_YET_PLAYED_TIMEOUT));
		result.put("$notifyForHLS", String.valueOf(NOTIFY_FOR_HLS));
		result.put("$filename", file.getName());
		return result;
	}

	private String createQrCodeImgTag(String data, String charset, String imgFormat, int width, int height)
			throws IOException, WriterException {
		ByteArrayOutputStream qrCodeImgData = new ByteArrayOutputStream();
		ImageIO.write(createBarCodeImage(data, charset, width, height), imgFormat, qrCodeImgData);
		return format("<img src='data:image/%s;base64,%s'>", imgFormat, printBase64Binary(qrCodeImgData.toByteArray()));
	}

	private String getRTMPUrl(ServletParameters servletParameters, String remoteAddress, UUID uuid) {
		if (Boolean.TRUE.equals(servletParameters.rtmp)) {
			// RTMP protocol
			return RTMP_DOWNLOAD_URL + "/" + uuid;
		} else {
			// HLS
			return HLS_DOWNLOAD_URL + "/" + uuid + ".m3u8";
		}
	}

	private int getWaitForVideo(ServletParameters servletParameters) {
		return Boolean.TRUE.equals(servletParameters.rtmp) ? WAIT_FOR_RTMP : WAIT_FOR_HLS;
	}

}
