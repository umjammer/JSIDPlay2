package server.restful.servlets.hls;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.common.IServletSystemProperties.HLS_DOWNLOAD_URL;
import static server.restful.common.ServletUtil.error;
import static server.restful.common.ServletUtil.info;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

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

	private static final Logger LOG = Logger.getLogger(ProxyServlet.class.getName());

	@Parameters(resourceBundle = "server.restful.servlets.hls.ProxyServletParameters")
	public static class ProxyServletParameters extends URLRequestPathServletParameters {

	}

	/**
	 * This class serves as kind of a proxy to make internal HTTP requests of HLS
	 * protocol through HTTPS to avoid mixed content in the browser.
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			WebServlet webServlet = getClass().getAnnotation(WebServlet.class);

			final ProxyServletParameters servletParameters = new ProxyServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					webServlet);

			if (parser.hasException()) {
				parser.usage();
				return;
			}
			URL url = servletParameters.getUrl();

			if (!url.toExternalForm().startsWith(HLS_DOWNLOAD_URL)) {
				throw new IOException();
			}
			if (LOG.isLoggable(Level.FINEST)) {
				info(getServletContext(), String.format("proxy: URL: %s", url));
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
			error(getServletContext(), t);
			setOutput(response, t);
		}
	}
}
