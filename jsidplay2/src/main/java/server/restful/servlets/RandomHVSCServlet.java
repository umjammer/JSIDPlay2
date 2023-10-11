package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;

import java.io.IOException;
import java.util.Properties;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.siddatabase.SidDatabase;
import server.restful.common.JSIDPlay2Servlet;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class RandomHVSCServlet extends JSIDPlay2Servlet {

	public static final String RANDOM_HVSC_PATH = "/random-hvsc";

	public RandomHVSCServlet(Configuration configuration, SidDatabase sidDatabase, Properties directoryProperties) {
		super(configuration, sidDatabase, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + RANDOM_HVSC_PATH;
	}

	@Override
	public boolean isSecured() {
		return true;
	}

	/**
	 * Get random HVSC SID tune.
	 *
	 * E.g. http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/random-hvsc
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doGet(request);
		try {
			String randomHVSC = getRandomHVSC();

			setOutput(response, MIME_TYPE_JSON, OBJECT_MAPPER.writer().writeValueAsString(randomHVSC));

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

	private String getRandomHVSC() throws IOException {
		if (sidDatabase != null) {
			String rndPath = sidDatabase.getRandomPath();
			if (rndPath != null) {
				return JSIDPlay2Servlet.C64_MUSIC + rndPath;
			}
		}
		return null;
	}

}
