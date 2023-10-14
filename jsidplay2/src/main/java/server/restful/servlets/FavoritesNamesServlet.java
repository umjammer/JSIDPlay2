package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.filters.RequestLogFilter.FILTER_PARAMETER_SERVLET_NAME;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.filters.RequestLogFilter;
import ui.entities.config.FavoritesSection;

@SuppressWarnings("serial")
public class FavoritesNamesServlet extends JSIDPlay2Servlet {

	public static final String FAVORITES_NAMES_PATH = "/favorites_names";

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + FAVORITES_NAMES_PATH;
	}

	@Override
	public List<Filter> getServletFilters() {
		return Arrays.asList(new RequestLogFilter());
	}

	@Override
	public Map<String, String> getServletFiltersParameterMap() {
		Map<String, String> result = new HashMap<>();
		result.put(FILTER_PARAMETER_SERVLET_NAME, getClass().getSimpleName());
		return result;
	}

	@Override
	public boolean isSecured() {
		return true;
	}

	/**
	 * Get contents of the first SID favorites tab.
	 *
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/favorites
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			List<String> favoritesNames = getFavoritesNames();

			setOutput(response, MIME_TYPE_JSON, OBJECT_MAPPER.writer().writeValueAsString(favoritesNames));

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

	private List<String> getFavoritesNames() {
		List<String> filters = configuration.getFavorites().stream().map(FavoritesSection::getName)
				.collect(Collectors.toList());
		// Small hack to add entries (only supported in C64 Jukebox)
		filters.add("Specials");
		return filters;
	}

}
