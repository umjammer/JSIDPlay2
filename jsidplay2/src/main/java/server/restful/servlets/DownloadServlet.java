package server.restful.servlets;

import static libsidutils.IOUtils.getFilenameSuffix;
import static libsidutils.ZipFileUtils.newFileInputStream;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.ATTACHMENT;
import static org.apache.tomcat.util.http.fileupload.FileUploadBase.CONTENT_DISPOSITION;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.ContentTypeAndFileExtensions.getMimeType;
import static server.restful.common.filters.RequestLogFilter.FILTER_PARAMETER_SERVLET_NAME;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.Parameters;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.IOUtils;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.filters.RequestLogFilter;
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.parameter.requestpath.FileRequestPathServletParameters;

@SuppressWarnings("serial")
public class DownloadServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.DownloadServletParameters")
	public static class DownloadServletParameters extends FileRequestPathServletParameters {

	}

	public static final String DOWNLOAD_PATH = "/download";

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + DOWNLOAD_PATH;
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
	 * Download SID.
	 *
	 * E.g.
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/download/C64Music/MUSICIANS/D/DRAX/Worktunes/Outro.sid
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			final DownloadServletParameters servletParameters = new DownloadServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					getServletPath());

			final File file = servletParameters.fetchFile(this, parser, request.isUserInRole(ROLE_ADMIN));
			if (file == null || parser.hasException()) {
				parser.usage();
				return;
			}
			response.setContentType(getMimeType(getFilenameSuffix(servletParameters.getFilePath())).toString());
			response.addHeader(CONTENT_DISPOSITION,
					ATTACHMENT + "; filename=" + URLEncoder.encode(file.getName(), StandardCharsets.UTF_8.name()));
			IOUtils.copy(newFileInputStream(file), response.getOutputStream());

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

}
