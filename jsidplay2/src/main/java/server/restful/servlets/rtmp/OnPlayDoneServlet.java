package server.restful.servlets.rtmp;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_STATIC;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.PlayerCleanupTimerTask.update;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.beust.jcommander.Parameters;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.PlayerWithStatus;
import server.restful.common.filters.RequestLogFilter;
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.parameter.requestparam.VideoRequestParamServletParameters;

@SuppressWarnings("serial")
@WebServlet(name = "OnPlayDoneServlet", urlPatterns = CONTEXT_ROOT_STATIC + "/on_play_done")
public class OnPlayDoneServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.rtmp.OnPlayDoneServletParameters")
	public static class OnPlayDoneServletParameters extends VideoRequestParamServletParameters {

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

	@Override
	public boolean isSecured() {
		return false;
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
			final OnPlayDoneServletParameters servletParameters = new OnPlayDoneServletParameters();

			/* ServletParameterParser parser = */new ServletParameterParser(request, response, servletParameters,
					getClass().getAnnotation(WebServlet.class), true);

			if (servletParameters.getUuid() == null /* || parser.hasException() */) {
				// Not every video has a valid UUID as it's name! e.g.
				// https://haendel.ddns.net/live/jsidplay2
				return;
			}
			UUID uuid = servletParameters.getUuid();

			info(String.format("onPlayDone: RTMP stream of: %s", uuid));
			update(uuid, PlayerWithStatus::onPlayDone);

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}
}
