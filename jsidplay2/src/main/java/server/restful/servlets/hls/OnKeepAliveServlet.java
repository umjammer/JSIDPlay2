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
import server.restful.common.converter.FractionSecondsToMsConverter;
import server.restful.common.parameter.ServletUsageFormatter;
import server.restful.common.parameter.requestparam.VideoRequestParamServletParameters;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class OnKeepAliveServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.hls.OnKeepAliveServletParameters")
	public static class OnKeepAliveServletParameters extends VideoRequestParamServletParameters {

		private Long currentTime = null;

		public Long getCurrentTime() {
			return currentTime;
		}

		@Parameter(names = {
				"--currentTime" }, converter = FractionSecondsToMsConverter.class, descriptionKey = "CURRENT_TIME", order = -2)
		public void setCurrentTime(Long currentTime) {
			this.currentTime = currentTime;
		}
	}

	public static final String ON_KEEP_ALIVE_PATH = "/on_keep_alive";

	public OnKeepAliveServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_STATIC + ON_KEEP_ALIVE_PATH;
	}

	@Override
	public boolean isSecured() {
		return false;
	}

	/**
	 * Keep alive video stream.
	 * 
	 * Compensates for HLS's missing directives (on_play and on_play_done),
	 * notifies, that the stream is still being consumed.
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
//		super.doGet(request);   // Calls are very frequent, therefore we are silent here
		try {
			final OnKeepAliveServletParameters servletParameters = new OnKeepAliveServletParameters();

			JCommander commander = parseRequestParameters(request, response, servletParameters, getServletPath());
			if (((ServletUsageFormatter) commander.getUsageFormatter()).getException() != null) {
				commander.usage();
				return;
			}
			UUID uuid = servletParameters.getUuid();
			Long currentTime = servletParameters.getCurrentTime();

//			info(String.format("onKeepAlive: HLS stream of: %s", uuid));   // Calls are very frequent, therefore we are silent here
			update(uuid, playerWithStatus -> playerWithStatus.onKeepAlive(currentTime));

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

}
