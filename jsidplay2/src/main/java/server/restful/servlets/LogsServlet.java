package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.ROLE_USER;
import static server.restful.JSIDPlay2Server.freeDebugEntityManager;
import static server.restful.JSIDPlay2Server.getDebugEntityManager;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ServletUtil.error;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.ServletUtil;
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.validator.MaxResultsValidator;
import ui.entities.debug.DebugEntry;
import ui.entities.debug.service.DebugService;

@SuppressWarnings("serial")
@WebServlet(name = "LogsServlet", displayName = "LogsServlet", urlPatterns = CONTEXT_ROOT_SERVLET
		+ "/logs/*", description = "Get log message list")
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { ROLE_USER, ROLE_ADMIN }))
public class LogsServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.LogsServletParameters")
	public static class LogsServletParameters {

		private Long instant = 0L;

		public Long getInstant() {
			return instant;
		}

		@Parameter(names = { "--instant" }, descriptionKey = "INSTANT", order = 1)
		public void setInstant(Long instant) {
			this.instant = instant;
		}

		private String sourceClassName = "";

		public String getSourceClassName() {
			return sourceClassName;
		}

		@Parameter(names = { "--sourceClassName" }, descriptionKey = "SOURCE_CLASS_NAME", order = 2)
		public void setSourceClassName(String sourceClassName) {
			this.sourceClassName = sourceClassName;
		}

		private String sourceMethodName = "";

		public String getSourceMethodName() {
			return sourceMethodName;
		}

		@Parameter(names = { "--sourceMethodName" }, descriptionKey = "SOURCE_METHOD_NAME", order = 3)
		public void setSourceMethodName(String sourceMethodName) {
			this.sourceMethodName = sourceMethodName;
		}

		private String level = "";

		public String getLevel() {
			return level;
		}

		@Parameter(names = { "--level" }, descriptionKey = "LEVEL", order = 4)
		public void setLevel(String level) {
			this.level = level;
		}

		private String message = "";

		public String getMessage() {
			return message;
		}

		@Parameter(names = { "--message" }, descriptionKey = "MESSAGE", order = 5)
		public void setMessage(String message) {
			this.message = message;
		}

		private Integer maxResults = 100;

		public Integer getMaxResults() {
			return maxResults;
		}

		@Parameter(names = {
				"--maxResults" }, descriptionKey = "MAX_RESULTS", validateWith = MaxResultsValidator.class, order = 2)
		public void setStartSong(Integer maxResults) {
			this.maxResults = maxResults;
		}

	}

	/**
	 * Get log message list.
	 *
	 * E.g. http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/logs
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			WebServlet webServlet = getClass().getAnnotation(WebServlet.class);
			ServletSecurity servletSecurity = getClass().getAnnotation(ServletSecurity.class);

			final LogsServletParameters servletParameters = new LogsServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					webServlet);

			if (parser.hasException()) {
				parser.usage();
				return;
			}
			List<DebugEntry> result = new ArrayList<>();

			boolean adminRole = !ServletUtil.isSecured(servletSecurity) || request.isUserInRole(ROLE_ADMIN);
			if (adminRole) {
				final DebugService debugService = new DebugService(getDebugEntityManager());
				result.addAll(debugService.findDebugEntries(servletParameters.getInstant(),
						servletParameters.getSourceClassName(), servletParameters.getSourceMethodName(),
						servletParameters.getLevel(), servletParameters.getMessage(),
						servletParameters.getMaxResults()));
			}
			setOutput(MIME_TYPE_JSON, response, result);

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(getServletContext(), t);
			setOutput(response, t);
		} finally {
			freeDebugEntityManager();
		}
	}

}
