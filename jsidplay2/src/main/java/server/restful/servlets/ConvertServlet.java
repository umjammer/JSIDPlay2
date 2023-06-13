package server.restful.servlets;

import static java.lang.Math.min;
import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
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
import static server.restful.common.IServletSystemProperties.RTMP_EXCEEDS_MAXIMUM_DURATION;
import static server.restful.common.IServletSystemProperties.RTMP_NOT_YET_PLAYED_TIMEOUT;
import static server.restful.common.IServletSystemProperties.RTMP_UPLOAD_URL;
import static server.restful.common.IServletSystemProperties.WAIT_FOR_VIDEO_AVAILABLE_RETRY_COUNT;
import static server.restful.common.PlayerCleanupTimerTask.count;
import static server.restful.common.PlayerCleanupTimerTask.create;
import static server.restful.common.QrCode.createBarCodeImage;
import static server.restful.common.filters.CounterBasedRateLimiterFilter.FILTER_PARAMETER_MAX_REQUESTS_PER_SERVLET;
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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
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
import libsidutils.siddatabase.SidDatabase;
import server.restful.common.HlsType;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.filters.CounterBasedRateLimiterFilter;
import server.restful.common.parameter.RequestPathServletParameters.FileRequestPathServletParameters;
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
import sidplay.audio.SIDRegDriver.Format;
import sidplay.audio.SIDRegDriver.SIDRegStreamDriver;
import sidplay.audio.SleepDriver;
import sidplay.audio.WAVDriver.WAVStreamDriver;
import sidplay.ini.IniConfig;
import ui.common.Convenience;
import ui.common.util.InternetUtil;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class ConvertServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.ConvertServletParameters")
	public static class ConvertServletParameters extends FileRequestPathServletParameters {

		private Integer startSong;

		public Integer getStartSong() {
			return startSong;
		}

		@Parameter(names = { "--startSong" }, descriptionKey = "START_SONG", order = -13)
		public void setStartSong(Integer startSong) {
			this.startSong = startSong;
		}

		private Boolean download = Boolean.FALSE;

		public Boolean getDownload() {
			return download;
		}

		@Parameter(names = "--download", arity = 1, descriptionKey = "DOWNLOAD", order = -12)
		public void setDownload(Boolean download) {
			this.download = download;
		}

		private Integer reuSize;

		public Integer getReuSize() {
			return reuSize;
		}

		@Parameter(names = { "--reuSize" }, descriptionKey = "REU_SIZE", order = -11)
		public void setReuSize(Integer reuSize) {
			this.reuSize = reuSize;
		}

		private Boolean sfxSoundExpander = Boolean.FALSE;

		public Boolean getSfxSoundExpander() {
			return sfxSoundExpander;
		}

		@Parameter(names = "--sfxSoundExpander", arity = 1, descriptionKey = "SFX_SOUND_EXPANDER", order = -10)
		public void setSfxSoundExpander(Boolean sfxSoundExpander) {
			this.sfxSoundExpander = sfxSoundExpander;
		}

		private Integer sfxSoundExpanderType = 0;

		public Integer getSfxSoundExpanderType() {
			return sfxSoundExpanderType;
		}

		@Parameter(names = { "--sfxSoundExpanderType" }, descriptionKey = "SFX_SOUND_EXPANDER_TYPE", order = -9)
		public void setSfxSoundExpanderType(Integer sfxSoundExpanderType) {
			this.sfxSoundExpanderType = sfxSoundExpanderType;
		}

		private Integer pressSpaceInterval = PRESS_SPACE_INTERVALL;

		public Integer getPressSpaceInterval() {
			return pressSpaceInterval;
		}

		@Parameter(names = { "--pressSpaceInterval" }, descriptionKey = "PRESS_SPACE_INTERVAL", order = -8)
		public void setPressSpaceInterval(Integer pressSpaceInterval) {
			this.pressSpaceInterval = pressSpaceInterval;
		}

		private Boolean showStatus = Boolean.TRUE;

		public Boolean getShowStatus() {
			return showStatus;
		}

		@Parameter(names = "--status", arity = 1, descriptionKey = "STATUS", order = -7)
		public void setShowStatus(Boolean showStatus) {
			this.showStatus = showStatus;
		}

		private Boolean useHls = Boolean.FALSE;

		public Boolean getUseHls() {
			return useHls;
		}

		@Parameter(names = "--hls", arity = 1, descriptionKey = "HLS", order = -6)
		public void setUseHls(Boolean useHls) {
			this.useHls = useHls;
		}

		private HlsType hlsType = HlsType.HLS_JS;

		public HlsType getHlsType() {
			return hlsType;
		}

		@Parameter(names = { "--hlsType" }, descriptionKey = "HLS_TYPE", order = -5)
		public void setHlsType(HlsType hlsType) {
			this.hlsType = hlsType;
		}

		private Format sidRegFormat = Format.APP;

		public Format getSidRegFormat() {
			return sidRegFormat;
		}

		@Parameter(names = "--sidRegFormat", descriptionKey = "SID_REG_FORMAT", order = -4)
		public void setSidRegFormat(Format sidRegFormat) {
			this.sidRegFormat = sidRegFormat;
		}

		private String autostart;

		public String getAutostart() {
			return autostart;
		}

		@Parameter(names = { "--autostart" }, descriptionKey = "AUTOSTART", order = -3)
		public void setAutostart(String autostart) {
			this.autostart = autostart;
		}

		private Boolean videoTuneAsAudio = Boolean.FALSE;

		public Boolean getVideoTuneAsAudio() {
			return videoTuneAsAudio;
		}

		@Parameter(names = "--videoTuneAsAudio", arity = 1, descriptionKey = "VIDEO_TUNE_AS_AUDIO", order = -2)
		public void setVideoTuneAsAudio(Boolean videoTuneAsAudio) {
			this.videoTuneAsAudio = videoTuneAsAudio;
		}

		@ParametersDelegate
		private IniConfig config = new IniConfig();

		public IniConfig getConfig() {
			return config;
		}

	}

	public static final String CONVERT_PATH = "/convert";

	public ConvertServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + CONVERT_PATH;
	}

	@Override
	public List<Filter> getServletFilters() {
		return Arrays.asList(new CounterBasedRateLimiterFilter());
	}

	@Override
	public Map<String, String> getServletFiltersParameterMap() {
		Map<String, String> result = new HashMap<>();
		result.put(FILTER_PARAMETER_MAX_REQUESTS_PER_SERVLET, String.valueOf(MAX_CONVERT_IN_PARALLEL));
		return result;
	}

	@Override
	public boolean isSecured() {
		// We cannot secure ConvertServlet here, since Chrome browser ignores basic
		// authentication in HTML5 audio tag src attribute
		return false;
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
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/convert/C64Music/10_Years_HVSC_1.d64?enableSidDatabase=true&startTime=00:00&defaultLength=04:00&fadeIn=00:00&fadeOut=00:00&loop=false&single=true&frequency=MEDIUM&sampling=RESAMPLE&mainVolume=0&secondVolume=0&thirdVolume=0&mainBalance=0.3&secondBalance=0.7&thirdBalance=0.5&mainDelay=0&secondDelay=20&thirdDelay=0&bufferSize=65536&cbr=64&vbrQuality=5&vbr=false&acBitRate=64000&vcBitRate=480000&vcAudioDelay=0&delayBypass=true&reverbBypass=true&defaultEmulation=RESIDFP&defaultClock=PAL&defaultModel=MOS8580&sidToRead=FIRST_SID&digiBoosted8580=false&fakeStereo=false&muteVoice1=false&muteVoice2=false&muteVoice3=false&muteVoice4=false&muteStereoVoice1=false&muteStereoVoice2=false&muteStereoVoice3=false&muteStereoVoice4=false&muteThirdSidVoice1=false&muteThirdSidVoice2=false&muteThirdSidVoice3=false&muteThirdSidVoice4=false&filter6581=FilterAverage6581&stereoFilter6581=FilterAverage6581&thirdFilter6581=FilterAverage6581&filter8580=FilterAverage8580&stereoFilter8580=FilterAverage8580&thirdFilter8580=FilterAverage8580&reSIDfpFilter6581=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter6581=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter6581=FilterAlankila6581R4AR_3789&reSIDfpFilter8580=FilterTrurl8580R5_3691&reSIDfpStereoFilter8580=FilterTrurl8580R5_3691&reSIDfpThirdFilter8580=FilterTrurl8580R5_3691&detectPSID64ChipModel=true&hls=true&hlsType=VIDEO_JS&pressSpaceInterval=90&status=true&jiffydos=true
	 * }
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doGet(request);
		try {
			final ConvertServletParameters servletParameters = new ConvertServletParameters();

			JCommander commander = parseRequestParameters(request, response, servletParameters, getServletPath());

			final File file = getFile(commander, servletParameters, true /* request.isUserInRole(ROLE_ADMIN) */);
			if (file == null) {
				commander.usage();
				return;
			}

			if (AUDIO_TUNE_FILE_FILTER.accept(file)
					|| (servletParameters.videoTuneAsAudio && VIDEO_TUNE_FILE_FILTER.accept(file))) {

				Audio audio = getAudioFormat(servletParameters.config);
				AudioDriver driver = getAudioDriverOfAudioFormat(audio, response.getOutputStream(), servletParameters);

				if (Boolean.TRUE.equals(servletParameters.download)) {
					response.addHeader(CONTENT_DISPOSITION, ATTACHMENT + "; filename="
							+ URLEncoder.encode(getAttachmentFilename(file, driver), UTF_8.name()));
				}
				response.setContentType(getMimeType(driver.getExtension()).toString());
				convert2audio(file, driver, servletParameters);

			} else if (VIDEO_TUNE_FILE_FILTER.accept(file) || DISK_FILE_FILTER.accept(file)
					|| TAPE_FILE_FILTER.accept(file) || CART_FILE_FILTER.accept(file)) {

				UUID uuid = UUID.randomUUID();

				Audio audio = getVideoFormat(servletParameters.config);
				AudioDriver driver = getAudioDriverOfVideoFormat(audio, uuid, servletParameters);

				if (Boolean.FALSE.equals(servletParameters.download) && audio == FLV) {
					if (count() < MAX_RTMP_IN_PARALLEL) {
						Thread parentThread = currentThread();
						new Thread(() -> {
							try {
								info(String.format("START uuid=%s", uuid), parentThread);
								convert2video(file, driver, servletParameters, uuid, parentThread);
								info(String.format("END uuid=%s", uuid), parentThread);
							} catch (IOException | SidTuneError e) {
								error(e, parentThread);
							}
						}, "RTMP").start();
						waitUntilVideoIsAvailable(uuid);

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
								+ URLEncoder.encode(getAttachmentFilename(file, driver), UTF_8.name()));
					}
					response.setContentType(getMimeType(driver.getExtension()).toString());
					File videoFile = convert2video(file, driver, servletParameters, null);
					copy(videoFile, response.getOutputStream());
					videoFile.delete();
				}
			} else {
				response.setContentType(getMimeType(getFilenameSuffix(file.getName())).toString());
				response.addHeader(CONTENT_DISPOSITION,
						ATTACHMENT + "; filename=" + URLEncoder.encode(file.getName(), UTF_8.name()));
				copy(file, response.getOutputStream());
			}
		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

	private String getAttachmentFilename(final File file, AudioDriver driver) {
		return getFilenameWithoutSuffix(file.getName()) + driver.getExtension();
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

	private AudioDriver getAudioDriverOfAudioFormat(Audio audio, OutputStream outputstream,
			ConvertServletParameters servletParameters) {
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
			return new SIDRegStreamDriver(outputstream, servletParameters.getSidRegFormat());
		}
	}

	private void convert2audio(File file, AudioDriver driver, ConvertServletParameters servletParameters)
			throws IOException, SidTuneError {
		ISidPlay2Section sidplay2Section = servletParameters.config.getSidplay2Section();

		Player player = new Player(servletParameters.config);
		player.getC64().getVIC().setPalEmulation(PALEmulation.NONE);
		if (Boolean.TRUE.equals(servletParameters.download)) {
			sidplay2Section.setDefaultPlayLength(min(sidplay2Section.getDefaultPlayLength(), MAX_AUD_DOWNLOAD_LENGTH));
		}
		File root = configuration.getSidplay2Section().getHvsc();
		if (root != null) {
			player.setSidDatabase(new SidDatabase(root));
		}
		Thread[] parentThreads = of(currentThread()).toArray(Thread[]::new);

		player.setAudioDriver(driver);
		player.setUncaughtExceptionHandler(
				(thread, throwable) -> uncaughtExceptionHandler(throwable, thread, parentThreads));
		player.setCheckDefaultLengthInRecordMode(Boolean.TRUE.equals(servletParameters.download));
		player.setCheckLoopOffInRecordMode(Boolean.TRUE.equals(servletParameters.download));
		player.setForceCheckSongLength(Boolean.TRUE.equals(servletParameters.download));

		if (servletParameters.sfxSoundExpander && !player.getC64().isCartridge()) {
			player.insertCartridge(CartridgeType.SOUNDEXPANDER, servletParameters.sfxSoundExpanderType);
		}
		if (servletParameters.reuSize != null && !player.getC64().isCartridge()) {
			player.insertCartridge(CartridgeType.REU, servletParameters.reuSize);
		}
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

	private AudioDriver getAudioDriverOfVideoFormat(Audio audio, UUID uuid,
			ConvertServletParameters servletParameters) {
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

	private File convert2video(File file, AudioDriver driver, ConvertServletParameters servletParameters, UUID uuid,
			Thread... parentThread) throws IOException, SidTuneError {
		File videoFile = null;
		ISidPlay2Section sidplay2Section = servletParameters.config.getSidplay2Section();

		Player player = new Player(servletParameters.config);
		if (Boolean.TRUE.equals(servletParameters.download)) {
			sidplay2Section.setDefaultPlayLength(min(sidplay2Section.getDefaultPlayLength(), MAX_VID_DOWNLOAD_LENGTH));
			videoFile = createVideoFile(player, driver);
		} else {
			sidplay2Section.setDefaultPlayLength(RTMP_EXCEEDS_MAXIMUM_DURATION);
		}
		Thread[] parentThreads = concat(of(parentThread), of(currentThread())).toArray(Thread[]::new);

		player.setAudioDriver(driver);
		player.setUncaughtExceptionHandler(
				(thread, throwable) -> uncaughtExceptionHandler(throwable, thread, parentThreads));
		player.setCheckDefaultLengthInRecordMode(Boolean.TRUE.equals(servletParameters.download));
		player.setCheckLoopOffInRecordMode(Boolean.TRUE.equals(servletParameters.download));
		player.setForceCheckSongLength(Boolean.TRUE.equals(servletParameters.download));

		new Convenience(player).autostart(file, Convenience.LEXICALLY_FIRST_MEDIA, servletParameters.autostart);

		if (servletParameters.sfxSoundExpander && !player.getC64().isCartridge()) {
			player.insertCartridge(CartridgeType.SOUNDEXPANDER, servletParameters.sfxSoundExpanderType);
		}
		if (servletParameters.reuSize != null && !player.getC64().isCartridge()) {
			player.insertCartridge(CartridgeType.REU, servletParameters.reuSize);
		}
		if (TAPE_FILE_FILTER.accept(file)) {
			player.getConfig().getC1541Section().setJiffyDosInstalled(false);
		}
		if (uuid != null) {
			create(uuid, player, file, servletParameters);
		}
		player.stopC64(false);
		return videoFile;
	}

	private File createVideoFile(Player player, AudioDriver driver) throws IOException {
		ISidPlay2Section sidplay2Section = player.getConfig().getSidplay2Section();

		File videoFile = File.createTempFile("jsidplay2video", driver.getExtension(), sidplay2Section.getTmpDir());
		videoFile.deleteOnExit();
		player.setRecordingFilenameProvider(tune -> getFilenameWithoutSuffix(videoFile.getAbsolutePath()));
		return videoFile;
	}

	private Map<String, String> createReplacements(ConvertServletParameters servletParameters,
			HttpServletRequest request, File file, UUID uuid) throws IOException, WriterException {
		String videoUrl = getVideoUrl(Boolean.TRUE.equals(servletParameters.useHls), uuid);
		String qrCodeImgTag = createQrCodeImgTag(getRequestURL(request), "UTF-8", "png", 320, 320);

		Map<String, String> result = new HashMap<>();
		result.put("$uuid", uuid.toString());
		result.put("$qrCodeImgTag", qrCodeImgTag);
		result.put("$videoUrl", videoUrl);
		result.put("$hls", String.valueOf(Boolean.TRUE.equals(servletParameters.useHls)));
		result.put("$hlsType", servletParameters.getHlsType().name());
		result.put("$hlsScript", servletParameters.getHlsType().getScript());
		result.put("$notYetPlayedTimeout", String.valueOf(RTMP_NOT_YET_PLAYED_TIMEOUT));
		result.put("$notifyForHLS", String.valueOf(NOTIFY_FOR_HLS));
		return result;
	}

	private String createQrCodeImgTag(String data, String charset, String imgFormat, int width, int height)
			throws IOException, WriterException {
		ByteArrayOutputStream qrCodeImgData = new ByteArrayOutputStream();
		ImageIO.write(createBarCodeImage(data, charset, width, height), imgFormat, qrCodeImgData);
		return format("<img src='data:image/%s;base64,%s'>", imgFormat, printBase64Binary(qrCodeImgData.toByteArray()));
	}

	private String getVideoUrl(boolean useHls, UUID uuid) {
		if (useHls) {
			// HLS protocol
			return HLS_DOWNLOAD_URL + "/" + uuid + ".m3u8";
		} else {
			// RTMP protocol
			return RTMP_DOWNLOAD_URL + "/" + uuid;
		}
	}

	private String getRequestURL(HttpServletRequest request) {
		StringBuilder result = new StringBuilder();
		result.append(request.getRequestURL());
		if (request.getQueryString() != null) {
			result.append("?");
			result.append(request.getQueryString());
		}
		return result.toString();
	}

	private void waitUntilVideoIsAvailable(UUID uuid) throws InterruptedException, MalformedURLException {
		URL url = new URL(getVideoUrl(true, uuid));
		int retryCount = 0;
		while (retryCount++ < WAIT_FOR_VIDEO_AVAILABLE_RETRY_COUNT) {
			try {
				InternetUtil.openConnection(url, configuration.getSidplay2Section());
				// Give video production a jump start
				Thread.sleep(1000);
				break;
			} catch (InterruptedException e) {
				throw e;
			} catch (IOException e) {
				// connection not yet established, retry!
				Thread.sleep(500);
			}
		}
	}

}
