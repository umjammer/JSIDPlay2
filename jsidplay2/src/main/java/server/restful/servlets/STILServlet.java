package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.filters.RequestLogFilter.FILTER_PARAMETER_SERVLET_NAME;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.Parameters;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidplay.sidtune.SidTuneError;
import libsidutils.stil.STIL.STILEntry;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.filters.RequestLogFilter;
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.parameter.requestpath.FileRequestPathServletParameters;

@SuppressWarnings("serial")
public class STILServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.STILServletParameters")
	public static class STILServletParameters extends FileRequestPathServletParameters {

	}

	public static final String STIL_PATH = "/stil";

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + STIL_PATH;
	}

	@Override
	public List<Filter> getServletFilters() {
		return Arrays.asList(new RequestLogFilter());
	}

	@Override
	public Map<String, String> getServletFiltersParameterMap() {
		Map<String, String> result = new HashMap<>();
		result.put(FILTER_PARAMETER_SERVLET_NAME, getClass().getSimpleName());
		return result;
	}

	@Override
	public boolean isSecured() {
		return true;
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
					getServletPath());

			final File file = servletParameters.fetchFile(this, parser, request.isUserInRole(ROLE_ADMIN));
			if (file == null || parser.hasException()) {
				parser.usage();
				return;
			}
			STILEntry stilEntry = createSTIL(file);

			setOutput(response, MIME_TYPE_JSON, OBJECT_MAPPER.writer().writeValueAsString(stilEntry));

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

	private STILEntry createSTIL(File file) throws IOException, SidTuneError {
		return stil != null ? stil.getSTILEntry(getCollectionName(file)) : null;
	}

}
