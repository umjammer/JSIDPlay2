package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.ROLE_USER;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JPG;
import static server.restful.common.ServletUtil.error;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;

import com.beust.jcommander.Parameters;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jsidplay2.Photos;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidplay.sidtune.SidTuneInfo;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.parameter.requestpath.FileRequestPathServletParameters;

@SuppressWarnings("serial")
@WebServlet(name = "PhotoServlet", displayName = "PhotoServlet", urlPatterns = CONTEXT_ROOT_SERVLET
		+ "/photo/*", description = "Get photo of composer")
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { ROLE_USER, ROLE_ADMIN }))
public class PhotoServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.PhotoServletParameters")
	public static class PhotoServletParameters extends FileRequestPathServletParameters {

	}

	/**
	 * Get photo of composer.
	 *
	 * E.g.
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/photo/C64Music/MUSICIANS/D/DRAX/Acid.sid
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			WebServlet webServlet = getClass().getAnnotation(WebServlet.class);
			ServletSecurity servletSecurity = getClass().getAnnotation(ServletSecurity.class);

			final PhotoServletParameters servletParameters = new PhotoServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					webServlet);

			final File file = servletParameters.fetchFile(configuration, directoryProperties, parser, servletSecurity,
					request.isUserInRole(ROLE_ADMIN));
			if (file == null || servletParameters.getHelp() || parser.hasException()) {
				parser.usage();
				return;
			}
			byte[] photo = getPhoto(file);

			response.setContentLength(photo.length);
			setOutput(MIME_TYPE_JPG, response, new ByteArrayInputStream(photo));

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(getServletContext(), t);
			setOutput(response, t);
		}
	}

	private byte[] getPhoto(File file) throws IOException, SidTuneError {
		String authorDirectory = getCollectionName(file);

		if (!authorDirectory.isEmpty()) {
			authorDirectory = new File(authorDirectory).getParent();
		}

		String author = null;
		if (file != null) {
			SidTuneInfo info = SidTune.load(file).getInfo();
			if (info.getInfoString().size() > 1) {
				Iterator<String> iterator = info.getInfoString().iterator();
				/* title = */iterator.next();
				author = iterator.next();
			}
		}
		return Optional.ofNullable(Photos.getPhoto(authorDirectory, author)).orElse(new byte[0]);
	}

}