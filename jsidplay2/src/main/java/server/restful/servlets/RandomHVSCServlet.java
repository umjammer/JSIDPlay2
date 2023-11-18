package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.ROLE_USER;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.ServletUtil.error;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.restful.common.JSIDPlay2Servlet;

@SuppressWarnings("serial")
@WebServlet(name = "RandomHVSCServlet", urlPatterns = CONTEXT_ROOT_SERVLET + "/random-hvsc")
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { ROLE_USER, ROLE_ADMIN }))
public class RandomHVSCServlet extends JSIDPlay2Servlet {

	/**
	 * Get random HVSC SID tune.
	 *
	 * E.g. http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/random-hvsc
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			String randomHVSC = getRandomHVSC();

			setOutput(response, MIME_TYPE_JSON, OBJECT_MAPPER.writer().writeValueAsString(randomHVSC));

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(getServletContext(), t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

	private String getRandomHVSC() {
		if (sidDatabase != null) {
			String rndPath = sidDatabase.getRandomPath();
			if (rndPath != null) {
				return JSIDPlay2Servlet.C64_MUSIC + rndPath;
			}
		}
		return null;
	}

}
