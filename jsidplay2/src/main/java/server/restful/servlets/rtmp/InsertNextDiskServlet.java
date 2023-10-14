package server.restful.servlets.rtmp;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_STATIC;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.PlayerCleanupTimerTask.update;
import static server.restful.common.filters.RequestLogFilter.FILTER_PARAMETER_SERVLET_NAME;

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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.PlayerWithStatus;
import server.restful.common.filters.RequestLogFilter;
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.parameter.requestparam.VideoRequestParamServletParameters;

@SuppressWarnings("serial")
public class InsertNextDiskServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.rtmp.InsertNextDiskServletParameters")
	public static class InsertNextDiskServletParameters extends VideoRequestParamServletParameters {

	}

	public static final String INSERT_NEXT_DISK_PATH = "/insert_next_disk";

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_STATIC + INSERT_NEXT_DISK_PATH;
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
	 * Insert next disk for Player running as a RTMP live video stream.
	 *
	 * {@code
	 * http://haendel.ddns.net:8080/static/insert_next_disk?name=<uuid>
	 * }
	 * 
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			final InsertNextDiskServletParameters servletParameters = new InsertNextDiskServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					getServletPath());

			if (parser.hasException()) {
				parser.usage();
				return;
			}
			UUID uuid = servletParameters.getUuid();

			StringBuilder diskImageName = new StringBuilder();

			info(String.format("insertNextDisk: RTMP stream of: %s", uuid));
			update(uuid, rtmpPlayerWithStatus -> insertNextDisk(rtmpPlayerWithStatus, diskImageName));

			setOutput(response, MIME_TYPE_JSON, diskImageName.toString());

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

	private void insertNextDisk(PlayerWithStatus rtmpPlayerWithStatus, StringBuilder diskImageName) {
		try {
			diskImageName
					.append(Optional.ofNullable(rtmpPlayerWithStatus.insertNextDisk()).map(File::getName).orElse(""));
		} catch (IOException e) {
			error(e);
		}
	}

}
