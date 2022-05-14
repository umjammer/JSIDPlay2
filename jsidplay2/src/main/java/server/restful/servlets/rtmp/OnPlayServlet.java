package server.restful.servlets.rtmp;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_STATIC;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.IServletSystemProperties.BASE_URL;
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
public class OnPlayServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.rtmp.OnPlayServletParameters")
	public static class ServletParameters {

		@Parameter(names = { "--name" }, descriptionKey = "NAME", converter = UUIDConverter.class, order = -2)
		private UUID uuid;

	}

	public static final String ON_PLAY_PATH = "/on_play";

	public OnPlayServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_STATIC + ON_PLAY_PATH;
	}

	/**
	 * Play video stream.
	 * 
	 * Implements RTMP directive on_play configured in nginx.conf.
	 *
	 * {@code
	 * http://haendel.ddns.net:8080/static/on_play
	 * } Example parameters:
	 * 
	 * <pre>
	 * app=live
	 * flashver=LNX &lt;version&gt;
	 * swfurl=
	 * tcurl=rtmp://haendel.ddns.net:1935/live
	 * pageurl=
	 * addr=&lt;client-ip-address&gt;
	 * clientid=25
	 * call=play
	 * name=&lt;UUID&gt;
	 * start=4294965296
	 * duration=0
	 * reset=0
	 * </pre>
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doPost(request);
		try {
			final ServletParameters servletParameters = new ServletParameters();

			JCommander commander = parseRequestParameters(request, response, servletParameters,
					BASE_URL + getServletPath(), true);
			if (servletParameters.uuid == null) {
				commander.usage();
				return;
			}
			UUID uuid = servletParameters.uuid;

			info(String.format("onPlay: RTMP stream of: %s", uuid));
			update(uuid, PlayerWithStatus::onPlay);

		} catch (Throwable t) {
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}
}
