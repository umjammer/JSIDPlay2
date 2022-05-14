package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_STATIC;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.ContentTypeAndFileExtensions.getMimeType;
import static server.restful.common.IServletSystemProperties.BASE_URL;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.PathUtils;
import libsidutils.ZipFileUtils;
import server.restful.JSIDPlay2Server;
import server.restful.common.JSIDPlay2Servlet;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class StaticServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.StaticServletParameters")
	public static class ServletParameters {

		@Parameter
		private String filePath;

	}

	public StaticServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_STATIC;
	}

	/**
	 * Get VUE web page.
	 *
	 * E.g. http://haendel.ddns.net:8080/static/hvsc.vue
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doGet(request);
		try {
			final ServletParameters servletParameters = new ServletParameters();

			JCommander commander = parseRequestParameters(request, response, servletParameters,
					BASE_URL + getServletPath() + "/<filePath>");
			if (servletParameters.filePath == null) {
				commander.usage();
				return;
			}

			try (InputStream source = getResourceAsStream(servletParameters.filePath)) {
				setOutput(response, getMimeType(PathUtils.getFilenameSuffix(servletParameters.filePath)),
						ZipFileUtils.convertStreamToString(source, "UTF-8"));
			}
		} catch (Throwable t) {
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}

	private InputStream getResourceAsStream(String filePath) throws FileNotFoundException {
		if (filePath.startsWith("/")) {
			filePath = filePath.substring(1);
		}
		File localFile = new File(configuration.getSidplay2Section().getTmpDir(), filePath);
		if (localFile.exists() && localFile.canRead()) {
			return new FileInputStream(localFile);
		}
		InputStream resourceAsStream = JSIDPlay2Server.class.getResourceAsStream("/server/restful/webapp/" + filePath);
		if (resourceAsStream == null) {
			throw new FileNotFoundException(filePath + " (No such file or directory)");
		}
		return resourceAsStream;
	}

}
