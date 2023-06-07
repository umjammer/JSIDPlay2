package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.PathUtils;
import server.restful.common.JSIDPlay2Servlet;
import ui.entities.collection.HVSCEntry;
import ui.entities.config.Configuration;
import ui.entities.config.FavoritesSection;

@SuppressWarnings("serial")
public class FavoritesServlet extends JSIDPlay2Servlet {

	public static final String FAVORITES_PATH = "/favorites";

	public FavoritesServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + FAVORITES_PATH;
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
		super.doGet(request);
		try {
			List<String> filters = getFirstFavorites();

			setOutput(response, MIME_TYPE_JSON, OBJECT_MAPPER.writer().writeValueAsString(filters));

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

	private List<String> getFirstFavorites() {
		List<String> filters = configuration.getFavorites().stream()
				.filter(favorites -> !favorites.getFavorites().isEmpty()).findFirst()
				.map(FavoritesSection::getFavorites).orElseGet(Collections::emptyList).stream()
				.map(this::getFavoriteFilename).filter(Objects::nonNull).collect(Collectors.toList());
		return filters;
	}

	private String getFavoriteFilename(HVSCEntry entry) {
		if (PathUtils.getFiles(entry.getPath(), configuration.getSidplay2Section().getHvsc(), null).size() > 0) {
			return C64_MUSIC + entry.getPath();
		} else if (PathUtils.getFiles(entry.getPath(), configuration.getSidplay2Section().getCgsc(), null).size() > 0) {
			return CGSC + entry.getPath();
		}
		return null;
	}

}
