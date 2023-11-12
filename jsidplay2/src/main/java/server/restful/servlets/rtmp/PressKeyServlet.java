package server.restful.servlets.rtmp;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_STATIC;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.ROLE_USER;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.rtmp.PlayerCleanupTimerTask.update;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidplay.components.keyboard.KeyTableEntry;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.filters.RequestLogFilter;
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.parameter.requestparam.VideoRequestParamServletParameters;

@SuppressWarnings("serial")
@WebServlet(name = "PressKeyServlet", urlPatterns = CONTEXT_ROOT_STATIC + "/press_key")
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { ROLE_USER, ROLE_ADMIN }))
public class PressKeyServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.rtmp.PressKeyServletParameters")
	public static class PressKeyServletParameters extends VideoRequestParamServletParameters {

		@Parameter(names = { "--type" }, descriptionKey = "TYPE", order = 1)
		private KeyTableEntry type;

		@Parameter(names = { "--press" }, descriptionKey = "PRESS", order = 2)
		private KeyTableEntry press;

		@Parameter(names = { "--release" }, descriptionKey = "RELEASE", order = 3)
		private KeyTableEntry release;

	}

	@Override
	public List<Filter> getServletFilters() {
		return Arrays.asList(new RequestLogFilter());
	}

	@Override
	public Map<String, String> getServletFiltersParameterMap() {
		Map<String, String> result = new HashMap<>();
		return result;
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
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			final PressKeyServletParameters servletParameters = new PressKeyServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					getClass().getAnnotation(WebServlet.class));

			if ((servletParameters.type == null && servletParameters.press == null && servletParameters.release == null)
					|| parser.hasException()) {
				parser.usage();
				return;
			}
			UUID uuid = servletParameters.getUuid();

			if (servletParameters.type != null) {
				info(String.format("typeKey: RTMP stream of: %s, key=%s", uuid, servletParameters.type.name()));
				update(uuid, rtmpPlayerWithStatus -> rtmpPlayerWithStatus.typeKey(servletParameters.type));

			} else if (servletParameters.press != null) {
				info(String.format("pressKey: RTMP stream of: %s, key=%s", uuid, servletParameters.press.name()));
				update(uuid, rtmpPlayerWithStatus -> rtmpPlayerWithStatus.pressKey(servletParameters.press));

			} else if (servletParameters.release != null) {
				info(String.format("releaseKey: RTMP stream of: %s, key=%s", uuid, servletParameters.release.name()));
				update(uuid, rtmpPlayerWithStatus -> rtmpPlayerWithStatus.releaseKey(servletParameters.release));
			}
		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

}
