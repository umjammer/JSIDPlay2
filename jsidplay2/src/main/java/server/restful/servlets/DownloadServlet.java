package server.restful.servlets;

import static libsidutils.PathUtils.getFilenameSuffix;
import static libsidutils.ZipFileUtils.copy;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.ATTACHMENT;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.CONTENT_DISPOSITION;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.ContentTypeAndFileExtensions.getMimeType;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.restful.common.JSIDPlay2Servlet;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class DownloadServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.DownloadServletParameters")
	public static class ServletParameters {

		@Parameter(description = "filePath")
		private String filePath;

	}

	public static final String DOWNLOAD_PATH = "/download";

	public DownloadServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + DOWNLOAD_PATH;
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
			final ServletParameters servletParameters = new ServletParameters();

			JCommander commander = parseRequestParameters(request, response, servletParameters, getServletPath());
			if (servletParameters.filePath == null) {
				commander.usage();
				return;
			}
			final File file = getAbsoluteFile(servletParameters.filePath, request.isUserInRole(ROLE_ADMIN));

			response.setContentType(getMimeType(getFilenameSuffix(servletParameters.filePath)).toString());
			response.addHeader(CONTENT_DISPOSITION, ATTACHMENT + "; filename=" + file.getName());
			copy(file, response.getOutputStream());

		} catch (Throwable t) {
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}

}
