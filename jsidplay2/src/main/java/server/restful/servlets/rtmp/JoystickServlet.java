package server.restful.servlets.rtmp;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_STATIC;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.PlayerCleanupTimerTask.update;

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
import server.restful.common.parameter.ServletUsageFormatter;
import server.restful.common.parameter.requestparam.VideoRequestParamServletParameters;
import server.restful.common.validator.JoystickNumberValidator;
import server.restful.common.validator.JoystickValueValidator;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class JoystickServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.rtmp.JoystickServletParameters")
	public static class JoystickServletParameters extends VideoRequestParamServletParameters {

		private int number;

		public int getNumber() {
			return number;
		}

		@Parameter(names = {
				"--number" }, descriptionKey = "NUMBER", validateWith = JoystickNumberValidator.class, order = 1)
		public void setNumber(int number) {
			this.number = number;
		}

		private int value;

		public int getValue() {
			return value;
		}

		@Parameter(names = {
				"--value" }, descriptionKey = "VALUE", validateWith = JoystickValueValidator.class, order = 2)
		public void setValue(int value) {
			this.value = value;
		}
	}

	public static final String JOYSTICK_PATH = "/joystick";

	public JoystickServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_STATIC + JOYSTICK_PATH;
	}

	@Override
	public boolean isSecured() {
		return true;
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
			final JoystickServletParameters servletParameters = new JoystickServletParameters();

			JCommander commander = parseRequestParameters(request, response, servletParameters, getServletPath());
			if (((ServletUsageFormatter) commander.getUsageFormatter()).getException() != null) {
				commander.usage();
				return;
			}
			UUID uuid = servletParameters.getUuid();
			int number = servletParameters.getNumber();
			int value = servletParameters.getValue();

			info(String.format("joystick: RTMP stream of: %s, number=%d, value=%d", uuid, number, value));
			update(uuid, rtmpPlayerWithStatus -> rtmpPlayerWithStatus.joystick(number, value));

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

}
