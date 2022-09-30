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
import server.restful.common.converter.UUIDConverter;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class InsertNextDiskServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.rtmp.InsertNextDiskServletParameters")
	public static class InsertNextDiskServletParameters {

		@Parameter(names = { "--name" }, descriptionKey = "NAME", converter = UUIDConverter.class, required = true)
		private UUID uuid;

	}

	public static final String INSERT_NEXT_DISK_PATH = "/insert_next_disk";

	public InsertNextDiskServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_STATIC + INSERT_NEXT_DISK_PATH;
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
		super.doGet(request);
		try {
			final InsertNextDiskServletParameters servletParameters = new InsertNextDiskServletParameters();

			JCommander commander = parseRequestParameters(request, response, servletParameters, getServletPath());
			if (servletParameters.uuid == null) {
				commander.usage();
				return;
			}
			UUID uuid = servletParameters.uuid;

			info(String.format("insertNextDisk: RTMP stream of: %s", uuid));
			StringBuilder diskImageName = new StringBuilder();
			update(uuid, rtmpPlayerWithStatus -> diskImageName.append(rtmpPlayerWithStatus.insertNextDisk().getName()));

			setOutput(request, response, diskImageName.toString(), String.class);

		} catch (Throwable t) {
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

}
