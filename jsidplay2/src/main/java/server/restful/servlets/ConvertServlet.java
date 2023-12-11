package server.restful.servlets;

import static java.lang.Math.min;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.xml.bind.DatatypeConverter.printBase64Binary;
import static libsidutils.IOUtils.convertStreamToString;
import static libsidutils.IOUtils.getFilenameSuffix;
import static libsidutils.IOUtils.getFilenameWithoutSuffix;
import static libsidutils.ZipFileUtils.newFileInputStream;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.ATTACHMENT;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.CONTENT_DISPOSITION;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_HTML;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_MPEG;
import static server.restful.common.ContentTypeAndFileExtensions.getMimeType;
import static server.restful.common.IServletSystemProperties.CACHE_CONTROL_RESPONSE_HEADER_UNCACHED;
import static server.restful.common.IServletSystemProperties.CONVERT_ASYNC_TIMEOUT;
import static server.restful.common.IServletSystemProperties.HLS_DOWNLOAD_URL;
import static server.restful.common.IServletSystemProperties.MAX_AUD_DOWNLOAD_LENGTH;
import static server.restful.common.IServletSystemProperties.MAX_CONVERT_IN_PARALLEL;
import static server.restful.common.IServletSystemProperties.MAX_CONVERT_RTMP_IN_PARALLEL;
import static server.restful.common.IServletSystemProperties.MAX_VID_DOWNLOAD_LENGTH;
import static server.restful.common.IServletSystemProperties.NOTIFY_FOR_HLS;
import static server.restful.common.IServletSystemProperties.PRESS_SPACE_INTERVALL;
import static server.restful.common.IServletSystemProperties.RTMP_DOWNLOAD_URL;
import static server.restful.common.IServletSystemProperties.RTMP_EXCEEDS_MAXIMUM_DURATION;
import static server.restful.common.IServletSystemProperties.RTMP_NOT_YET_PLAYED_TIMEOUT;
import static server.restful.common.IServletSystemProperties.RTMP_UPLOAD_URL;
import static server.restful.common.IServletSystemProperties.TEXT_TO_SPEECH;
import static server.restful.common.IServletSystemProperties.WAIT_FOR_VIDEO_AVAILABLE_RETRY_COUNT;
import static server.restful.common.QrCode.createBarCodeImage;
import static server.restful.common.ServletUtil.error;
import static server.restful.common.ServletUtil.info;
import static server.restful.common.ServletUtil.uncaughtExceptionHandler;
import static server.restful.common.filters.HeadRequestFilter.FILTER_PARAMETER_CONTENT_TYPE;
import static server.restful.common.filters.RTMPBasedRateLimiterFilter.FILTER_PARAMETER_MAX_RTMP_PER_SERVLET;
import static server.restful.common.rtmp.PlayerCleanupTimerTask.create;
import static sidplay.audio.Audio.FLV;
import static sidplay.audio.Audio.MP3;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.imageio.ImageIO;

import org.apache.http.HttpHeaders;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.google.zxing.WriterException;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidplay.common.Engine;
import libsidplay.components.cart.CartridgeType;
import libsidplay.components.mos656x.PALEmulation;
import libsidplay.config.IC1541Section;
import libsidplay.config.IEmulationSection;
import libsidplay.config.ISidPlay2Section;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import server.restful.common.ContentTypeAndFileExtensions;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.async.DefaultThreadFactory;
import server.restful.common.async.HttpAsyncContextRunnable;
import server.restful.common.converter.LocaleConverter;
import server.restful.common.converter.WebResourceConverter;
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.parameter.requestpath.FileRequestPathServletParameters;
import server.restful.common.rtmp.HlsType;
import server.restful.common.text2speech.TextToSpeech;
import server.restful.common.text2speech.TextToSpeechBean;
import server.restful.common.text2speech.TextToSpeechType;
import sidplay.Player;
import sidplay.audio.AACDriver.AACStreamDriver;
import sidplay.audio.AVIDriver.AVIFileDriver;
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
import sidplay.audio.ThrottlingDriver;
import sidplay.audio.WAVDriver.WAVStreamDriver;
import sidplay.ini.IniConfig;
import ui.common.Convenience;
import ui.common.ConvenienceResult;
import ui.common.util.InternetUtil;

