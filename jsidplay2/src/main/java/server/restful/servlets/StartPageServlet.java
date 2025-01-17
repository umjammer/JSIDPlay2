package server.restful.servlets;

import static java.nio.charset.StandardCharsets.UTF_8;
import static libsidutils.IOUtils.convertStreamToString;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_START_PAGE;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_HTML;
import static server.restful.common.IServletSystemProperties.BASE_URL;
import static server.restful.common.IServletSystemProperties.MAX_REQUESTS_PER_MINUTE;
import static server.restful.common.IServletSystemProperties.MIN_TIME_BETWEEN_REQUESTS;
import static server.restful.common.ServletUtil.error;
import static server.restful.common.filters.TimeBasedRateLimiterFilter.FILTER_PARAMETER_MAX_REQUESTS_PER_MINUTE;
import static server.restful.common.filters.TimeDistanceBasedRateLimiterFilter.FILTER_PARAMETER_MIN_TIME_BETWEEN_REQUESTS;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.filters.TimeBasedRateLimiterFilter;
import server.restful.common.filters.TimeDistanceBasedRateLimiterFilter;

@SuppressWarnings("serial")
@WebServlet(name = "StartPageServlet", displayName = "StartPageServlet", urlPatterns = CONTEXT_ROOT_START_PAGE, description = "Start page")
public class StartPageServlet extends JSIDPlay2Servlet {

	@Override
	public Map<Class<? extends HttpFilter>, Map<String, String>> getServletFiltersParameterMap() {
		Map<Class<? extends HttpFilter>, Map<String, String>> result = new HashMap<>();

		Map<String, String> timeDistanceBasedRateLimiterFilterParameters = new HashMap<>();
		timeDistanceBasedRateLimiterFilterParameters.put(FILTER_PARAMETER_MIN_TIME_BETWEEN_REQUESTS,
				String.valueOf(MIN_TIME_BETWEEN_REQUESTS));
		result.put(TimeDistanceBasedRateLimiterFilter.class, timeDistanceBasedRateLimiterFilterParameters);

		Map<String, String> timeBasedRateLimiterFilterParameters = new HashMap<>();
		timeBasedRateLimiterFilterParameters.put(FILTER_PARAMETER_MAX_REQUESTS_PER_MINUTE,
				String.valueOf(MAX_REQUESTS_PER_MINUTE));
		result.put(TimeBasedRateLimiterFilter.class, timeBasedRateLimiterFilterParameters);

		return result;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			Map<String, String> replacements = new HashMap<>();
			replacements.put("baseUrl", BASE_URL);

			try (InputStream is = StartPageServlet.class.getResourceAsStream("/doc/restful.html")) {
				setOutput(MIME_TYPE_HTML, response, convertStreamToString(is, UTF_8.name(), replacements));
			}
		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(getServletContext(), t);
			setOutput(response, t);
		}
	}

}
