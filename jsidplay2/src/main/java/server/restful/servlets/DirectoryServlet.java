package server.restful.servlets;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static ui.entities.config.OnlineSection.APP_SERVER_URL;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.PathUtils;
import libsidutils.ZipFileUtils;
import net.java.truevfs.access.TFile;
import server.restful.common.JSIDPlay2Servlet;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class DirectoryServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.DirectoryServletParameters")
	public static class ServletParameters {

		@Parameter(names = { "--filter" }, descriptionKey = "FILTER", order = -2)
		private String filter;

		@Parameter
		private String filePath;

	}

	public static final String DIRECTORY_PATH = "/directory";

	public DirectoryServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + DIRECTORY_PATH;
	}

	/**
	 * Get directory contents containing music collections.
	 *
	 * E.g.
	 * http://haendel.ddns.net:8080/jsidplay2service/JSIDPlay2REST/directory/C64Music/MUSICIANS?filter=.*%5C.(sid%7Cdat%7Cmus%7Cstr%7Cmp3%7Cmp4%7Cjpg%7Cprg%7Cd64)$
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doGet(request);
		try {
			final ServletParameters servletParameters = new ServletParameters();

			JCommander commander = parseRequestParameters(request, response, servletParameters,
					APP_SERVER_URL + getServletPath() + "/<filePath>?filter=<regexp>");
			if (servletParameters.filePath == null) {
				commander.usage();
				return;
			}
			List<String> files = getDirectory(servletParameters.filePath, servletParameters.filter,
					request.isUserInRole(ROLE_ADMIN));

			response.setContentType(MIME_TYPE_JSON.toString());
			response.getOutputStream().println(new ObjectMapper().writer().writeValueAsString(files));
		} catch (Throwable t) {
			error(t);
			response.setContentType(MIME_TYPE_TEXT.toString());
			t.printStackTrace(new PrintStream(response.getOutputStream()));
		}
		response.setStatus(HttpServletResponse.SC_OK);
	}

	private List<String> getDirectory(String filePath, String filter, boolean adminRole) {
		if (filePath == null || filePath.equals("/")) {
			return getRoot(adminRole);
		} else if (filePath.startsWith(C64_MUSIC)) {
			File root = configuration.getSidplay2Section().getHvsc();
			return getCollectionFiles(root, filePath, filter, C64_MUSIC, adminRole);
		} else if (filePath.startsWith(CGSC)) {
			File root = configuration.getSidplay2Section().getCgsc();
			return getCollectionFiles(root, filePath, filter, CGSC, adminRole);
		}
		for (String directoryLogicalName : directoryProperties.stringPropertyNames()) {
			String[] splitted = directoryProperties.getProperty(directoryLogicalName).split(",");
			String directoryValue = splitted.length > 0 ? splitted[0] : null;
			boolean needToBeAdmin = splitted.length > 1 ? Boolean.parseBoolean(splitted[1]) : false;
			if ((!needToBeAdmin || adminRole) && filePath.startsWith(directoryLogicalName) && directoryValue != null) {
				File root = new TFile(directoryValue);
				return getCollectionFiles(root, filePath, filter, directoryLogicalName, adminRole);
			}
		}
		return getRoot(adminRole);
	}

	private List<String> getCollectionFiles(File rootFile, String path, String filter, String virtualCollectionRoot,
			boolean adminRole) {
		ArrayList<String> result = new ArrayList<>();
		if (rootFile != null) {
			if (path.endsWith("/")) {
				path = path.substring(0, path.length() - 1);
			}
			File file = ZipFileUtils.newFile(rootFile, path.substring(virtualCollectionRoot.length()));
			File[] listFiles = file.listFiles(pathname -> {
				if (pathname.isDirectory() && pathname.getName().endsWith(".tmp")) {
					return false;
				}
				return pathname.isDirectory() || filter == null
						|| pathname.getName().toLowerCase(Locale.US).matches(filter);
			});
			if (listFiles != null) {
				List<File> asList = Arrays.asList(listFiles);
				Collections.sort(asList, (file1, file2) -> {
					if (file1.isDirectory() && !file2.isDirectory()) {
						return -1;
					} else if (!file1.isDirectory() && file2.isDirectory()) {
						return 1;
					} else {
						return file1.getName().compareToIgnoreCase(file2.getName());
					}
				});
				addPath(result, virtualCollectionRoot + PathUtils.getCollectionName(rootFile, file) + "/../", null);
				for (File f : asList) {
					addPath(result, virtualCollectionRoot + PathUtils.getCollectionName(rootFile, f), f);
				}
			}
		}
		if (result.isEmpty()) {
			return getRoot(adminRole);
		}
		return result;
	}

	private void addPath(ArrayList<String> result, String path, File f) {
		result.add(path + (f != null && f.isDirectory() ? "/" : ""));
	}

	private List<String> getRoot(boolean adminRole) {
		List<String> result = new ArrayList<>(Arrays.asList(C64_MUSIC + "/", CGSC + "/"));

		for (String directoryLogicalName : directoryProperties.stringPropertyNames()) {
			String[] splitted = directoryProperties.getProperty(directoryLogicalName).split(",");
			boolean needToBeAdmin = splitted.length > 1 ? Boolean.parseBoolean(splitted[1]) : false;
			if (!needToBeAdmin || adminRole) {
				result.add(directoryLogicalName + "/");
			}
		}
		return result;
	}
}
