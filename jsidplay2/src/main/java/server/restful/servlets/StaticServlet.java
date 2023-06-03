package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_STATIC;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.ContentTypeAndFileExtensions.getMimeType;
import static server.restful.common.IServletSystemProperties.STATIC_RES_MAX_AGE;
import static server.restful.common.parameter.ServletParameterHelper.CONVERT_MESSAGES_DE;
import static server.restful.common.parameter.ServletParameterHelper.CONVERT_MESSAGES_EN;
import static server.restful.common.parameter.ServletParameterHelper.CONVERT_OPTIONS;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.http.HttpHeaders;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.PathUtils;
import libsidutils.ZipFileUtils;
import server.restful.common.ContentTypeAndFileExtensions;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.parameter.RequestPathServletParameters.WebResourceRequestPathServletParameters;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class StaticServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.StaticServletParameters")
	public static class StaticServletParameters extends WebResourceRequestPathServletParameters {

	}

	public StaticServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_STATIC;
	}

	@Override
	public boolean isSecured() {
		return false;
	}

	/**
	 * Get VUE web page.
	 *
	 * E.g. http://haendel.ddns.net:8080/static/c64jukebox.vue
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doGet(request);
		try {
			final StaticServletParameters servletParameters = new StaticServletParameters();

			JCommander commander = parseRequestParameters(request, response, servletParameters, getServletPath());
			if (servletParameters.getResource() == null) {
				commander.usage();
				return;
			}
			try (InputStream source = servletParameters.getResource()) {

				Map<String, String> replacements = new HashMap<>();
				replacements.put("$convertOptions", CONVERT_OPTIONS);
				replacements.put("$convertMessagesEn", CONVERT_MESSAGES_EN);
				replacements.put("$convertMessagesDe", CONVERT_MESSAGES_DE);
				replacements.put("$assembly64Url", configuration.getOnlineSection().getAssembly64Url());
				ContentTypeAndFileExtensions mimeType = getMimeType(PathUtils.getFilenameSuffix(request.getPathInfo()));
				if (mimeType.isText()) {
					response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=" + STATIC_RES_MAX_AGE);
					setOutput(response, mimeType, ZipFileUtils.convertStreamToString(source, "UTF-8", replacements));
				} else {
					response.setHeader(HttpHeaders.CACHE_CONTROL, "public, max-age=" + STATIC_RES_MAX_AGE);
					response.setContentType(mimeType.toString());
					ZipFileUtils.copy(source, response.getOutputStream());
				}
			}
		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

}
