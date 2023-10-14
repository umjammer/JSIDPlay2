package server.restful.servlets.rtmp;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_STATIC;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.PlayerCleanupTimerTask.update;
import static server.restful.common.filters.RequestLogFilter.FILTER_PARAMETER_SERVLET_NAME;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.beust.jcommander.Parameters;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.PlayerWithStatus;
import server.restful.common.filters.RequestLogFilter;
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.parameter.requestparam.VideoRequestParamServletParameters;

@SuppressWarnings("serial")
public class SetSidModel6581Servlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.rtmp.SetSidModel6581ServletParameters")
	public static class SetSidModel6581ServletParameters extends VideoRequestParamServletParameters {

	}

	public static final String SET_DEFAULT_SID_MODEL_6581_PATH = "/set_default_sid_model_6581";

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_STATIC + SET_DEFAULT_SID_MODEL_6581_PATH;
	}

	@Override
	public List<Filter> getServletFilters() {
		return Arrays.asList(new RequestLogFilter());
	}

	@Override
	public Map<String, String> getServletFiltersParameterMap() {
		Map<String, String> result = new HashMap<>();
		result.put(FILTER_PARAMETER_SERVLET_NAME, getClass().getSimpleName());
		return result;
	}

	@Override
	public boolean isSecured() {
		return true;
	}

	/**
	 * Set default chip model to MOS6581 for Player running as a RTMP live video
	 * stream.
	 *
	 * {@code
	 * http://haendel.ddns.net:8080/static/set_default_sid_model_6581?name=<uuid>
	 * }
	 * 
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			final SetSidModel6581ServletParameters servletParameters = new SetSidModel6581ServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					getServletPath());

			if (parser.hasException()) {
				parser.usage();
				return;
			}
			UUID uuid = servletParameters.getUuid();

			info(String.format("setDefaultSidModel6581: RTMP stream of: %s", uuid));
			update(uuid, PlayerWithStatus::setDefaultSidModel6581);

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

}
