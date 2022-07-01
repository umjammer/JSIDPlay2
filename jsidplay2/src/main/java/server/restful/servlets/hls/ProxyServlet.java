package server.restful.servlets.hls;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.ZipFileUtils;
import server.restful.common.JSIDPlay2Servlet;
import ui.common.util.InternetUtil;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class ProxyServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.hls.ProxyServletParameters")
	public static class ServletParameters {

		@Parameter(descriptionKey = "URL")
		private String url;

	}

	public static final String PROXY_PATH = "/proxy";

	public ProxyServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + PROXY_PATH;
	}

	/**
	 * This class serves as kind of a proxy to make internal HTTP requests of HLS
	 * protocol through HTTPS to avoid mixed content in the browser.
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
//		super.doGet(request);   // Calls are very frequent, therefore we are silent here
		try {
			final ServletParameters servletParameters = new ServletParameters();

			JCommander commander = parseRequestParameters(request, response, servletParameters, getServletPath());
			if (servletParameters.url == null) {
				commander.usage();
				return;
			}
			URL url = new URL(servletParameters.url.substring(1).replaceFirst("/", "//"));

			URLConnection connection = InternetUtil.openConnection(url, configuration.getSidplay2Section());
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ZipFileUtils.copy(connection.getInputStream(), bos);

			for (String header : connection.getHeaderFields().keySet().stream().filter(Objects::nonNull)
					.collect(Collectors.toList())) {
				String value = connection.getHeaderFields().get(header).stream().findFirst().get();
				response.setHeader(header, value);
			}
			response.setContentLength(bos.size());
			response.getOutputStream().write(bos.toByteArray());
			response.setStatus(HttpServletResponse.SC_OK);

		} catch (IOException e) {
			response.setStatus(HttpServletResponse.SC_NOT_FOUND);
		} catch (Throwable t) {
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
		}
	}
}
