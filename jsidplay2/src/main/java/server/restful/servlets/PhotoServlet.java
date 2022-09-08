package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JPG;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.Optional;
import java.util.Properties;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jsidplay2.Photos;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidplay.sidtune.SidTuneInfo;
import libsidutils.PathUtils;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.ServletBaseParameters;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class PhotoServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.PhotoServletParameters")
	public static class ServletParameters extends ServletBaseParameters {

	}

	public static final String PHOTO_PATH = "/photo";

	public PhotoServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + PHOTO_PATH;
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
		super.doGet(request);
		try {
			final ServletParameters servletParameters = new ServletParameters();

			JCommander commander = parseRequestParameters(request, response, servletParameters, getServletPath());
			if (servletParameters.getFilePath() == null) {
				commander.usage();
				return;
			}

			final File file = getAbsoluteFile(servletParameters, request.isUserInRole(ROLE_ADMIN));

			byte[] photo = getPhoto(configuration.getSidplay2Section().getHvsc(), file);

			response.setContentLength(photo.length);
			response.setContentType(MIME_TYPE_JPG.toString());
			response.getOutputStream().write(photo);

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

	private byte[] getPhoto(File hvscRoot, File tuneFile) throws IOException, SidTuneError {
		String collectionName = null;
		if (hvscRoot != null && tuneFile != null && tuneFile.getParentFile() != null) {
			collectionName = PathUtils.getCollectionName(hvscRoot, tuneFile.getParentFile());
		}
		String author = null;
		if (tuneFile != null) {
			SidTuneInfo info = SidTune.load(extract(tuneFile)).getInfo();
			if (info.getInfoString().size() > 1) {
				Iterator<String> iterator = info.getInfoString().iterator();
				/* title = */iterator.next();
				author = iterator.next();
			}
		}
		return Optional.ofNullable(Photos.getPhoto(collectionName, author)).orElse(new byte[0]);
	}

}