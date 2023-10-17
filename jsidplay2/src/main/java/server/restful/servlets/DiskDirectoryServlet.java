package server.restful.servlets;

import static libsidutils.ZipFileUtils.newFileInputStream;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.ROLE_USER;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.beust.jcommander.Parameters;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.IOUtils;
import libsidutils.directory.Directory;
import libsidutils.directory.DiskDirectory;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.filters.RequestLogFilter;
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.parameter.requestpath.FileRequestPathServletParameters;

@SuppressWarnings("serial")
@WebServlet(name = "DiskDirectoryServlet", urlPatterns = CONTEXT_ROOT_SERVLET + "/disk-directory/*")
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { ROLE_USER, ROLE_ADMIN }))
public class DiskDirectoryServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.DiskDirectoryServletParameters")
	public static class DiskDirectoryServletParameters extends FileRequestPathServletParameters {

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

	/**
	 * Get Directory of Disk.
	 *
	 * E.g.
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/disk-directory/C64Music/10_Years_HVSC_1.d64
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			final DiskDirectoryServletParameters servletParameters = new DiskDirectoryServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					getClass().getAnnotation(WebServlet.class));

			final File file = servletParameters.fetchFile(this, parser, request.isUserInRole(ROLE_ADMIN));
			if (file == null || parser.hasException()) {
				parser.usage();
				return;
			}
			Directory directory = createDiskDirectory(file);

			setOutput(response, MIME_TYPE_JSON, OBJECT_MAPPER.writer().writeValueAsString(directory));

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

	private Directory createDiskDirectory(final File file) throws IOException, FileNotFoundException {
		File extractedFile = extract(file);
		Directory directory = new DiskDirectory(extractedFile);
		IOUtils.deleteDirectory(extractedFile.getParentFile());
		return directory;
	}

	protected File extract(final File file) throws IOException, FileNotFoundException {
		File targetDir = new File(configuration.getSidplay2Section().getTmpDir(), UUID.randomUUID().toString());
		File targetFile = new File(targetDir, file.getName());
		targetDir.mkdirs();
		try (FileOutputStream out = new FileOutputStream(targetFile)) {
			IOUtils.copy(newFileInputStream(file), out);
		}
		return targetFile;
	}

}