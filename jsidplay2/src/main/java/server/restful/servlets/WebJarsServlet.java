package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_WEBJARS;
import static server.restful.common.ContentTypeAndFileExtensions.getMimeType;
import static server.restful.common.IServletSystemProperties.CACHE_CONTROL_RESPONSE_HEADER_CACHED;
import static server.restful.common.ServletUtil.error;

import java.io.IOException;
import java.io.InputStream;

import org.apache.http.HttpHeaders;

import com.beust.jcommander.Parameters;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.IOUtils;
import server.restful.common.ContentTypeAndFileExtensions;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.parameter.requestpath.WebJarsResourceRequestPathServletParameters;

@SuppressWarnings("serial")
@WebServlet(name = "WebJarsServlet", displayName = "WebJarsServlet", urlPatterns = CONTEXT_ROOT_WEBJARS
		+ "/*", description = "Get web jars")
public class WebJarsServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.WebJarsServletParameters")
	public static class WebJarsServletParameters extends WebJarsResourceRequestPathServletParameters {

	}

	/**
	 * Get web jars.
	 *
	 * E.g. http://haendel.ddns.net:8080/static/c64jukebox.vue
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			WebServlet webServlet = getClass().getAnnotation(WebServlet.class);

			final WebJarsServletParameters servletParameters = new WebJarsServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					webServlet);

			if (servletParameters.getHelp() || parser.hasException()) {
				parser.usage();
				return;
			}
			try (InputStream source = servletParameters.getResource()) {

				response.setHeader(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_RESPONSE_HEADER_CACHED);
				ContentTypeAndFileExtensions mimeType = getMimeType(IOUtils.getFilenameSuffix(request.getPathInfo()));
				setOutput(mimeType, response, source);
			}
		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(getServletContext(), t);
			setOutput(response, t);
		}
	}

}
