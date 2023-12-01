package server.restful.servlets.rtmp;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_STATIC;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.ROLE_USER;
import static server.restful.common.ServletUtil.error;
import static server.restful.common.ServletUtil.info;
import static server.restful.common.rtmp.PlayerCleanupTimerTask.update;

import java.io.IOException;
import java.util.UUID;

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
import server.restful.common.rtmp.PlayerWithStatus;

@SuppressWarnings("serial")
@WebServlet(name = "SetDefaultEmulationReSidFpServlet", displayName = "SetDefaultEmulationReSidFpServlet", urlPatterns = CONTEXT_ROOT_STATIC
		+ "/set_default_emulation_residfp", description = "Set default emulation to RESIDFP for Player running as a RTMP live video stream")
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { ROLE_USER, ROLE_ADMIN }))
public class SetDefaultEmulationReSidFpServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.rtmp.SetDefaultEmulationReSidFpServletParameters")
	public static class SetDefaultEmulationReSidFpServletParameters extends VideoRequestParamServletParameters {

	}

	/**
	 * Set default emulation to RESIDFP for Player running as a RTMP live video
	 * stream.
	 *
	 * {@code
	 * http://haendel.ddns.net:8080/static/set_default_emulation_residfp?name=<uuid>
	 * }
	 * 
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			WebServlet webServlet = getClass().getAnnotation(WebServlet.class);

			final SetDefaultEmulationReSidFpServletParameters servletParameters = new SetDefaultEmulationReSidFpServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					webServlet);

			if (parser.hasException()) {
				parser.usage();
				return;
			}
			UUID uuid = servletParameters.getUuid();

			info(getServletContext(), String.format("setDefaultEmulationReSidFp: RTMP stream of: %s", uuid));
			update(uuid, PlayerWithStatus::setDefaultEmulationReSidFp);

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(getServletContext(), t);
			setOutput(response, t);
		}
	}

}
