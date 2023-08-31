package server.restful.servlets.rtmp;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_STATIC;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.PlayerCleanupTimerTask.update;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

import com.beust.jcommander.Parameters;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.PlayerWithStatus;
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.parameter.requestparam.VideoRequestParamServletParameters;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class InsertNextCartServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.rtmp.InsertNextCartServletParameters")
	public static class InsertNextCartServletParameters extends VideoRequestParamServletParameters {

	}

	public static final String INSERT_NEXT_CART_PATH = "/insert_next_cart";

	public InsertNextCartServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_STATIC + INSERT_NEXT_CART_PATH;
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
		super.doGet(request);
		try {
			final InsertNextCartServletParameters servletParameters = new InsertNextCartServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					getServletPath());

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