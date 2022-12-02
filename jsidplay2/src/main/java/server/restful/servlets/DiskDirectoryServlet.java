package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.parameter.ServletParameterHelper.check;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.UUID;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.ZipFileUtils;
import libsidutils.directory.Directory;
import libsidutils.directory.DiskDirectory;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.parameter.RequestPathServletParameters.FileRequestPathServletParameters;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class DiskDirectoryServlet extends JSIDPlay2Servlet {

	static {
		check(DiskDirectoryServletParameters.class);
	}

	@Parameters(resourceBundle = "server.restful.servlets.DiskDirectoryServletParameters")
	public static class DiskDirectoryServletParameters extends FileRequestPathServletParameters {

	}

	public static final String DISK_DIRECTORY_PATH = "/disk-directory";

	public DiskDirectoryServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + DISK_DIRECTORY_PATH;
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

			JCommander commander = parseRequestParameters(request, response, servletParameters, getServletPath());

			final File file = getFile(commander, servletParameters, true);
			if (file == null) {
				commander.usage();
				return;
			}
			Directory directory = new DiskDirectory(extract(file));

			setOutput(response, MIME_TYPE_JSON, OBJECT_MAPPER.writer().writeValueAsString(directory));

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

	protected File extract(final File file) throws IOException, FileNotFoundException {
		File targetDir = new File(configuration.getSidplay2Section().getTmpDir(), UUID.randomUUID().toString());
		File targetFile = new File(targetDir, file.getName());
		targetDir.deleteOnExit();
		targetFile.deleteOnExit();
		targetDir.mkdirs();
		try (FileOutputStream out = new FileOutputStream(targetFile)) {
			ZipFileUtils.copy(file, out);
		}
		return targetFile;
	}

}