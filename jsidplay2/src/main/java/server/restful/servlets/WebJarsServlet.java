package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_WEBJARS;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.ContentTypeAndFileExtensions.getMimeType;
import static server.restful.common.IServletSystemProperties.STATIC_RES_MAX_AGE;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.http.HttpHeaders;

import com.beust.jcommander.Parameters;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.IOUtils;
import server.restful.common.ContentTypeAndFileExtensions;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.parameter.requestpath.WebJarsResourceRequestPathServletParameters;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class WebJarsServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.WebJarsServletParameters")
	public static class WebJarsServletParameters extends WebJarsResourceRequestPathServletParameters {

	}

	public WebJarsServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_WEBJARS;
	}

	@Override
	public boolean isSecured() {
		return false;
	}

	/**
	 * Get VUE web page.
	 *
	 * E.g. http://haendel.ddns.net:8080/static/c64jukebox.vue
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doGet(request);
		try {
			final WebJarsServletParameters servletParameters = new WebJarsServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					getServletPath());

			if (parser.hasException()) {
				parser.usage();
				return;
			}
			try (InputStream source = servletParameters.getResource()) {

				ContentTypeAndFileExtensions mimeType = getMimeType(IOUtils.getFilenameSuffix(request.getPathInfo()));
				response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=" + STATIC_RES_MAX_AGE);
				response.setContentType(mimeType.toString());
				IOUtils.copy(source, response.getOutputStream());
			}
		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

}
