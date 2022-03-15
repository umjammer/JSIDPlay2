package server.restful.servlets;

import static java.lang.String.format;
import static java.lang.Thread.getAllStackTraces;
import static java.nio.charset.StandardCharsets.UTF_8;
import static javax.xml.bind.DatatypeConverter.printBase64Binary;
import static libsidplay.components.keyboard.KeyTableEntry.SPACE;
import static libsidutils.PathUtils.getFilenameSuffix;
import static libsidutils.PathUtils.getFilenameWithoutSuffix;
import static libsidutils.ZipFileUtils.convertStreamToString;
import static libsidutils.ZipFileUtils.copy;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.ATTACHMENT;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.CONTENT_DISPOSITION;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.common.CleanupPlayerTimerTask.create;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_HTML;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.ContentTypeAndFileExtensions.getMimeType;
import static server.restful.common.IServletSystemProperties.MAX_CONVERT_IN_PARALLEL;
import static server.restful.common.IServletSystemProperties.MAX_LENGTH;
import static server.restful.common.IServletSystemProperties.MAX_RTMP_IN_PARALLEL;
import static server.restful.common.IServletSystemProperties.PRESS_SPACE_INTERVALL;
import static server.restful.common.IServletSystemProperties.RTMP_EXTERNAL_DOWNLOAD_URL;
import static server.restful.common.IServletSystemProperties.RTMP_INTERNAL_DOWNLOAD_URL;
import static server.restful.common.IServletSystemProperties.RTMP_NOT_YET_PLAYED_TIMEOUT;
import static server.restful.common.IServletSystemProperties.RTMP_UPLOAD_URL;
import static server.restful.common.IServletSystemProperties.WAIT_FOR_RTMP;
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
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.apache.http.HttpHeaders;

