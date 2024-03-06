package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.freeDebugEntityManager;
import static server.restful.JSIDPlay2Server.getDebugEntityManager;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ServletUtil.error;

import java.io.IOException;
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
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.parameter.requestparam.LogRequestParamServlet;
import server.restful.common.validator.MaxResultsValidator;
import ui.entities.debug.DebugEntry;
import ui.entities.debug.service.DebugService;

@SuppressWarnings("serial")
@WebServlet(name = "LogsServlet", displayName = "LogsServlet", urlPatterns = CONTEXT_ROOT_SERVLET
		+ "/logs", description = "Get log message list")
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { ROLE_ADMIN }))
public class LogsServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.LogsServletParameters")
	public static class LogsServletParameters extends LogRequestParamServlet {

		private Integer maxResults;

		public Integer getMaxResults() {
			return maxResults;
		}

		@Parameter(names = {
				"--maxResults" }, descriptionKey = "MAX_RESULTS", required = true, validateWith = MaxResultsValidator.class, order = 1)
		public void setMaxResults(Integer maxResults) {
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

			final LogsServletParameters servletParameters = new LogsServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					webServlet);

			if (parser.hasException()) {
				parser.usage();
				return;
			}
			final DebugService debugService = new DebugService(getDebugEntityManager());
			List<DebugEntry> result = debugService.findDebugEntries(servletParameters.getInstant(),
					servletParameters.getSourceClassName(), servletParameters.getSourceMethodName(),
					servletParameters.getLevel(), servletParameters.getMessage(), servletParameters.getMaxResults(),
					servletParameters.getOrder());

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
