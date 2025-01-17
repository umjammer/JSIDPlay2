package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.ROLE_USER;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ServletUtil.error;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.restful.common.JSIDPlay2Servlet;
import ui.entities.config.FilterSection;

@SuppressWarnings("serial")
@WebServlet(name = "FiltersServlet", displayName = "FiltersServlet", urlPatterns = CONTEXT_ROOT_SERVLET
		+ "/filters", description = "Get SID filter definitions")
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { ROLE_USER, ROLE_ADMIN }))
public class FiltersServlet extends JSIDPlay2Servlet {

	/**
	 * Get SID filter definitions.
	 *
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/filters
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			List<String> filters = getFilters();

			setOutput(MIME_TYPE_JSON, response, filters);

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(getServletContext(), t);
			setOutput(response, t);
		}
	}

	private List<String> getFilters() {
		List<String> result = new ArrayList<>();
		for (FilterSection iFilterSection : configuration.getFilterSection()) {
			if (iFilterSection.isReSIDFilter6581()) {
				result.add("RESID_MOS6581_" + iFilterSection.getName());
			} else if (iFilterSection.isReSIDFilter8580()) {
				result.add("RESID_MOS8580_" + iFilterSection.getName());
			} else if (iFilterSection.isReSIDfpFilter6581()) {
				result.add("RESIDFP_MOS6581_" + iFilterSection.getName());
			} else if (iFilterSection.isReSIDfpFilter8580()) {
				result.add("RESIDFP_MOS8580_" + iFilterSection.getName());
			}
		}
		return result;
	}

}
