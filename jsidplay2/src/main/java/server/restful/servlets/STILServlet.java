package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.beust.jcommander.Parameters;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidplay.sidtune.SidTuneError;
import libsidutils.stil.STIL;
import libsidutils.stil.STIL.STILEntry;
import net.java.truevfs.access.TFile;
import net.java.truevfs.access.TFileInputStream;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.parameter.requestpath.FileRequestPathServletParameters;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class STILServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.STILServletParameters")
	public static class STILServletParameters extends FileRequestPathServletParameters {

	}

	public static final String STIL_PATH = "/stil";

	public STILServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + STIL_PATH;
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
		super.doGet(request);
		try {
			final STILServletParameters servletParameters = new STILServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					getServletPath());

			final File file = servletParameters.getFile(this, parser, request.isUserInRole(ROLE_ADMIN));
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

	private STILEntry createSTIL(File file)
			throws IOException, SidTuneError, NoSuchFieldException, IllegalAccessException {
		File hvscRoot = configuration.getSidplay2Section().getHvsc();

		STIL stil = null;
		if (hvscRoot != null) {
			try (InputStream input = new TFileInputStream(new TFile(hvscRoot, STIL.STIL_FILE))) {
				stil = new STIL(input);
			}
		}
		return stil != null ? stil.getSTILEntry(getCollectionName(file)) : null;
	}

}
