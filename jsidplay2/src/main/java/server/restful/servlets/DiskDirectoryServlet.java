package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.ROLE_USER;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ServletUtil.error;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.beust.jcommander.Parameters;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.IOUtils;
import libsidutils.directory.Directory;
import libsidutils.directory.DiskDirectory;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.parameter.requestpath.FileRequestPathServletParameters;

@SuppressWarnings("serial")
@WebServlet(name = "DiskDirectoryServlet", urlPatterns = CONTEXT_ROOT_SERVLET + "/disk-directory/*")
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { ROLE_USER, ROLE_ADMIN }))
public class DiskDirectoryServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.DiskDirectoryServletParameters")
	public static class DiskDirectoryServletParameters extends FileRequestPathServletParameters {

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
		try {
			WebServlet webServlet = getClass().getAnnotation(WebServlet.class);
			ServletSecurity servletSecurity = getClass().getAnnotation(ServletSecurity.class);

			final DiskDirectoryServletParameters servletParameters = new DiskDirectoryServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					webServlet);

			final File file = servletParameters.fetchFile(configuration, directoryProperties, parser, servletSecurity,
					request.isUserInRole(ROLE_ADMIN));
			if (file == null || parser.hasException()) {
				parser.usage();
				return;
			}
			Directory directory = createDiskDirectory(file);

			setOutput(MIME_TYPE_JSON, response, directory);

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(getServletContext(), t);
			setOutput(response, t);
		}
	}

	private Directory createDiskDirectory(final File file) throws IOException, FileNotFoundException {
		File extractedFile = IOUtils.extract(configuration.getSidplay2Section().getTmpDir(), file);
		return new DiskDirectory(extractedFile);
	}

}