import com.beust.jcommander.JCommander;
import com.google.zxing.WriterException;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidplay.common.Event;
import libsidplay.components.cart.CartridgeType;
import libsidplay.config.IC1541Section;
import libsidplay.config.IConfig;
import libsidplay.config.ISidPlay2Section;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.PathUtils;
import libsidutils.siddatabase.SidDatabase;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.ServletParameters;
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
import sidplay.player.State;
import ui.common.Convenience;
import ui.common.filefilter.AudioTuneFileFilter;
import ui.common.filefilter.CartFileFilter;
import ui.common.filefilter.DiskFileFilter;
import ui.common.filefilter.TapeFileFilter;
import ui.common.filefilter.VideoTuneFileFilter;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class ConvertServlet extends JSIDPlay2Servlet {

	public static final String CONVERT_PATH = "/convert";

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
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/convert/C64Music/DEMOS/0-9/1_45_Tune.sid
	 * } <BR>
	 * E.g. stream video<BR>
	 * {@code
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/convert/Demos/ALGODANCER2/ALGODANCER2.d64?defaultLength=00:30&enableSidDatabase=true&single=true&loop=false&bufferSize=65536&sampling=RESAMPLE&frequency=MEDIUM&defaultEmulation=RESIDFP&defaultModel=MOS8580&filter6581=FilterAlankila6581R4AR_3789&stereoFilter6581=FilterAlankila6581R4AR_3789&thirdFilter6581=FilterAlankila6581R4AR_3789&filter8580=FilterAlankila6581R4AR_3789&stereoFilter8580=FilterAlankila6581R4AR_3789&thirdFilter8580=FilterAlankila6581R4AR_3789&reSIDfpFilter6581=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter6581=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter6581=FilterAlankila6581R4AR_3789&reSIDfpFilter8580=FilterAlankila6581R4AR_3789&reSIDfpStereoFilter8580=FilterAlankila6581R4AR_3789&reSIDfpThirdFilter8580=FilterAlankila6581R4AR_3789&digiBoosted8580=true
	 * }
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doGet(request);
		try {
			String filePath = request.getPathInfo();
			File file = getAbsoluteFile(filePath, request.isUserInRole(ROLE_ADMIN));
			if (audioTuneFileFilter.accept(file)) {

				final ServletParameters servletParameters = new ServletParameters();
				final IniConfig config = servletParameters.getConfig();

				String[] args = getRequestParameters(request);

				JCommander.newBuilder().addObject(servletParameters).programName(getClass().getName()).build()
						.parse(args);

				Audio audio = getAudioFormat(config);
				AudioDriver driver = getAudioDriverOfAudioFormat(audio, response.getOutputStream());

				response.setContentType(getMimeType(driver.getExtension()).toString());
				if (Boolean.TRUE.equals(servletParameters.getDownload())) {
					response.addHeader(CONTENT_DISPOSITION, ATTACHMENT + "; filename="
							+ getFilenameWithoutSuffix(file.getName()) + driver.getExtension());
				}
				convert2audio(config, file, driver, servletParameters);
			} else if (videoTuneFileFilter.accept(file) || cartFileFilter.accept(file) || diskFileFilter.accept(file)
					|| tapeFileFilter.accept(file)) {

				final ServletParameters servletParameters = new ServletParameters();
				final IniConfig config = servletParameters.getConfig();

				String[] args = getRequestParameters(request);

				JCommander.newBuilder().addObject(servletParameters).programName(getClass().getName()).build()
						.parse(args);

				UUID uuid = UUID.randomUUID();

				Audio audio = getVideoFormat(config);
				AudioDriver driver = getAudioDriverOfVideoFormat(audio, uuid, servletParameters.getDownload());

				if (Boolean.FALSE.equals(servletParameters.getDownload()) && audio == FLV) {
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
								servletParameters.setStarted(true);
							}
						}, "RTMP").start();
						while (!servletParameters.isStarted()) {
							Thread.yield();
						}
						response.setHeader(HttpHeaders.PRAGMA, "no-cache");
						response.setHeader(HttpHeaders.CACHE_CONTROL, "private, no-store, no-cache, must-revalidate");
						response.setContentType(MIME_TYPE_HTML.toString());

						Map<String, String> replacements = createReplacements(request, file, uuid);
						try (InputStream is = ConvertServlet.class
								.getResourceAsStream("/server/restful/webapp/convert.html")) {
							response.getWriter().println(convertStreamToString(is, UTF_8.name(), replacements));
						}
					} else {
						response.setContentType(MIME_TYPE_TEXT.toString());
						response.sendError(429, "Too Many Requests");
						return;
					}
				} else {
					response.setContentType(getMimeType(driver.getExtension()).toString());
					if (Boolean.TRUE.equals(servletParameters.getDownload())) {
						response.addHeader(CONTENT_DISPOSITION, ATTACHMENT + "; filename="
								+ getFilenameWithoutSuffix(file.getName()) + driver.getExtension());
					}
					File videoFile = convert2video(config, file, driver, servletParameters, null);
					copy(videoFile, response.getOutputStream());
					videoFile.delete();
				}
			} else {
				response.setContentType(getMimeType(getFilenameSuffix(filePath)).toString());
				response.addHeader(CONTENT_DISPOSITION, ATTACHMENT + "; filename=" + new File(filePath).getName());
				copy(getAbsoluteFile(filePath, request.isUserInRole(ROLE_ADMIN)), response.getOutputStream());
			}
		} catch (Throwable t) {
			error(t);
			response.setContentType(MIME_TYPE_TEXT.toString());
			t.printStackTrace(new PrintWriter(response.getWriter()));
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}

	private String[] getRequestParameters(HttpServletRequest request) {
		return Collections.list(request.getParameterNames()).stream()
				.map(name -> Arrays.asList("--" + name,
						Arrays.asList(request.getParameterValues(name)).stream().findFirst().orElse("?")))
				.flatMap(List::stream).toArray(String[]::new);
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
			return new SIDRegStreamDriver(outputstream);
		}
	}

	private void convert2audio(IConfig config, File file, AudioDriver driver, ServletParameters servletParameters)
			throws IOException, SidTuneError {
		Player player = new Player(config);
		setSIDDatabase(player);
		player.setAudioDriver(driver);
		player.setDefaultLengthInRecordMode(true);
		player.setCheckLoopOffInRecordMode(Boolean.TRUE.equals(servletParameters.getDownload()));
		player.setForceCheckSongLength(true);

		SidTune tune = SidTune.load(file);
		tune.getInfo().setSelectedSong(servletParameters.getSong());
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
			UUID liveVideoUuid) throws IOException, SidTuneError {
		File videoFile = null;

		ISidPlay2Section sidplay2Section = config.getSidplay2Section();
		IC1541Section c1541Section = config.getC1541Section();

		c1541Section.setJiffyDosInstalled(Boolean.TRUE.equals(servletParameters.getJiffydos()));

		Player player = new Player(config);
		if (liveVideoUuid != null) {
			setSIDDatabase(player);
		} else {
			sidplay2Section.setDefaultPlayLength(Math.min(sidplay2Section.getDefaultPlayLength(), MAX_LENGTH));
			videoFile = createVideoFile(player, driver);
		}
		player.setAudioDriver(driver);
		player.setDefaultLengthInRecordMode(liveVideoUuid == null);
		player.setCheckLoopOffInRecordMode(Boolean.TRUE.equals(servletParameters.getDownload()));
		player.setForceCheckSongLength(true);

		addPressSpaceListener(player);
		new Convenience(player).autostart(file, Convenience.LEXICALLY_FIRST_MEDIA, null);
		if (servletParameters.getReuSize() != null) {
			player.insertCartridge(CartridgeType.REU, servletParameters.getReuSize());
		}
		if (liveVideoUuid != null) {
			create(liveVideoUuid, player, file, resourceBundle);
			servletParameters.setStarted(true);
		}
		player.stopC64(false);
		return videoFile;
	}

	private void setSIDDatabase(Player player) throws IOException {
		File root = configuration.getSidplay2Section().getHvsc();
		if (root != null) {
			player.setSidDatabase(new SidDatabase(root));
		}
	}

	private File createVideoFile(Player player, AudioDriver driver) throws IOException {
		ISidPlay2Section sidplay2Section = player.getConfig().getSidplay2Section();

		File videoFile = File.createTempFile("jsidplay2video", driver.getExtension(), sidplay2Section.getTmpDir());
		videoFile.deleteOnExit();
		player.setRecordingFilenameProvider(tune -> PathUtils.getFilenameWithoutSuffix(videoFile.getAbsolutePath()));
		return videoFile;
	}

	private void addPressSpaceListener(Player player) {
		player.stateProperty().addListener(event -> {
			if (event.getNewValue() == State.START) {
				player.getC64().getEventScheduler().schedule(new Event("Key Pressed") {

					@Override
					public void event() throws InterruptedException {
						// press space every N seconds
						player.getC64().getKeyboard().keyPressed(SPACE);

						player.getC64().getEventScheduler().schedule(new Event("Key Released") {
							@Override
							public void event() throws InterruptedException {
								player.getC64().getKeyboard().keyReleased(SPACE);
							}
						}, (long) (player.getC64().getClock().getCpuFrequency()));

						player.getC64().getEventScheduler().schedule(this,
								PRESS_SPACE_INTERVALL * (long) player.getC64().getClock().getCpuFrequency());
					}

				}, PRESS_SPACE_INTERVALL * (long) player.getC64().getClock().getCpuFrequency());
			}
		});
	}

	private Map<String, String> createReplacements(HttpServletRequest request, File file, UUID uuid)
			throws IOException, WriterException {
		String rtmpUrl = getRTMPUrl(request.getRemoteAddr(), uuid);
		String createQrCodeImgTag = createQrCodeImgTag(rtmpUrl, "UTF-8", "png", 320, 320);

		Map<String, String> result = new HashMap<>();
		result.put("$uuid", uuid.toString());
		result.put("$barcodeImg", createQrCodeImgTag);
		result.put("$barcodeImgTop", "8px");
		result.put("$barcodeImgRight", "-328px");
		result.put("$rtmp", rtmpUrl);
		result.put("$waitForRTMP", String.valueOf(WAIT_FOR_RTMP));
		result.put("$notYetPlayedTimeout", String.valueOf(RTMP_NOT_YET_PLAYED_TIMEOUT));
		result.put("$filename", file.getName());
		return result;
	}

	private String createQrCodeImgTag(String data, String charset, String imgFormat, int width, int height)
			throws IOException, WriterException {
		ByteArrayOutputStream qrCodeImgData = new ByteArrayOutputStream();
		ImageIO.write(createBarCodeImage(data, charset, width, height), imgFormat, qrCodeImgData);
		return format("<img src='data:image/%s;base64,%s'>", imgFormat, printBase64Binary(qrCodeImgData.toByteArray()));
	}

	private String getRTMPUrl(String remoteAddress, UUID uuid) {
		boolean isLocal = remoteAddress.startsWith("192.168.") || remoteAddress.equals("127.0.0.1");
		String baseUrl = isLocal ? RTMP_INTERNAL_DOWNLOAD_URL : RTMP_EXTERNAL_DOWNLOAD_URL;
		if (baseUrl.startsWith("http")) {
			// HLS protocol
			return baseUrl + "/" + uuid + ".m3u8";
		}
		// RTMP protocol
		return baseUrl + "/" + uuid;
	}

}
