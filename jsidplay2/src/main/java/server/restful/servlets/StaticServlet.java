package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_STATIC;
import static server.restful.common.ContentTypeAndFileExtensions.getMimeType;
import static server.restful.common.IServletSystemProperties.BASE_URL;
import static server.restful.common.IServletSystemProperties.CACHE_CONTROL_RESPONSE_HEADER_CACHED;
import static server.restful.common.IServletSystemProperties.CACHE_CONTROL_RESPONSE_HEADER_UNCACHED;
import static server.restful.common.ServletUtil.error;
import static server.restful.common.parameter.ServletParameterHelper.CONVERT_MESSAGES_DE;
import static server.restful.common.parameter.ServletParameterHelper.CONVERT_MESSAGES_EN;
import static server.restful.common.parameter.ServletParameterHelper.CONVERT_OPTIONS;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.apache.http.HttpHeaders;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.IOUtils;
import server.restful.common.ContentTypeAndFileExtensions;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.TeaVMFormat;
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.parameter.requestpath.WebResourceRequestPathServletParameters;

@SuppressWarnings("serial")
@WebServlet(name = "StaticServlet", displayName = "StaticServlet", urlPatterns = CONTEXT_ROOT_STATIC
		+ "/*", description = "Get VUE demo pages or web resources")
public class StaticServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.StaticServletParameters")
	public static class StaticServletParameters extends WebResourceRequestPathServletParameters {

		@Parameter(names = { "--help", "-h" }, arity = 1, descriptionKey = "USAGE", help = true, order = 0)
		private Boolean help = Boolean.FALSE;

		private Boolean useDevTools = Boolean.FALSE;

		public Boolean getUseDevTools() {
			return useDevTools;
		}

		@Parameter(names = "--devtools", arity = 1, descriptionKey = "USE_DEV_TOOLS", hidden = true, order = 1)
		public void setUseDevTools(Boolean useDevTools) {
			this.useDevTools = useDevTools;
		}

		private TeaVMFormat teaVMFormat = TeaVMFormat.JS;

		public TeaVMFormat getTeaVMFormat() {
			return teaVMFormat;
		}

		@Parameter(names = "--teavmFormat", arity = 1, descriptionKey = "TEAVM_FORMAT", order = 2)
		public void setTeaVMFormat(TeaVMFormat teaVMFormat) {
			this.teaVMFormat = teaVMFormat;
		}

	}

	/**
	 * Get VUE demo page or web resources.
	 *
	 * E.g. http://haendel.ddns.net:8080/static/c64jukebox.vue
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			WebServlet webServlet = getClass().getAnnotation(WebServlet.class);

			final StaticServletParameters servletParameters = new StaticServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					webServlet);

			if (servletParameters.help || parser.hasException()) {
				parser.usage();
				return;
			}
			try (InputStream source = servletParameters.getResource()) {
				ContentTypeAndFileExtensions mimeType = getMimeType(IOUtils.getFilenameSuffix(request.getPathInfo()));

				Map<String, String> replacements = new HashMap<>();
				replacements.put("baseUrl", BASE_URL);
				replacements.put("convertOptions", CONVERT_OPTIONS);
				replacements.put("convertMessagesEn", CONVERT_MESSAGES_EN);
				replacements.put("convertMessagesDe", CONVERT_MESSAGES_DE);
				replacements.put("assembly64Url", configuration.getOnlineSection().getAssembly64Url());
				replacements.put("year", String.valueOf(LocalDate.now().getYear()));
				replacements.put("teaVMFormat", servletParameters.getTeaVMFormat().name().toLowerCase(Locale.US));
				replacements.put("teaVMFormatName", servletParameters.getTeaVMFormat().getTeaVMFormatName());
				if (!ContentTypeAndFileExtensions.MIME_TYPE_JAVASCRIPT.isCompatible(mimeType.getMimeType())) {
					replacements.put("min", Boolean.TRUE.equals(servletParameters.getUseDevTools()) ? "" : ".min");
					replacements.put("prod", Boolean.TRUE.equals(servletParameters.getUseDevTools()) ? "" : ".prod");
					replacements.put("lib",
							Boolean.TRUE.equals(servletParameters.getUseDevTools()) ? "lib" : "lib-minified");
				}
				if (mimeType.isCacheable()) {
					response.setHeader(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_RESPONSE_HEADER_CACHED);
				} else {
					response.setHeader(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL_RESPONSE_HEADER_UNCACHED);
				}
				if (mimeType.isText()) {
					setOutput(mimeType, response, IOUtils.convertStreamToString(source, "UTF-8", replacements));
				} else {
					setOutput(mimeType, response, source);
				}
			}
		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(getServletContext(), t);
			setOutput(response, t);
		}
	}

}
