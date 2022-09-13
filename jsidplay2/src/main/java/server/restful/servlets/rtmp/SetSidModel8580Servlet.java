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
import server.restful.common.PlayerWithStatus;
import server.restful.common.converter.UUIDConverter;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class SetSidModel8580Servlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.rtmp.SetSidModel8580ServletParameters")
	public static class ServletParameters {

		@Parameter(names = { "--name" }, descriptionKey = "NAME", converter = UUIDConverter.class, required = true)
		private UUID uuid;

	}

	public static final String SET_DEFAULT_SID_MODEL_8580_PATH = "/set_default_sid_model_8580";

	public SetSidModel8580Servlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_STATIC + SET_DEFAULT_SID_MODEL_8580_PATH;
	}

	/**
	 * Set default chip model to MOS8580 for Player running as a RTMP live video
	 * stream.
	 *
	 * {@code
	 * http://haendel.ddns.net:8080/static/set_default_sid_model_8580?name=<uuid>
	 * }
	 * 
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doGet(request);
		try {
			final ServletParameters servletParameters = new ServletParameters();

			JCommander commander = parseRequestParameters(request, response, servletParameters, getServletPath());
			if (servletParameters.uuid == null) {
				commander.usage();
				return;
			}
			UUID uuid = servletParameters.uuid;

			info(String.format("setDefaultSidModel8580: RTMP stream of: %s", uuid));
			update(uuid, PlayerWithStatus::setDefaultSidModel8580);

		} catch (Throwable t) {
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

}
