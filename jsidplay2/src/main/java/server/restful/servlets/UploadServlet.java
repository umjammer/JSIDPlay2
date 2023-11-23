package server.restful.servlets;

import static libsidutils.IOUtils.copy;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.ROLE_USER;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.IServletSystemProperties.MAX_UPLOADS_IN_PARALLEL;
import static server.restful.common.IServletSystemProperties.UPLOADSERVLET_FILE_SIZE_THRESHOLD;
import static server.restful.common.IServletSystemProperties.UPLOADSERVLET_MAX_FILE_SIZE;
import static server.restful.common.IServletSystemProperties.UPLOADSERVLET_MAX_REQUEST_SIZE;
import static server.restful.common.IServletSystemProperties.UPLOAD_ASYNC_TIMEOUT;
import static server.restful.common.ServletUtil.error;
import static ui.common.Convenience.LEXICALLY_FIRST_MEDIA;
import static ui.common.Convenience.MACOSX;
import static ui.common.Convenience.TOP_LEVEL_FIRST_COMPARATOR;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiPredicate;
import java.util.zip.GZIPInputStream;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidplay.config.ISidPlay2Section;
import libsidutils.IOUtils;
import libsidutils.ZipFileUtils;
import net.java.truevfs.access.TArchiveDetector;
import net.java.truevfs.access.TFile;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.UploadContents;
import server.restful.common.async.DefaultThreadFactory;
import server.restful.common.async.HttpAsyncContextRunnable;
import server.restful.common.parameter.ServletParameterParser;
import ui.common.util.Extract7ZipUtil;

@SuppressWarnings("serial")
@WebServlet(name = "UploadServlet", asyncSupported = true, urlPatterns = CONTEXT_ROOT_SERVLET + "/upload/*")
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { ROLE_USER, ROLE_ADMIN }))
@MultipartConfig(maxFileSize = UPLOADSERVLET_MAX_FILE_SIZE, maxRequestSize = UPLOADSERVLET_MAX_REQUEST_SIZE, fileSizeThreshold = UPLOADSERVLET_FILE_SIZE_THRESHOLD)
public class UploadServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.UploadServletParameters")
	public static class UploadServletParameters {

		private String filePath;

		public String getFilePath() {
			return filePath;
		}

		@Parameter(descriptionKey = "FILE_PATH", required = true)
		public void setFilePath(String filePath) {
			this.filePath = filePath;
		}

	}

	private ExecutorService executorService;

	@Override
	public void init() throws ServletException {
		executorService = Executors.newFixedThreadPool(MAX_UPLOADS_IN_PARALLEL, new DefaultThreadFactory("/upload"));
	}

	@Override
	public void destroy() {
		executorService.shutdown();
	}

	/**
	 * Upload any C64 content to a temporary server location to play back (can be
	 * any C64 resource even packed contents).
	 *
	 * E.g.
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/upload/Outro.sid
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		AsyncContext asyncContext = request.startAsync(request, response);
		asyncContext.setTimeout(UPLOAD_ASYNC_TIMEOUT);

		executorService.execute(new HttpAsyncContextRunnable(asyncContext, getServletContext()) {

			public void run(HttpServletRequest request, HttpServletResponse response) throws IOException {
				try {
					final UploadServletParameters servletParameters = new UploadServletParameters();

					ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
							UploadServlet.class.getAnnotation(WebServlet.class));

					final String filePath = servletParameters.getFilePath();
					if (filePath == null || parser.hasException()) {
						parser.usage();
						return;
					}

					UploadContents uploadContents = getInput(request, UploadContents.class);

					File uploadFile = createUploadFile(uploadContents, filePath);

					setOutput(MIME_TYPE_JSON, response, String.valueOf(uploadFile).replace("\\", "/"));

				} catch (Throwable t) {
					error(getServletContext(), t);
					response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
					setOutput(response, t);
				}
			}

			private File createUploadFile(UploadContents uploadContents, final String filePath) throws IOException {
				ISidPlay2Section sidplay2Section = configuration.getSidplay2Section();

				File tmpDir = new File(sidplay2Section.getTmpDir(), UUID.randomUUID().toString());
				tmpDir.mkdir();

				File file = new File(tmpDir, new File(filePath).getName());
				file.deleteOnExit();

				try (OutputStream fos = new FileOutputStream(file)) {
					copy(new ByteArrayInputStream(uploadContents.getContents()), fos);
				}

				TFile tFile = new TFile(file);
				File uploadResult = null;

				if (file.getName().toLowerCase(Locale.ENGLISH).endsWith(".zip")) {
					// uncompress zip
					TFile.cp_rp(tFile, tmpDir, TArchiveDetector.ALL);
					uploadResult = getUploadResult(tmpDir, tFile, LEXICALLY_FIRST_MEDIA, null);
				} else if (file.getName().toLowerCase(Locale.ENGLISH).endsWith(".gz")) {
					// uncompress gzip
					File dst = new File(tmpDir, IOUtils.getFilenameWithoutSuffix(file.getName()));
					try (InputStream is = new GZIPInputStream(ZipFileUtils.newFileInputStream(file))) {
						TFile.cp(is, dst);
					}
					uploadResult = getUploadResult(tmpDir, tmpDir, LEXICALLY_FIRST_MEDIA, null);
				} else if (file.getName().toLowerCase(Locale.ENGLISH).endsWith("7z")) {
					// uncompress 7zip
					Extract7ZipUtil extract7Zip = new Extract7ZipUtil(tFile, tmpDir);
					extract7Zip.extract();
					uploadResult = getUploadResult(tmpDir, tmpDir, LEXICALLY_FIRST_MEDIA, null);
				} else {
					uploadResult = file;
				}
				return uploadResult;
			}

			private File getUploadResult(File dir, File file, BiPredicate<File, File> mediaTester, File uploadResult) {
				final File[] listFiles = dir.listFiles();
				if (listFiles == null) {
					return null;
				}
				final List<File> asList = Arrays.asList(listFiles);
				asList.sort(TOP_LEVEL_FIRST_COMPARATOR);
				for (File member : asList) {
					File memberFile = new File(dir, member.getName());
					if (memberFile.isFile() && (CART_FILE_FILTER.accept(memberFile)
							|| AUDIO_TUNE_FILE_FILTER.accept(memberFile) || VIDEO_TUNE_FILE_FILTER.accept(memberFile)
							|| DISK_FILE_FILTER.accept(memberFile) || TAPE_FILE_FILTER.accept(memberFile))) {

						if (mediaTester.test(memberFile, uploadResult)) {
							uploadResult = memberFile;
						}
					} else if (memberFile.isDirectory() && !memberFile.getName().equals(MACOSX)) {
						File toAttachChild = getUploadResult(memberFile, new TFile(memberFile), mediaTester,
								uploadResult);
						if (toAttachChild != null) {
							uploadResult = toAttachChild;
						}
					}
				}
				return uploadResult;
			}

		});
	}

}
