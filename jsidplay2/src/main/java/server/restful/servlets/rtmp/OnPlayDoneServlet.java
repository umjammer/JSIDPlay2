package server.restful.servlets.rtmp;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_STATIC;
import static server.restful.common.ServletUtil.error;
import static server.restful.common.ServletUtil.info;
import static server.restful.common.rtmp.PlayerCleanupTimerTask.update;

import java.io.IOException;
import java.util.UUID;

import com.beust.jcommander.Parameters;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.parameter.requestparam.VideoRequestParamServletParameters;
import server.restful.common.rtmp.PlayerWithStatus;

@SuppressWarnings("serial")
@WebServlet(name = "OnPlayDoneServlet", displayName = "OnPlayDoneServlet", urlPatterns = CONTEXT_ROOT_STATIC
		+ "/on_play_done", description = "Stop play video stream. Implements RTMP directive on_play_done configured in nginx.conf")
public class OnPlayDoneServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.rtmp.OnPlayDoneServletParameters")
	public static class OnPlayDoneServletParameters extends VideoRequestParamServletParameters {

	}

	/**
	 * Stop play video stream.
	 * 
	 * Implements RTMP directive on_play_done configured in nginx.conf.
	 *
	 * {@code
	 * http://haendel.ddns.net:8080/static/on_play_done
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
	 * call=play_done
	 * name=&lt;UUID&gt;
	 * start=4294965296
	 * duration=0
	 * reset=0
	 * </pre>
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			WebServlet webServlet = getClass().getAnnotation(WebServlet.class);

			final OnPlayDoneServletParameters servletParameters = new OnPlayDoneServletParameters();

			/* ServletParameterParser parser = */new ServletParameterParser(request, response, servletParameters,
					webServlet, true);

			if (servletParameters.getUuid() == null || servletParameters.getHelp() /* || parser.hasException() */) {
				// Not every video has a valid UUID as it's name! e.g.
				// https://haendel.ddns.net/live/jsidplay2
				return;
			}
			UUID uuid = servletParameters.getUuid();

			info(getServletContext(), String.format("onPlayDone: RTMP stream of: %s", uuid));
			update(uuid, PlayerWithStatus::onPlayDone);

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(getServletContext(), t);
			setOutput(response, t);
		}
	}
}
