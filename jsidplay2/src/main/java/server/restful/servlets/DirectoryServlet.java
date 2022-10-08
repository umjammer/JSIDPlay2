package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.parameter.RequestPathServletParameters.DirectoryRequestPathServletParameters;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class DirectoryServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.DirectoryServletParameters")
	public static class DirectoryServletParameters extends DirectoryRequestPathServletParameters {

		private String filter = ".*\\.(sid|dat|mus|str|mp3|mp4|jpg|prg|d64)$";

		public String getFilter() {
			return filter;
		}

		@Parameter(names = { "--filter" }, descriptionKey = "FILTER")
		public void setFilter(String filter) {
			this.filter = filter;
		}
	}

	public static final String DIRECTORY_PATH = "/directory";

	public DirectoryServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + DIRECTORY_PATH;
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
		super.doGet(request);
		try {
			final DirectoryServletParameters servletParameters = new DirectoryServletParameters();

			JCommander commander = parseRequestParameters(request, response, servletParameters, getServletPath());

			List<String> files = getDirectory(commander, servletParameters, request.isUserInRole(ROLE_ADMIN));
			if (files == null) {
				commander.usage();
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
