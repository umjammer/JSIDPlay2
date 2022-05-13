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
import server.restful.common.JSIDPlay2Servlet;
import sidplay.ini.converter.UUIDConverter;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class JoystickServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.rtmp.JoystickServletParameters")
	public static class ServletParameters {

		@Parameter(names = { "--name" }, descriptionKey = "NAME", converter = UUIDConverter.class, order = -4)
		private UUID uuid;

		@Parameter(names = { "--number" }, descriptionKey = "NUMBER", order = -3)
		private int number;

		@Parameter(names = { "--value" }, descriptionKey = "VALUE", order = -2)
		private int value;
	}

	public static final String JOYSTICK_PATH = "/joystick";

	public JoystickServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_STATIC + JOYSTICK_PATH;
	}

	/**
	 * Press joystick 1/2 for Player running as a RTMP live video stream.
	 *
	 * {@code
	 * http://haendel.ddns.net:8080/static/joystick?name=<uuid>&number=0&value=<value>
	 * http://haendel.ddns.net:8080/static/joystick?name=<uuid>&number=1&value=<value>
	 * }
	 * 
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doGet(request);
		try {
			final ServletParameters servletParameters = new ServletParameters();

			JCommander commander = parseRequestParameters(request, response, servletParameters,
					APP_SERVER_URL + getServletPath() + "?name=uuid");
			if (servletParameters.uuid == null) {
				commander.usage();
				return;
			}
			UUID uuid = servletParameters.uuid;
			int number = servletParameters.number;
			int value = servletParameters.value;

			info(String.format("joystick: RTMP stream of: %s, number=%d, value=%d", uuid, number, value));
			update(uuid, rtmpPlayerWithStatus -> rtmpPlayerWithStatus.joystick(number, value));

		} catch (Throwable t) {
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

}
