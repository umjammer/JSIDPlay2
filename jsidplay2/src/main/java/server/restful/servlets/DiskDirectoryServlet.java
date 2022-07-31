package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.C64Font;
import libsidutils.directory.Directory;
import libsidutils.directory.DiskDirectory;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.ServletBaseParameters;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class DiskDirectoryServlet extends JSIDPlay2Servlet implements C64Font {

	@Parameters(resourceBundle = "server.restful.servlets.DiskDirectoryServletParameters")
	public static class ServletParameters extends ServletBaseParameters {

	}

	public static final String DISK_DIRECTORY_PATH = "/disk-directory";

	public DiskDirectoryServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + DISK_DIRECTORY_PATH;
	}

	/**
	 * Get Directory of Disk.
	 *
	 * E.g.
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/disk-directory/C64Music/10_Years_HVSC_1.d64
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doGet(request);
		try {
			final ServletParameters servletParameters = new ServletParameters();

			JCommander commander = parseRequestParameters(request, response, servletParameters, getServletPath());
			if (servletParameters.getFilePath() == null) {
				commander.usage();
				return;
			}

			final File file = getAbsoluteFile(servletParameters, request.isUserInRole(ROLE_ADMIN));

			Directory directory = new DiskDirectory(extract(file));

			setOutput(response, MIME_TYPE_JSON, new ObjectMapper().writer().writeValueAsString(directory));

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

}