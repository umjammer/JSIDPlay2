package server.restful.servlets.rtmp;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_STATIC;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.PlayerCleanupTimerTask.update;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
@WebServlet(name = "InsertNextCartServlet", urlPatterns = CONTEXT_ROOT_STATIC + "/insert_next_cart")
public class InsertNextCartServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.rtmp.InsertNextCartServletParameters")
	public static class InsertNextCartServletParameters extends VideoRequestParamServletParameters {

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
		return true;
	}

	/**
	 * Insert next cart for Player running as a RTMP live video stream.
	 *
	 * {@code
	 * http://haendel.ddns.net:8080/static/insert_next_cart?name=<uuid>
	 * }
	 * 
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			final InsertNextCartServletParameters servletParameters = new InsertNextCartServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					getClass().getAnnotation(WebServlet.class));

			if (parser.hasException()) {
				parser.usage();
				return;
			}
			UUID uuid = servletParameters.getUuid();

			StringBuilder cartImageName = new StringBuilder();

			info(String.format("insertNextCart: RTMP stream of: %s", uuid));
			update(uuid, rtmpPlayerWithStatus -> insertNextCart(rtmpPlayerWithStatus, cartImageName));

			setOutput(response, MIME_TYPE_JSON, cartImageName.toString());

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

	private void insertNextCart(PlayerWithStatus rtmpPlayerWithStatus, StringBuilder cartImageName) {
		try {
			cartImageName
					.append(Optional.ofNullable(rtmpPlayerWithStatus.insertNextCart()).map(File::getName).orElse(null));
		} catch (IOException e) {
			error(e);
		}
	}

}
