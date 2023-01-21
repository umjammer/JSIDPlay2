package server.restful.servlets;

import static java.nio.charset.StandardCharsets.UTF_8;
import static libsidutils.ZipFileUtils.convertStreamToString;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_START_PAGE;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_HTML;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.IServletSystemProperties.BASE_URL;
import static server.restful.common.IServletSystemProperties.MAX_START_PAGE_IN_PARALLEL;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.filters.LimitRequestServletFilter;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class StartPageServlet extends JSIDPlay2Servlet {

	public StartPageServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_START_PAGE;
	}

	@Override
	public Optional<Filter> getServletFilter() {
		return Optional.of(new LimitRequestServletFilter(MAX_START_PAGE_IN_PARALLEL));
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doGet(request);
		try {
			Map<String, String> replacements = new HashMap<>();
			replacements.put("https://haendel.ddns.net:8443", BASE_URL);

			try (InputStream is = StartPageServlet.class.getResourceAsStream("/doc/restful.html")) {
				setOutput(response, MIME_TYPE_HTML, convertStreamToString(is, UTF_8.name(), replacements));
			}
		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

}
