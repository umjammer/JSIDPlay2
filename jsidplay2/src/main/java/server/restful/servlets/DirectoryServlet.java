package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.ROLE_USER;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ServletUtil.error;

import java.io.IOException;
import java.util.List;

import com.beust.jcommander.Parameters;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.parameter.requestpath.DirectoryRequestPathServletParameters;

@SuppressWarnings("serial")
@WebServlet(name = "DirectoryServlet", displayName = "DirectoryServlet", urlPatterns = CONTEXT_ROOT_SERVLET
		+ "/directory/*", description = "Get directory contents containing music collections")
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { ROLE_USER, ROLE_ADMIN }))
public class DirectoryServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.DirectoryServletParameters")
	public static class DirectoryServletParameters extends DirectoryRequestPathServletParameters {

	}

	/**
	 * Get directory contents containing music collections.
	 *
	 * E.g.
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/directory/C64Music/MUSICIANS?filter=.*%5C.(sid%7Cdat%7Cmus%7Cstr%7Cmp3%7Cmp4%7Cjpg%7Cprg%7Cd64)$
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			WebServlet webServlet = getClass().getAnnotation(WebServlet.class);
			ServletSecurity servletSecurity = getClass().getAnnotation(ServletSecurity.class);

			final DirectoryServletParameters servletParameters = new DirectoryServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					webServlet);

			final List<String> files = servletParameters.fetchDirectory(configuration, directoryProperties, parser,
					servletSecurity, request.isUserInRole(ROLE_ADMIN));
			if (files == null || servletParameters.getHelp() || parser.hasException()) {
				parser.usage();
				return;
			}
			setOutput(MIME_TYPE_JSON, response, files);

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(getServletContext(), t);
			setOutput(response, t);
		}
	}

}
