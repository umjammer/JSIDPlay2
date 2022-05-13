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
import server.restful.common.PlayerWithStatus;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class SetDefaultEmulationReSidServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.rtmp.SetDefaultEmulationReSidServletParameters")
	public static class ServletParameters {

		@Parameter(names = { "--name" }, descriptionKey = "NAME", order = -2)
		private String name;

	}

	public static final String SET_DEFAULT_EMULATION_RESID_PATH = "/set_default_emulation_resid";

	public SetDefaultEmulationReSidServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_STATIC + SET_DEFAULT_EMULATION_RESID_PATH;
	}

	/**
	 * Set default emulation to RESIDFP for Player running as a RTMP live video
	 * stream.
	 *
	 * {@code
	 * http://haendel.ddns.net:8080/static/set_default_emulation_resid?name=<uuid>
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

			info(String.format("setDefaultEmulationReSid: RTMP stream of: %s", uuid));
			update(uuid, PlayerWithStatus::setDefaultEmulationReSid);

		} catch (Throwable t) {
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

}
