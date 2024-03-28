package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.ROLE_USER;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ServletUtil.error;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.IOUtils;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.converter.WebResourceConverter;
import server.restful.common.parameter.ServletParameterParser;
import ui.entities.collection.HVSCEntry;
import ui.entities.config.FavoritesSection;

@SuppressWarnings("serial")
@WebServlet(name = "FavoritesServlet", displayName = "FavoritesServlet", urlPatterns = CONTEXT_ROOT_SERVLET
		+ "/favorites", description = "Get contents of the first SID favorites tab")
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { ROLE_USER, ROLE_ADMIN }))
public class FavoritesServlet extends JSIDPlay2Servlet {

	public static final String BUILT_IN_FAVORITES_PATH = "/favorites/jsidplay2-%d.json";

	@Parameters(resourceBundle = "server.restful.servlets.FavoritesServletParameters")
	public static class FavoritesServletParameters {

		private Integer favoritesNumber = null;

		public Integer getFavoritesNumber() {
			return favoritesNumber;
		}

		@Parameter(names = { "--favoritesNumber" }, descriptionKey = "FAVORITES_NUMBER", order = -2)
		public void setFavoritesNumber(Integer favoritesNumber) {
			this.favoritesNumber = favoritesNumber;
		}

	}

	private static class FavoritesWrapper {
		private FavoritesWrapper(String filename) {
			this.filename = filename;
		}

		@JsonProperty
		private String filename;
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
			WebServlet webServlet = getClass().getAnnotation(WebServlet.class);

			final FavoritesServletParameters servletParameters = new FavoritesServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					webServlet);

			if (parser.hasException()) {
				parser.usage();
				return;
			}
			final Integer favoritesNumber = servletParameters.getFavoritesNumber();

			if (favoritesNumber == null) {
				setOutput(MIME_TYPE_JSON, response, // app, only
						getFavoritesByNumber(0).stream().map(fav -> fav.filename).collect(Collectors.toList()));
			} else if (favoritesNumber < configuration.getFavorites().size()) {
				List<FavoritesWrapper> favorites = getFavoritesByNumber(favoritesNumber);

				setOutput(MIME_TYPE_JSON, response, favorites);
			} else {
				String resource = String.format(BUILT_IN_FAVORITES_PATH,
						(favoritesNumber - configuration.getFavorites().size()));

				try (InputStream is = new WebResourceConverter("<ServletPath>").convert(resource)) {
					setOutput(MIME_TYPE_JSON, response, is);
				}
			}

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(getServletContext(), t);
			setOutput(response, t);
		}
	}

	private List<FavoritesWrapper> getFavoritesByNumber(Integer favoritesNumber) {
		return configuration.getFavorites().stream()
				.filter(favorites -> configuration.getFavorites().indexOf(favorites) == favoritesNumber).findFirst()
				.map(FavoritesSection::getFavorites).orElseGet(Collections::emptyList).stream().map(this::getFavorite)
				.collect(Collectors.toList());
	}

	private FavoritesWrapper getFavorite(HVSCEntry entry) {
		if (IOUtils.getFiles(entry.getPath(), configuration.getSidplay2Section().getHvsc(), null).size() > 0) {
			return new FavoritesWrapper(C64_MUSIC + entry.getPath());
		} else if (IOUtils.getFiles(entry.getPath(), configuration.getSidplay2Section().getCgsc(), null).size() > 0) {
			return new FavoritesWrapper(CGSC + entry.getPath());
		}
		return null;
	}

}
