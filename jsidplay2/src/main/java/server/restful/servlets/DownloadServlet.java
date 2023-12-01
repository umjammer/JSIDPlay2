package server.restful.servlets;

import static libsidutils.IOUtils.getFilenameSuffix;
import static libsidutils.ZipFileUtils.newFileInputStream;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.ATTACHMENT;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.CONTENT_DISPOSITION;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.ROLE_USER;
import static server.restful.common.ContentTypeAndFileExtensions.getMimeType;
import static server.restful.common.ServletUtil.error;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import com.beust.jcommander.Parameters;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.restful.common.ContentTypeAndFileExtensions;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.parameter.requestpath.FileRequestPathServletParameters;

@SuppressWarnings("serial")
@WebServlet(name = "DownloadServlet", urlPatterns = CONTEXT_ROOT_SERVLET + "/download/*")
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { ROLE_USER, ROLE_ADMIN }))
public class DownloadServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.DownloadServletParameters")
	public static class DownloadServletParameters extends FileRequestPathServletParameters {

	}

	/**
	 * Download SID.
	 *
	 * E.g.
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/download/C64Music/MUSICIANS/D/DRAX/Worktunes/Outro.sid
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			WebServlet webServlet = getClass().getAnnotation(WebServlet.class);
			ServletSecurity servletSecurity = getClass().getAnnotation(ServletSecurity.class);

			final DownloadServletParameters servletParameters = new DownloadServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					webServlet);

			final File file = servletParameters.fetchFile(configuration, directoryProperties, parser, servletSecurity,
					request.isUserInRole(ROLE_ADMIN));
			if (file == null || parser.hasException()) {
				parser.usage();
				return;
			}
			response.addHeader(CONTENT_DISPOSITION,
					ATTACHMENT + "; filename=" + URLEncoder.encode(file.getName(), StandardCharsets.UTF_8.name()));
			ContentTypeAndFileExtensions mimeType = getMimeType(getFilenameSuffix(servletParameters.getFilePath()));
			setOutput(mimeType, response, newFileInputStream(file));

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(getServletContext(), t);
			setOutput(response, t);
		}
	}

}
