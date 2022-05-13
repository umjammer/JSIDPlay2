package server.restful.servlets.rtmp;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_STATIC;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.PlayerCleanupTimerTask.update;
import static ui.entities.config.OnlineSection.APP_SERVER_URL;

import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidplay.components.keyboard.KeyTableEntry;
import server.restful.common.JSIDPlay2Servlet;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class PressKeyServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.rtmp.PressKeyServletParameters")
	public static class ServletParameters {

		@Parameter(names = { "--name" }, descriptionKey = "NAME", order = -2)
		private String name;

	}

	public static final String PRESS_KEY_PATH = "/press_key";

	public PressKeyServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_STATIC + PRESS_KEY_PATH;
	}

	/**
	 * Press key for Player running as a RTMP live video stream.
	 *
	 * {@code
	 * http://haendel.ddns.net:8080/static/press_key?name=<uuid>&type=KEY
	 * http://haendel.ddns.net:8080/static/press_key?name=<uuid>&press=KEY
	 * http://haendel.ddns.net:8080/static/press_key?name=<uuid>&release=KEY
	 * }
	 * 
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doPost(request);
		try {
			final ServletParameters servletParameters = new ServletParameters();

			JCommander commander = parseRequestParameters(request, response, servletParameters,
					APP_SERVER_URL + getServletPath() + "?name=uuid");
			if (servletParameters.name == null) {
				commander.usage();
				return;
			}
			UUID uuid = UUID.fromString(servletParameters.name);

			if (request.getParameter("type") != null) {
				KeyTableEntry key = KeyTableEntry.valueOf(KeyTableEntry.class, request.getParameter("type"));

				info(String.format("typeKey: RTMP stream of: %s, key=%s", uuid, key.name()));
				update(uuid, rtmpPlayerWithStatus -> rtmpPlayerWithStatus.typeKey(key));
			} else if (request.getParameter("press") != null) {
				KeyTableEntry key = KeyTableEntry.valueOf(KeyTableEntry.class, request.getParameter("press"));

				info(String.format("pressKey: RTMP stream of: %s, key=%s", uuid, key.name()));
				update(uuid, rtmpPlayerWithStatus -> rtmpPlayerWithStatus.pressKey(key));
			} else if (request.getParameter("release") != null) {
				KeyTableEntry key = KeyTableEntry.valueOf(KeyTableEntry.class, request.getParameter("release"));

				info(String.format("releaseKey: RTMP stream of: %s, key=%s", uuid, key.name()));
				update(uuid, rtmpPlayerWithStatus -> rtmpPlayerWithStatus.releaseKey(key));
			}
		} catch (Throwable t) {
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

}
