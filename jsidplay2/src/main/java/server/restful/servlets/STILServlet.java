package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.ROLE_USER;
import static server.restful.common.ServletUtil.error;

import java.io.File;
import java.io.IOException;

import com.beust.jcommander.Parameters;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidplay.sidtune.SidTuneError;
import libsidutils.stil.STIL.STILEntry;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.parameter.requestpath.FileRequestPathServletParameters;

@SuppressWarnings("serial")
@WebServlet(name = "STILServlet", urlPatterns = CONTEXT_ROOT_SERVLET + "/stil/*")
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { ROLE_USER, ROLE_ADMIN }))
public class STILServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.STILServletParameters")
	public static class STILServletParameters extends FileRequestPathServletParameters {

	}

	/**
	 * Get SID tune information list (STIL).
	 *
	 * E.g.
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/stil/C64Music/MUSICIANS/D/DRAX/Acid.sid
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			final STILServletParameters servletParameters = new STILServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					getClass().getAnnotation(WebServlet.class));

			final File file = servletParameters.fetchFile(this, parser, request.isUserInRole(ROLE_ADMIN));
			if (file == null || parser.hasException()) {
				parser.usage();
				return;
			}
			STILEntry stilEntry = createSTIL(file);

			setOutput(response, stilEntry);

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(getServletContext(), t);
			setOutput(response, t);
		}
	}

	private STILEntry createSTIL(File file) throws IOException, SidTuneError {
		return stil != null ? stil.getSTILEntry(getCollectionName(file)) : null;
	}

}
