package server.restful.servlets.hls;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_STATIC;
import static server.restful.common.ServletUtil.error;
import static server.restful.common.ServletUtil.info;
import static server.restful.common.rtmp.PlayerCleanupTimerTask.update;

import java.io.IOException;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.converter.FractionSecondsToMsConverter;
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.parameter.requestparam.VideoRequestParamServletParameters;

@SuppressWarnings("serial")
@WebServlet(name = "OnKeepAliveServlet", displayName = "OnKeepAliveServlet", urlPatterns = CONTEXT_ROOT_STATIC
		+ "/on_keep_alive", description = "Keep alive video stream")
public class OnKeepAliveServlet extends JSIDPlay2Servlet {

	private static final Logger LOG = Logger.getLogger(OnKeepAliveServlet.class.getName());

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

		private Long bufferedEnd = null;

		public Long getBufferedEnd() {
			return bufferedEnd;
		}

		@Parameter(names = {
				"--bufferedEnd" }, converter = FractionSecondsToMsConverter.class, descriptionKey = "BUFFERED_END", order = -3)
		public void setBufferedEnd(Long bufferedEnd) {
			this.bufferedEnd = bufferedEnd;
		}
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
		try {
			WebServlet webServlet = getClass().getAnnotation(WebServlet.class);

			final OnKeepAliveServletParameters servletParameters = new OnKeepAliveServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					webServlet);

			if (parser.hasException()) {
				parser.usage();
				return;
			}
			UUID uuid = servletParameters.getUuid();
			Long currentTime = servletParameters.getCurrentTime();
			Long bufferedEnd = servletParameters.getBufferedEnd();

			if (LOG.isLoggable(Level.FINEST)) {
				info(getServletContext(), String.format("onKeepAlive: HLS stream of: %s", uuid));
			}
			update(uuid, playerWithStatus -> playerWithStatus.onKeepAlive(currentTime, bufferedEnd));

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(getServletContext(), t);
			setOutput(response, t);
		}
	}

}
