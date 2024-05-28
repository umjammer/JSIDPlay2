package server.restful.servlets.rtmp;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_STATIC;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.ROLE_USER;
import static server.restful.common.ServletUtil.error;
import static server.restful.common.ServletUtil.info;
import static server.restful.common.rtmp.PlayerCleanupTimerTask.update;

import java.io.IOException;
import java.util.UUID;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.parameter.requestparam.VideoRequestParamServletParameters;
import server.restful.common.validator.JoystickNumberValidator;
import server.restful.common.validator.JoystickValueValidator;

@SuppressWarnings("serial")
@WebServlet(name = "JoystickServlet", displayName = "JoystickServlet", urlPatterns = CONTEXT_ROOT_STATIC
		+ "/joystick", description = "Press joystick 1/2 for Player running as a RTMP live video stream")
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { ROLE_USER, ROLE_ADMIN }))
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
		try {
			WebServlet webServlet = getClass().getAnnotation(WebServlet.class);

			final JoystickServletParameters servletParameters = new JoystickServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					webServlet);

			if (servletParameters.getHelp() || parser.hasException()) {
				parser.usage();
				return;
			}
			UUID uuid = servletParameters.getUuid();
			int number = servletParameters.getNumber();
			int value = servletParameters.getValue();

			info(getServletContext(),
					String.format("joystick: RTMP stream of: %s, number=%d, value=%d", uuid, number, value));
			update(uuid, rtmpPlayerWithStatus -> rtmpPlayerWithStatus.joystick(number, value));

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(getServletContext(), t);
			setOutput(response, t);
		}
	}

}
