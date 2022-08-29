package server.restful.servlets;

import static libsidutils.PathUtils.getFilenameSuffix;
import static libsidutils.ZipFileUtils.copy;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.ATTACHMENT;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.CONTENT_DISPOSITION;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.ContentTypeAndFileExtensions.getMimeType;

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
public class SaveAsServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.SaveAsServletParameters")
	public static class ServletParameters {

		@Parameter(names = { "--filename" }, descriptionKey = "FILENAME", order = -1)
		private String filename;

	}

	public static final String SAVEAS_PATH = "/save_as";

	public SaveAsServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + SAVEAS_PATH;
	}

	/**
	 * Save request content as file.
	 *
	 * <BR>
	 * {@code
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/save_as&filename=playlist.js2web
	 * }
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doPost(request);
		try {
			final ServletParameters servletParameters = new ServletParameters();

			JCommander commander = parseRequestParameters(request, response, servletParameters, getServletPath());
			if (servletParameters.filename == null) {
				commander.usage();
				return;
			}
			response.setContentType(getMimeType(getFilenameSuffix(servletParameters.filename)).toString());
			response.addHeader("x-suggested-filename", servletParameters.filename);
			response.addHeader(CONTENT_DISPOSITION, ATTACHMENT + "; filename=" + servletParameters.filename);
			copy(request.getInputStream(), response.getOutputStream());

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}
}
