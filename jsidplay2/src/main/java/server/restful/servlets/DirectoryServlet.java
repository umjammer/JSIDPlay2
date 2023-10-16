package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.Parameters;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.filters.RequestLogFilter;
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.parameter.requestpath.DirectoryRequestPathServletParameters;

@SuppressWarnings("serial")
@WebServlet(name = "DirectoryServlet", urlPatterns = CONTEXT_ROOT_SERVLET + "/directory/*")
public class DirectoryServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.DirectoryServletParameters")
	public static class DirectoryServletParameters extends DirectoryRequestPathServletParameters {

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

	@Override
	public boolean isSecured() {
		return true;
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
			final DirectoryServletParameters servletParameters = new DirectoryServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					getClass().getAnnotation(WebServlet.class));

			final List<String> files = servletParameters.fetchDirectory(this, parser, request.isUserInRole(ROLE_ADMIN));
			if (files == null || parser.hasException()) {
				parser.usage();
				return;
			}
			setOutput(response, MIME_TYPE_JSON, OBJECT_MAPPER.writer().writeValueAsString(files));

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

}
