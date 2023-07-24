package server.restful.servlets;

import static libsidutils.IOUtils.getFilenameSuffix;
import static libsidutils.ZipFileUtils.newFileInputStream;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.ATTACHMENT;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.CONTENT_DISPOSITION;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.ContentTypeAndFileExtensions.getMimeType;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.IOUtils;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.parameter.requestpath.FileRequestPathServletParameters;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class DownloadServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.DownloadServletParameters")
	public static class DownloadServletParameters extends FileRequestPathServletParameters {

	}

	public static final String DOWNLOAD_PATH = "/download";

	public DownloadServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + DOWNLOAD_PATH;
	}

	@Override
	public boolean isSecured() {
		return true;
	}

	/**
	 * Download SID.
	 *
	 * E.g.
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/download/C64Music/MUSICIANS/D/DRAX/Worktunes/Outro.sid
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doGet(request);
		try {
			final DownloadServletParameters servletParameters = new DownloadServletParameters();

			JCommander commander = parseRequestParameters(request, response, servletParameters, getServletPath());

			final File file = servletParameters.getFile(this, commander, request);
			if (file == null) {
				commander.usage();
				return;
			}
			response.setContentType(getMimeType(getFilenameSuffix(servletParameters.getFilePath())).toString());
			response.addHeader(CONTENT_DISPOSITION,
					ATTACHMENT + "; filename=" + URLEncoder.encode(file.getName(), StandardCharsets.UTF_8.name()));
			IOUtils.copy(newFileInputStream(file), response.getOutputStream());

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

}
