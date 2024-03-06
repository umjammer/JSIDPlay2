package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.freeDebugEntityManager;
import static server.restful.JSIDPlay2Server.getDebugEntityManager;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ServletUtil.error;

import java.io.IOException;

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
import ui.entities.debug.service.DebugService;

@SuppressWarnings("serial")
@WebServlet(name = "CountLogsServlet", displayName = "CountLogsServlet", urlPatterns = CONTEXT_ROOT_SERVLET
		+ "/count-logs", description = "Get number of log message list entries")
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { ROLE_ADMIN }))
public class CountLogsServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.CountLogsServletParameters")
	public static class CountLogsServletParameters extends LogRequestParamServlet {

	}

	/**
	 * Get number of log message list entries.
	 *
	 * E.g. http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/count-logs
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			WebServlet webServlet = getClass().getAnnotation(WebServlet.class);

			final CountLogsServletParameters servletParameters = new CountLogsServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					webServlet);

			if (parser.hasException()) {
				parser.usage();
				return;
			}
			final DebugService debugService = new DebugService(getDebugEntityManager());
			Long result = debugService.countDebugEntries(servletParameters.getInstant(),
					servletParameters.getSourceClassName(), servletParameters.getSourceMethodName(),
					servletParameters.getLevel(), servletParameters.getMessage(), servletParameters.getOrder());

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
