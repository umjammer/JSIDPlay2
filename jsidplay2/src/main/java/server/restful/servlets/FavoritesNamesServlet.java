package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.ROLE_USER;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ServletUtil.error;
import static server.restful.servlets.FavoritesServlet.BUILT_IN_FAVORITES_PATH;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

import com.beust.jcommander.ParameterException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.converter.WebResourceConverter;
import ui.entities.config.FavoritesSection;

@SuppressWarnings("serial")
@WebServlet(name = "FavoritesNamesServlet", urlPatterns = CONTEXT_ROOT_SERVLET + "/favorite_names")
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { ROLE_USER, ROLE_ADMIN }))
public class FavoritesNamesServlet extends JSIDPlay2Servlet {

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

			setOutput(MIME_TYPE_JSON, response, favoritesNames);

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(getServletContext(), t);
			setOutput(response, t);
		}
	}

	private List<String> getFavoritesNames() {
		List<String> filters = configuration.getFavorites().stream().map(FavoritesSection::getName)
				.collect(Collectors.toList());
		int i = 0;
		do {
			String resource = String.format(BUILT_IN_FAVORITES_PATH, i);
			try (InputStream source = new WebResourceConverter("<ServletPath>").convert(resource)) {
				filters.add(String.format("Playlist-%d", ++i));
			} catch (IOException | ParameterException e) {
				break;
			}
		} while (true);
		return filters;
	}

}
