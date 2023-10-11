package server.restful.servlets;

import static libsidutils.ZipFileUtils.newFileInputStream;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

import com.beust.jcommander.Parameters;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.IOUtils;
import libsidutils.directory.Directory;
import libsidutils.directory.DiskDirectory;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.parameter.requestpath.FileRequestPathServletParameters;

@SuppressWarnings("serial")
public class DiskDirectoryServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.DiskDirectoryServletParameters")
	public static class DiskDirectoryServletParameters extends FileRequestPathServletParameters {

	}

	public static final String DISK_DIRECTORY_PATH = "/disk-directory";

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + DISK_DIRECTORY_PATH;
	}

	@Override
	public boolean isSecured() {
		return true;
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
		super.doGet(request);
		try {
			final DiskDirectoryServletParameters servletParameters = new DiskDirectoryServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					getServletPath());

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