@SuppressWarnings("serial")
@WebServlet(name = "ConvertServlet", displayName = "ConvertServlet", asyncSupported = true, urlPatterns = CONTEXT_ROOT_SERVLET
		+ "/convert/*", description = "Stream e.g. SID as MP3 or D64 as RTMP video stream")
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { /* mobile Chrome ignores basic auth in audio src! */ }))
public class ConvertServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.ConvertServletParameters")
	public static class ConvertServletParameters extends FileRequestPathServletParameters {

		private Boolean useDevTools = Boolean.FALSE;

		public Boolean getUseDevTools() {
			return useDevTools;
		}

		@Parameter(names = "--devtools", arity = 1, descriptionKey = "USE_DEV_TOOLS", hidden = true, order = -18)
		public void setUseDevTools(Boolean useDevTools) {
			this.useDevTools = useDevTools;
		}

		private Integer startSong;

		public Integer getStartSong() {
			return startSong;
		}

		@Parameter(names = { "--startSong" }, descriptionKey = "START_SONG", order = -17)
		public void setStartSong(Integer startSong) {
			this.startSong = startSong;
		}

		private Locale locale = Locale.ENGLISH;

		public Locale getLocale() {
			return locale;
		}

		@Parameter(names = "--locale", descriptionKey = "LOCALE", converter = LocaleConverter.class, order = -16)
		public void setLocale(Locale locale) {
			this.locale = locale;
		}

		private TextToSpeechType textToSpeechType = TextToSpeechType.PICO2WAVE;

		public TextToSpeechType getTextToSpeechType() {
			return textToSpeechType;
		}

		@Parameter(names = "--textToSpeechType", arity = 1, descriptionKey = "TEXT_TO_SPEECH_TYPE", order = -15)
		public void setTextToSpeechType(TextToSpeechType textToSpeechType) {
			this.textToSpeechType = textToSpeechType;
		}

		private Locale textToSpeechLocale;

		public Locale getTextToSpeechLocale() {
			return textToSpeechLocale;
		}

		@Parameter(names = "--textToSpeechLocale", descriptionKey = "TEXT_TO_SPEECH_LOCALE", converter = LocaleConverter.class, order = -14)
		public void setTextToSpeechLocale(Locale textToSpeechLocale) {
			this.textToSpeechLocale = textToSpeechLocale;
		}

		private Boolean download = Boolean.FALSE;

		public Boolean getDownload() {
			return download;
		}

		@Parameter(names = "--download", arity = 1, descriptionKey = "DOWNLOAD", order = -13)
		public void setDownload(Boolean download) {
			this.download = download;
		}

		private Integer reuSize;

		public Integer getReuSize() {
			return reuSize;
		}

		@Parameter(names = { "--reuSize" }, descriptionKey = "REU_SIZE", order = -12)
		public void setReuSize(Integer reuSize) {
			this.reuSize = reuSize;
		}

		private Boolean sfxSoundExpander = Boolean.FALSE;

		public Boolean getSfxSoundExpander() {
			return sfxSoundExpander;
		}

		@Parameter(names = "--sfxSoundExpander", arity = 1, descriptionKey = "SFX_SOUND_EXPANDER", order = -11)
		public void setSfxSoundExpander(Boolean sfxSoundExpander) {
			this.sfxSoundExpander = sfxSoundExpander;
		}

		private Integer sfxSoundExpanderType = 0;

		public Integer getSfxSoundExpanderType() {
			return sfxSoundExpanderType;
		}

		@Parameter(names = { "--sfxSoundExpanderType" }, descriptionKey = "SFX_SOUND_EXPANDER_TYPE", order = -10)
		public void setSfxSoundExpanderType(Integer sfxSoundExpanderType) {
			this.sfxSoundExpanderType = sfxSoundExpanderType;
		}

		private Integer pressSpaceInterval = PRESS_SPACE_INTERVALL;

		public Integer getPressSpaceInterval() {
			return pressSpaceInterval;
		}

		@Parameter(names = { "--pressSpaceInterval" }, descriptionKey = "PRESS_SPACE_INTERVAL", order = -9)
		public void setPressSpaceInterval(Integer pressSpaceInterval) {
			this.pressSpaceInterval = pressSpaceInterval;
		}

		private Boolean showStatus = Boolean.TRUE;

		public Boolean getShowStatus() {
			return showStatus;
		}

		@Parameter(names = "--status", arity = 1, descriptionKey = "STATUS", order = -8)
		public void setShowStatus(Boolean showStatus) {
			this.showStatus = showStatus;
		}

		private Boolean useHls = Boolean.FALSE;

		public Boolean getUseHls() {
			return useHls;
		}

		@Parameter(names = "--hls", arity = 1, descriptionKey = "HLS", order = -7)
		public void setUseHls(Boolean useHls) {
			this.useHls = useHls;
		}

		private HlsType hlsType = HlsType.HLS_JS;

		public HlsType getHlsType() {
			return hlsType;
		}

		@Parameter(names = { "--hlsType" }, descriptionKey = "HLS_TYPE", order = -6)
		public void setHlsType(HlsType hlsType) {
			this.hlsType = hlsType;
		}

		private Format sidRegFormat = Format.APP;

		public Format getSidRegFormat() {
			return sidRegFormat;
		}

		@Parameter(names = "--sidRegFormat", descriptionKey = "SID_REG_FORMAT", order = -5)
		public void setSidRegFormat(Format sidRegFormat) {
			this.sidRegFormat = sidRegFormat;
		}

		private String autostart;

		public String getAutostart() {
			return autostart;
		}

		@Parameter(names = { "--autostart" }, descriptionKey = "AUTOSTART", order = -4)
		public void setAutostart(String autostart) {
			this.autostart = autostart;
		}

		private Boolean videoTuneAsAudio = Boolean.FALSE;

		public Boolean getVideoTuneAsAudio() {
			return videoTuneAsAudio;
		}

		@Parameter(names = "--videoTuneAsAudio", arity = 1, descriptionKey = "VIDEO_TUNE_AS_AUDIO", order = -3)
		public void setVideoTuneAsAudio(Boolean videoTuneAsAudio) {
			this.videoTuneAsAudio = videoTuneAsAudio;
		}

		private Boolean audioTuneAsVideo = Boolean.FALSE;

		public Boolean getAudioTuneAsVideo() {
			return audioTuneAsVideo;
		}

		@Parameter(names = "--audioTuneAsVideo", arity = 1, descriptionKey = "AUDIO_TUNE_AS_VIDEO", order = -2)
		public void setAudioTuneAsVideo(Boolean audioTuneAsVideo) {
			this.audioTuneAsVideo = audioTuneAsVideo;
		}

		@ParametersDelegate
		private IniConfig config = new IniConfig();

		public IniConfig getConfig() {
			return config;
		}

	}

	private ExecutorService executorService;

	@Override
	public void init() throws ServletException {
		executorService = Executors.newFixedThreadPool(MAX_CONVERT_IN_PARALLEL, new DefaultThreadFactory("/convert"));
	}

	@Override
	public void destroy() {
		executorService.shutdown();
	}

	@Override
	public Map<String, String> getServletFiltersParameterMap() {
		Map<String, String> result = new HashMap<>();
		result.put(FILTER_PARAMETER_CONTENT_TYPE, MIME_TYPE_MPEG.getMimeType());
		result.put(FILTER_PARAMETER_MAX_RTMP_PER_SERVLET, String.valueOf(MAX_CONVERT_RTMP_IN_PARALLEL));
		return result;
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
		WebServlet webServlet = getClass().getAnnotation(WebServlet.class);
		ServletSecurity servletSecurity = getClass().getAnnotation(ServletSecurity.class);

		AsyncContext asyncContext = request.startAsync(request, response);
		asyncContext.setTimeout(CONVERT_ASYNC_TIMEOUT);

		executorService.execute(new HttpAsyncContextRunnable(asyncContext, getServletContext()) {

			public void run(HttpServletRequest request, HttpServletResponse response) throws IOException {
				try {
					final ConvertServletParameters servletParameters = new ConvertServletParameters();

					ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
							webServlet);

					final File file = servletParameters.fetchFile(configuration, directoryProperties, parser,
							servletSecurity, request.isUserInRole(ROLE_ADMIN));
					if (file == null || parser.hasException()) {
						parser.usage();
						return;
					}
					if ((AUDIO_TUNE_FILE_FILTER.accept(file)
							|| (servletParameters.videoTuneAsAudio && VIDEO_TUNE_FILE_FILTER.accept(file)))
							&& !servletParameters.audioTuneAsVideo) {

						AudioDriver driver = getAudioDriverOfAudioFormat(response, servletParameters);

						if (Boolean.TRUE.equals(servletParameters.download)) {
							response.addHeader(CONTENT_DISPOSITION, ATTACHMENT + "; filename="
									+ URLEncoder.encode(getAttachmentFilename(file, driver), UTF_8.name()));
						}
						response.setContentType(getMimeType(driver.getExtension()).toString());
						convert2audio(file, driver, servletParameters);

					} else if (VIDEO_TUNE_FILE_FILTER.accept(file) || DISK_FILE_FILTER.accept(file)
							|| TAPE_FILE_FILTER.accept(file) || CART_FILE_FILTER.accept(file)
							|| AUDIO_TUNE_FILE_FILTER.accept(file)) {

						UUID uuid = UUID.randomUUID();

						AudioDriver driver = getAudioDriverOfVideoFormat(uuid, servletParameters);

						if (Boolean.FALSE.equals(servletParameters.download)
								&& driver.lookup(FLVStreamDriver.class).isPresent()) {

							Thread poolThread = Thread.currentThread();
							new Thread(() -> {
								try {
									convert2video(file, driver, servletParameters, uuid, parentThread, poolThread);
								} catch (IOException | SidTuneError e) {
									error(getServletContext(), e, parentThread);
								}
							}, "RTMP").start();
							waitUntilVideoIsAvailable(uuid);

							response.setHeader(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_RESPONSE_HEADER_UNCACHED);

							Map<String, String> replacements = createReplacements(servletParameters, request, file,
									uuid);
							try (InputStream is = new WebResourceConverter("<ServletPath>").convert("/convert.html")) {
								if (!isComplete()) {
									setOutput(MIME_TYPE_HTML, response,
											convertStreamToString(is, UTF_8.name(), replacements));
								}
							}
						} else {

							if (Boolean.TRUE.equals(servletParameters.download)) {
								response.addHeader(CONTENT_DISPOSITION, ATTACHMENT + "; filename="
										+ URLEncoder.encode(getAttachmentFilename(file, driver), UTF_8.name()));
							}
							File videoFile = convert2video(file, driver, servletParameters, null, parentThread);

							ContentTypeAndFileExtensions mimeType = getMimeType(driver.getExtension());
							if (!isComplete()) {
								response.setContentLength((int) videoFile.length());
								setOutput(mimeType, response, newFileInputStream(videoFile));
							}
							videoFile.delete();
						}
					} else {
						response.addHeader(CONTENT_DISPOSITION,
								ATTACHMENT + "; filename=" + URLEncoder.encode(file.getName(), UTF_8.name()));
						ContentTypeAndFileExtensions mimeType = getMimeType(getFilenameSuffix(file.getName()));
						if (!isComplete()) {
							response.setContentLength((int) file.length());
							setOutput(mimeType, response, newFileInputStream(file));
						}
					}
				} catch (Throwable t) {
					error(getServletContext(), t, parentThread);
					if (!isComplete()) {
						response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
						setOutput(response, t);
					}
				}
			}

			private String getAttachmentFilename(final File file, AudioDriver driver) {
				return getFilenameWithoutSuffix(file.getName()) + driver.getExtension();
			}

			private AudioDriver getAudioDriverOfAudioFormat(HttpServletResponse response,
					ConvertServletParameters servletParameters) throws IOException {
				switch (Optional.ofNullable(servletParameters.config.getAudioSection().getAudio()).orElse(MP3)) {
				case WAV:
					return getThrottlingDriver(new WAVStreamDriver(response.getOutputStream()), servletParameters);
				case FLAC:
					return getThrottlingDriver(new FLACStreamDriver(response.getOutputStream()), servletParameters);
				case AAC:
					return getThrottlingDriver(new AACStreamDriver(response.getOutputStream()), servletParameters);
				case MP3:
				default:
					return getThrottlingDriver(new MP3StreamDriver(response.getOutputStream()), servletParameters);
				case SID_DUMP:
					return getThrottlingDriver(new SIDDumpStreamDriver(response.getOutputStream()), servletParameters);
				case SID_REG:
					return getThrottlingDriver(
							new SIDRegStreamDriver(response.getOutputStream(), servletParameters.getSidRegFormat()),
							servletParameters);
				}
			}

			private void convert2audio(File file, AudioDriver driver, ConvertServletParameters servletParameters)
					throws IOException, SidTuneError {
				info(getServletContext(), String.format("START file=%s", file.getAbsolutePath()), parentThread);

				ISidPlay2Section sidplay2Section = servletParameters.config.getSidplay2Section();
				IEmulationSection emulationSection = servletParameters.config.getEmulationSection();

				sidplay2Section.setHvsc(configuration.getSidplay2Section().getHvsc());
				emulationSection.setEngine(Engine.EMULATION);

				Player player = new Player(servletParameters.config);
				player.getC64().getVIC().setPalEmulation(PALEmulation.NONE);
				player.setSidDatabase(sidDatabase);
				player.setSTIL(stil);
				if (Boolean.TRUE.equals(servletParameters.download)) {
					sidplay2Section
							.setDefaultPlayLength(min(sidplay2Section.getDefaultPlayLength(), MAX_AUD_DOWNLOAD_LENGTH));
				}
				if (TEXT_TO_SPEECH && servletParameters.textToSpeechType != TextToSpeechType.NONE) {
					player.setMenuHook(new TextToSpeech(servletParameters.textToSpeechType,
							new TextToSpeechBean(file, player, getTextToSpeechLocale(servletParameters))));
				}

				Thread poolThread = Thread.currentThread();
				player.setAudioDriver(driver);
				player.setUncaughtExceptionHandler((thread, throwable) -> uncaughtExceptionHandler(getServletContext(),
						throwable, thread, parentThread, poolThread));
				player.setCheckDefaultLengthInRecordMode(Boolean.TRUE.equals(servletParameters.download));
				player.setCheckLoopOffInRecordMode(Boolean.TRUE.equals(servletParameters.download));
				player.setForceCheckSongLength(Boolean.TRUE.equals(servletParameters.download));

				insertCartridge(servletParameters, player);

				SidTune tune = SidTune.load(file);
				tune.getInfo().setSelectedSong(servletParameters.startSong);

				player.play(tune);
				player.stopC64(false);

				info(getServletContext(), String.format("END file=%s", file.getAbsolutePath()), parentThread);
			}

			private AudioDriver getAudioDriverOfVideoFormat(UUID uuid, ConvertServletParameters servletParameters) {
				switch (Optional.ofNullable(servletParameters.config.getAudioSection().getAudio()).orElse(FLV)) {
				case FLV:
				default:
					if (Boolean.TRUE.equals(servletParameters.download)) {
						return new FLVFileDriver();
					} else {
						return getThrottlingDriver(new FLVStreamDriver(RTMP_UPLOAD_URL + "/" + uuid),
								servletParameters);
					}
				case AVI:
					return new AVIFileDriver();
				case MP4:
					return new MP4FileDriver();
				}
			}

			private File convert2video(File file, AudioDriver driver, ConvertServletParameters servletParameters,
					UUID uuid, Thread... parentThreads) throws IOException, SidTuneError {
				info(getServletContext(), String.format("START file=%s, uuid=%s", file.getAbsolutePath(), uuid),
						parentThreads);

				File videoFile = null;
				ISidPlay2Section sidplay2Section = servletParameters.config.getSidplay2Section();
				IEmulationSection emulationSection = servletParameters.config.getEmulationSection();
				IC1541Section c1541Section = servletParameters.config.getC1541Section();

				emulationSection.setEngine(Engine.EMULATION);
				if (TAPE_FILE_FILTER.accept(file)) {
					c1541Section.setJiffyDosInstalled(false);
				}

				Player player = new Player(servletParameters.config);
				if (Boolean.TRUE.equals(servletParameters.download)) {
					sidplay2Section
							.setDefaultPlayLength(min(sidplay2Section.getDefaultPlayLength(), MAX_VID_DOWNLOAD_LENGTH));
					videoFile = createVideoFile(player, driver);
				} else {
					sidplay2Section.setDefaultPlayLength(RTMP_EXCEEDS_MAXIMUM_DURATION);
				}

				player.setAudioDriver(driver);
				player.setUncaughtExceptionHandler((thread, throwable) -> uncaughtExceptionHandler(getServletContext(),
						throwable, thread, parentThreads));
				player.setCheckDefaultLengthInRecordMode(Boolean.TRUE.equals(servletParameters.download));
				player.setCheckLoopOffInRecordMode(Boolean.TRUE.equals(servletParameters.download));
				player.setForceCheckSongLength(Boolean.TRUE.equals(servletParameters.download));

				ConvenienceResult convenienceResult = new Convenience(player).autostart(file,
						Convenience.LEXICALLY_FIRST_MEDIA, servletParameters.autostart, true);

				if (!player.getC64().isCartridge()) {
					insertCartridge(servletParameters, player);
				}
				if (uuid != null) {
					create(uuid, player, file, convenienceResult, servletParameters);
				}
				player.stopC64(false);

				info(getServletContext(), String.format("END file=%s, uuid=%s", file.getAbsolutePath(), uuid),
						parentThreads);
				return videoFile;
			}

			private AudioDriver getThrottlingDriver(AudioDriver audioDriver,
					ConvertServletParameters servletParameters) {
				if (Boolean.TRUE.equals(servletParameters.download)) {
					return audioDriver;
				} else {
					return new ProxyDriver(new ThrottlingDriver(), audioDriver);
				}
			}

			private Locale getTextToSpeechLocale(ConvertServletParameters servletParameters) {
				return servletParameters.textToSpeechLocale != null ? servletParameters.textToSpeechLocale
						: servletParameters.locale;
			}

			private void insertCartridge(ConvertServletParameters servletParameters, Player player) throws IOException {
				if (servletParameters.sfxSoundExpander) {
					player.insertCartridge(CartridgeType.SOUNDEXPANDER, servletParameters.sfxSoundExpanderType);
				} else if (servletParameters.reuSize != null || servletParameters.isREU()) {
					player.insertCartridge(CartridgeType.REU,
							servletParameters.reuSize != null ? servletParameters.reuSize : 16384);
				}
			}

			private File createVideoFile(Player player, AudioDriver driver) throws IOException {
				ISidPlay2Section sidplay2Section = player.getConfig().getSidplay2Section();

				File videoFile = File.createTempFile("jsidplay2video", driver.getExtension(),
						sidplay2Section.getTmpDir());
				videoFile.deleteOnExit();
				player.setRecordingFilenameProvider(tune -> getFilenameWithoutSuffix(videoFile.getAbsolutePath()));
				return videoFile;
			}

			private Map<String, String> createReplacements(ConvertServletParameters servletParameters,
					HttpServletRequest request, File file, UUID uuid) throws IOException, WriterException {
				String videoUrl = getVideoUrl(Boolean.TRUE.equals(servletParameters.useHls), uuid);
				String qrCodeImgTag = createQrCodeImgTag(createShareWithURL(request), "UTF-8", "png", 320, 320);

				Map<String, String> replacements = new HashMap<>();
				replacements.put("$uuid", uuid.toString());
				replacements.put("$qrCodeImgTag", qrCodeImgTag);
				replacements.put("$videoUrl", videoUrl);
				replacements.put("$hls", String.valueOf(Boolean.TRUE.equals(servletParameters.useHls)));
				replacements.put("$hlsType", servletParameters.getHlsType().name());
				replacements.put("$hlsScript", servletParameters.getHlsType().getScript());
				replacements.put("$hlsStyle", servletParameters.getHlsType().getStyle());
				replacements.put("$notYetPlayedTimeout", String.valueOf(RTMP_NOT_YET_PLAYED_TIMEOUT));
				replacements.put("$notifyForHLS", String.valueOf(NOTIFY_FOR_HLS));
				replacements.put("$min", Boolean.TRUE.equals(servletParameters.getUseDevTools()) ? "" : ".min");
				replacements.put("$lib",
						Boolean.TRUE.equals(servletParameters.getUseDevTools()) ? "lib" : "lib-minified");
				return replacements;
			}

			private String createQrCodeImgTag(String data, String charset, String imgFormat, int width, int height)
					throws IOException, WriterException {
				ByteArrayOutputStream qrCodeImgData = new ByteArrayOutputStream();
				ImageIO.write(createBarCodeImage(data, charset, width, height), imgFormat, qrCodeImgData);
				return format("<img src='data:image/%s;base64,%s'>", imgFormat,
						printBase64Binary(qrCodeImgData.toByteArray()));
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

			private String createShareWithURL(HttpServletRequest request) {
				StringBuilder result = new StringBuilder();
				result.append(request.getRequestURL());
				if (request.getQueryString() != null) {
					result.append("?");
					result.append(request.getQueryString());
				}
				return result.toString();
			}

			private void waitUntilVideoIsAvailable(UUID uuid)
					throws InterruptedException, URISyntaxException, IOException {
				URL url = new URI(getVideoUrl(true, uuid)).toURL();
				int retryCount = 0;
				while (retryCount++ < WAIT_FOR_VIDEO_AVAILABLE_RETRY_COUNT) {
					try {
						InternetUtil.openConnection(url, configuration.getSidplay2Section());
						// Give video production a jump start
						Thread.sleep(1000);
						return;
					} catch (InterruptedException | SocketTimeoutException e) {
						throw e;
					} catch (IOException e) {
						// connection not yet established, retry!
						Thread.sleep(500);
					}
				}
				throw new IOException("Video is still not available, please retry later!");
			}

		});
	}
}
