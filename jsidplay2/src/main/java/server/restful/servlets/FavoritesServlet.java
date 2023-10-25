package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.ROLE_USER;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.fasterxml.jackson.core.JsonProcessingException;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.IOUtils;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.converter.WebResourceConverter;
import server.restful.common.filters.RequestLogFilter;
import server.restful.common.parameter.ServletParameterParser;
import ui.entities.collection.HVSCEntry;
import ui.entities.config.FavoritesSection;

@SuppressWarnings("serial")
@WebServlet(name = "FavoritesServlet", urlPatterns = CONTEXT_ROOT_SERVLET + "/favorites")
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { ROLE_USER, ROLE_ADMIN }))
public class FavoritesServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.FavoritesServletParameters")
	public static class FavoritesServletParameters {

		private Integer favoritesNumber = 0;

		public Integer getFavoritesNumber() {
			return favoritesNumber;
		}

		@Parameter(names = { "--favoritesNumber" }, descriptionKey = "FAVORITES_NUMBER", order = -2)
		public void setFavoritesNumber(Integer favoritesNumber) {
			this.favoritesNumber = favoritesNumber;
		}

	}

	@Override
	public List<Filter> getServletFilters() {
		return Arrays.asList(new RequestLogFilter());
	}

	@Override
	public Map<String, String> getServletFiltersParameterMap() {
		Map<String, String> result = new HashMap<>();
		return result;
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
			final FavoritesServletParameters servletParameters = new FavoritesServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					getClass().getAnnotation(WebServlet.class));

			if (parser.hasException()) {
				parser.usage();
				return;
			}
			final Integer favoritesNumber = servletParameters.getFavoritesNumber();

			if (favoritesNumber < configuration.getFavorites().size()) {
				List<String> favorites = getFavoritesByNumber(favoritesNumber);

				setOutput(response, MIME_TYPE_JSON, OBJECT_MAPPER.writer().writeValueAsString(favorites));
			} else {
				String favorites = getSpecialFavorites(favoritesNumber);

				setOutput(response, MIME_TYPE_JSON, OBJECT_MAPPER.writer().writeValueAsString(favorites));
			}

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

	private List<String> getFavoritesByNumber(Integer favoritesNumber) {
		List<String> filters = configuration.getFavorites().stream()
				.filter(favorites -> configuration.getFavorites().indexOf(favorites) == favoritesNumber).findFirst()
				.map(FavoritesSection::getFavorites).orElseGet(Collections::emptyList).stream()
				.map(this::getFavoritesFilename).collect(Collectors.toList());
		return filters;
	}

	private String getFavoritesFilename(HVSCEntry entry) {
		if (IOUtils.getFiles(entry.getPath(), configuration.getSidplay2Section().getHvsc(), null).size() > 0) {
			return C64_MUSIC + entry.getPath();
		} else if (IOUtils.getFiles(entry.getPath(), configuration.getSidplay2Section().getCgsc(), null).size() > 0) {
			return CGSC + entry.getPath();
		}
		return null;
	}

	private String getSpecialFavorites(final Integer favoritesNumber) throws JsonProcessingException, IOException {
		String resource = String.format("/favorites/jsidplay2-%d.json",
				(favoritesNumber - configuration.getFavorites().size()));
		try (InputStream source = new WebResourceConverter("<internal>").convert(resource)) {
			return IOUtils.convertStreamToString(source, "UTF-8");
		}
	}

}
