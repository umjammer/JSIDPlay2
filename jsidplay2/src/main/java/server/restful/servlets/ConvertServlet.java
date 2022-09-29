package server.restful.servlets;

import static java.lang.Math.min;
import static java.lang.String.format;
import static java.lang.Thread.getAllStackTraces;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.xml.bind.DatatypeConverter.printBase64Binary;
import static libsidutils.PathUtils.getFilenameSuffix;
import static libsidutils.PathUtils.getFilenameWithoutSuffix;
import static libsidutils.ZipFileUtils.convertStreamToString;
import static libsidutils.ZipFileUtils.copy;
import static org.apache.http.HttpStatus.SC_TOO_MANY_REQUESTS;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.ATTACHMENT;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.CONTENT_DISPOSITION;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_HTML;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.ContentTypeAndFileExtensions.getMimeType;
import static server.restful.common.IServletSystemProperties.HLS_DOWNLOAD_URL;
import static server.restful.common.IServletSystemProperties.MAX_AUD_DOWNLOAD_LENGTH;
import static server.restful.common.IServletSystemProperties.MAX_CONVERT_IN_PARALLEL;
import static server.restful.common.IServletSystemProperties.MAX_RTMP_IN_PARALLEL;
import static server.restful.common.IServletSystemProperties.MAX_VID_DOWNLOAD_LENGTH;
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
import libsidplay.components.mos656x.PALEmulation;
import libsidplay.config.IConfig;
import libsidplay.config.ISidPlay2Section;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import libsidutils.siddatabase.SidDatabase;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.ServletBaseParameters;
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
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class ConvertServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.ConvertServletParameters")
	public static class ServletParameters extends ServletBaseParameters {

		private Integer startSong;

		public Integer getStartSong() {
			return startSong;
		}

		@Parameter(names = { "--startSong" }, descriptionKey = "START_SONG", order = -8)
		public void setStartSong(Integer startSong) {
			this.startSong = startSong;
		}

		private Boolean download = Boolean.FALSE;

		public Boolean getDownload() {
			return download;
		}

		@Parameter(names = "--download", arity = 1, descriptionKey = "DOWNLOAD", order = -7)
		public void setDownload(Boolean download) {
			this.download = download;
		}

		private Integer reuSize;

		public Integer getReuSize() {
			return reuSize;
		}

		@Parameter(names = { "--reuSize" }, descriptionKey = "REU_SIZE", order = -6)
		public void setReuSize(Integer reuSize) {
			this.reuSize = reuSize;
		}

		private Integer pressSpaceInterval = PRESS_SPACE_INTERVALL;

		public Integer getPressSpaceInterval() {
			return pressSpaceInterval;
		}

		@Parameter(names = { "--pressSpaceInterval" }, descriptionKey = "PRESS_SPACE_INTERVAL", order = -5)
		public void setPressSpaceInterval(Integer pressSpaceInterval) {
			this.pressSpaceInterval = pressSpaceInterval;
		}

		private Boolean status = Boolean.TRUE;

		public Boolean getStatus() {
			return status;
		}

		@Parameter(names = "--status", arity = 1, descriptionKey = "STATUS", order = -4)
		public void setStatus(Boolean status) {
			this.status = status;
		}

		private Boolean rtmp = Boolean.TRUE;

		public Boolean getRtmp() {
			return rtmp;
		}

		@Parameter(names = "--rtmp", arity = 1, descriptionKey = "RTMP", order = -3)
		public void setRtmp(Boolean rtmp) {
			this.rtmp = rtmp;
		}

		private String autostart;

		public String getAutostart() {
			return autostart;
		}

		@Parameter(names = { "--autostart" }, descriptionKey = "AUTOSTART", order = -2)
		public void setAutostart(String autostart) {
			this.autostart = autostart;
		}

		@ParametersDelegate
		private IniConfig config = new IniConfig();

		public IniConfig getConfig() {
			return config;
		}

		private volatile boolean started;

	}

	public static final String CONVERT_PATH = "/convert";

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(ConvertServlet.class.getName());

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

			final File file = parseRequestPath(commander, servletParameters, true);
			if (file == null) {
				commander.usage();
				return;
			}
			final IniConfig config = servletParameters.config;

			if (AUDIO_TUNE_FILE_FILTER.accept(file)) {

				Audio audio = getAudioFormat(config);
				AudioDriver driver = getAudioDriverOfAudioFormat(audio, response.getOutputStream());

				if (Boolean.TRUE.equals(servletParameters.download)) {
					response.addHeader(CONTENT_DISPOSITION, ATTACHMENT + "; filename="
							+ getFilenameWithoutSuffix(file.getName()) + driver.getExtension());
				}
				response.setContentType(getMimeType(driver.getExtension()).toString());
				convert2audio(config, file, driver, servletParameters);

			} else if (VIDEO_TUNE_FILE_FILTER.accept(file) || DISK_FILE_FILTER.accept(file)
					|| TAPE_FILE_FILTER.accept(file) || CART_FILE_FILTER.accept(file)) {

				UUID uuid = UUID.randomUUID();

				Audio audio = getVideoFormat(config);
				AudioDriver driver = getAudioDriverOfVideoFormat(audio, uuid, servletParameters);

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
						response.sendError(SC_TOO_MANY_REQUESTS, "Too Many Requests");
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
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
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
		ISidPlay2Section sidplay2Section = config.getSidplay2Section();

		Player player = new Player(config);
		player.getC64().getVIC().setPalEmulation(PALEmulation.NONE);
		if (Boolean.TRUE.equals(servletParameters.download)) {
			sidplay2Section.setDefaultPlayLength(min(sidplay2Section.getDefaultPlayLength(), MAX_AUD_DOWNLOAD_LENGTH));
		}
		File root = configuration.getSidplay2Section().getHvsc();
		if (root != null) {
			player.setSidDatabase(new SidDatabase(root));
		}
		player.setAudioDriver(driver);
		player.setUncaughtExceptionHandler((thread, throwable) -> uncaughtExceptionHandler(thread, throwable));
		player.setCheckDefaultLengthInRecordMode(Boolean.TRUE.equals(servletParameters.download));
		player.setCheckLoopOffInRecordMode(Boolean.TRUE.equals(servletParameters.download));
		player.setForceCheckSongLength(Boolean.TRUE.equals(servletParameters.download));

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

	private AudioDriver getAudioDriverOfVideoFormat(Audio audio, UUID uuid, ServletParameters servletParameters) {
		switch (audio) {
		case FLV:
		default:
			if (Boolean.TRUE.equals(servletParameters.download)) {
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

		Player player = new Player(config);
		if (Boolean.TRUE.equals(servletParameters.download)) {
			sidplay2Section.setDefaultPlayLength(min(sidplay2Section.getDefaultPlayLength(), MAX_VID_DOWNLOAD_LENGTH));
			videoFile = createVideoFile(player, driver);
		} else {
			sidplay2Section.setDefaultPlayLength(MAX_VID_DOWNLOAD_LENGTH);
		}
		player.setAudioDriver(driver);
		player.setUncaughtExceptionHandler((thread, throwable) -> uncaughtExceptionHandler(thread, throwable));
		player.setCheckDefaultLengthInRecordMode(Boolean.TRUE.equals(servletParameters.download));
		player.setCheckLoopOffInRecordMode(Boolean.TRUE.equals(servletParameters.download));
		player.setForceCheckSongLength(Boolean.TRUE.equals(servletParameters.download));

		new Convenience(player).autostart(file, Convenience.LEXICALLY_FIRST_MEDIA, servletParameters.autostart);

		if (servletParameters.reuSize != null && !player.getC64().isCartridge()) {
			player.insertCartridge(CartridgeType.REU, servletParameters.reuSize);
		}
		if (TAPE_FILE_FILTER.accept(file)) {
			player.getConfig().getC1541Section().setJiffyDosInstalled(false);
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
		String videoUrl = getVideoUrl(servletParameters, request.getRemoteAddr(), uuid);
		String qrCodeImgTag = createQrCodeImgTag(videoUrl, "UTF-8", "png", 320, 320);

		Map<String, String> result = new HashMap<>();
		result.put("$uuid", uuid.toString());
		result.put("$qrCodeImgTag", qrCodeImgTag);
		result.put("$videoUrl", videoUrl);
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

	private String getVideoUrl(ServletParameters servletParameters, String remoteAddress, UUID uuid) {
		if (Boolean.TRUE.equals(servletParameters.rtmp)) {
			// RTMP protocol
			return RTMP_DOWNLOAD_URL + "/" + uuid;
		} else {
			// HLS protocol
			return HLS_DOWNLOAD_URL + "/" + uuid + ".m3u8";
		}
	}

	private int getWaitForVideo(ServletParameters servletParameters) {
		return Boolean.TRUE.equals(servletParameters.rtmp) ? WAIT_FOR_RTMP : WAIT_FOR_HLS;
	}

}
