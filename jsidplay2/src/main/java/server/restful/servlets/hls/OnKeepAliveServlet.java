package server.restful.servlets.hls;

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
public class OnKeepAliveServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.hls.OnKeepAliveServletParameters")
	public static class ServletParameters {

		@Parameter(names = { "--name" }, descriptionKey = "NAME", converter = UUIDConverter.class, order = -2)
		private UUID uuid;

	}

	public static final String ON_KEEP_ALIVE_PATH = "/on_keep_alive";

	public OnKeepAliveServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_STATIC + ON_KEEP_ALIVE_PATH;
	}

	/**
	 * Keep alive video stream.
	 * 
	 * Implements HLS missing notifications, that the stream is still produced.
	 *
	 * {@code
	 * http://haendel.ddns.net:8080/static/on_keep_alive
	 * } Example parameters:
	 * 
	 * <pre>
	 * name=&lt;UUID&gt;
	 * </pre>
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doGet(request);
		try {
			final ServletParameters servletParameters = new ServletParameters();

			JCommander commander = parseRequestParameters(request, response, servletParameters, getServletPath(), true);
			if (servletParameters.uuid == null) {
				commander.usage();
				return;
			}
			UUID uuid = servletParameters.uuid;

			info(String.format("onKeepAlive: HLS stream of: %s", uuid));
			update(uuid, PlayerWithStatus::onKeepAlive);

		} catch (Throwable t) {
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}
}
