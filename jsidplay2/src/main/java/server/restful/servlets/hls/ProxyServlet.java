package server.restful.servlets.hls;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.IServletSystemProperties.HLS_DOWNLOAD_URL;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;

import com.beust.jcommander.Parameters;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.IOUtils;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.parameter.requestpath.URLRequestPathServletParameters;
import ui.common.util.InternetUtil;

@SuppressWarnings("serial")
@WebServlet(name = "ProxyServlet", urlPatterns = CONTEXT_ROOT_SERVLET + "/proxy/*")
public class ProxyServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.hls.ProxyServletParameters")
	public static class ProxyServletParameters extends URLRequestPathServletParameters {

	}

	@Override
	public boolean isSecured() {
		return false;
	}

	/**
	 * This class serves as kind of a proxy to make internal HTTP requests of HLS
	 * protocol through HTTPS to avoid mixed content in the browser.
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			final ProxyServletParameters servletParameters = new ProxyServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					getClass().getAnnotation(WebServlet.class));

			if (parser.hasException()) {
				parser.usage();
				return;
			}
			URL url = servletParameters.getUrl();

			if (!url.toExternalForm().startsWith(HLS_DOWNLOAD_URL)) {
				throw new IOException();
			}
			URLConnection connection = InternetUtil.openConnection(url, configuration.getSidplay2Section());
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			IOUtils.copy(connection.getInputStream(), bos);

			connection.getHeaderFields().entrySet().stream().filter(entry -> Objects.nonNull(entry.getKey()))
					.forEach(entry -> entry.getValue().forEach(value -> response.setHeader(entry.getKey(), value)));

			response.setContentLength(bos.size());
			response.getOutputStream().write(bos.toByteArray());

		} catch (IOException e) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}
}
