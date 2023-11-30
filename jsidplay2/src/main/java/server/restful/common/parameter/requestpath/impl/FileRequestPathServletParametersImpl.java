package server.restful.common.parameter.requestpath.impl;

import static server.restful.common.JSIDPlay2Servlet.C64_MUSIC;
import static server.restful.common.JSIDPlay2Servlet.CART_FILE_FILTER;
import static server.restful.common.JSIDPlay2Servlet.CGSC;
import static server.restful.common.JSIDPlay2Servlet.DISK_FILE_FILTER;
import static server.restful.common.JSIDPlay2Servlet.OBJECT_MAPPER;
import static server.restful.common.JSIDPlay2Servlet.TAPE_FILE_FILTER;
import static server.restful.common.JSIDPlay2Servlet.VIDEO_TUNE_FILE_FILTER;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

import jakarta.servlet.annotation.ServletSecurity;
import libsidutils.IOUtils;
import net.java.truevfs.access.TFile;
import server.restful.common.ServletUtil;
import server.restful.common.parameter.ServletParameterParser;
import ui.assembly64.ContentEntry;
import ui.assembly64.ContentEntrySearchResult;
import ui.common.util.InternetUtil;
import ui.entities.config.Configuration;
import ui.entities.config.SidPlay2Section;

public abstract class FileRequestPathServletParametersImpl {

	private static final String ASSEMBLY64_CSDB_REU_ID = "25";

	public File fetchFile(Configuration configuration, Properties directoryProperties, ServletParameterParser parser,
			ServletSecurity servletSecurity, boolean isAdmin) throws IOException, URISyntaxException {
		SidPlay2Section sidplay2Section = configuration.getSidplay2Section();

		boolean adminRole = !ServletUtil.isSecured(servletSecurity) || isAdmin;

		String path = getFilePath();
		if (path == null) {
			return null;
		}
		if (getItemId() != null && getCategoryId() != null) {
			File file = fetchAssembly64File(configuration, getItemId(), getCategoryId(), path.substring(1));
			if (file != null && file.exists()) {
				return file;
			}
		} else if (sidplay2Section.getHvsc() != null && path.startsWith(C64_MUSIC)) {
			File file = IOUtils.getFile(path.substring(C64_MUSIC.length()), sidplay2Section.getHvsc(), null);
			if (file.exists() && file.getAbsolutePath().startsWith(sidplay2Section.getHvsc().getAbsolutePath())) {
				return file;
			}
		} else if (sidplay2Section.getCgsc() != null && path.startsWith(CGSC)) {
			File file = IOUtils.getFile(path.substring(CGSC.length()), null, sidplay2Section.getCgsc());
			if (file.exists() && file.getAbsolutePath().startsWith(sidplay2Section.getCgsc().getAbsolutePath())) {
				return file;
			}
		} else if (new File(path).getAbsolutePath().startsWith(sidplay2Section.getTmpDir().getAbsolutePath())) {
			// XXX getCanonicalPath ???
			File file = new File(path);
			if (file.exists()) {
				return file;
			}
		} else {
			for (String directoryLogicalName : directoryProperties.stringPropertyNames()) {
				String[] splitted = directoryProperties.getProperty(directoryLogicalName).split(",");
				String directoryValue = splitted.length > 0 ? splitted[0] : null;
				boolean needToBeAdmin = splitted.length > 1 ? Boolean.parseBoolean(splitted[1]) : false;
				if ((!needToBeAdmin || adminRole) && path.startsWith(directoryLogicalName) && directoryValue != null) {
					TFile root = new TFile(directoryValue);
					File file = IOUtils.getFile(path.substring(directoryLogicalName.length()), root, null);
					if (file.exists() && file.getAbsolutePath().startsWith(root.getAbsolutePath())) {
						return file;
					}
				}
			}
		}
		parser.setException(new FileNotFoundException(path));
		return null;
	}

	public boolean isREU() {
		return ASSEMBLY64_CSDB_REU_ID.equals(getCategoryId());
	}

	private File fetchAssembly64File(Configuration configuration, String itemId, String categoryId, String fileId)
			throws IOException, URISyntaxException {
		String assembly64Url = configuration.getOnlineSection().getAssembly64Url();
		String encodedItemId = new String(Base64.getEncoder().encode(itemId.getBytes()));
		URL url = new URI(assembly64Url + "/leet/search/v2/contententries/" + encodedItemId + "/" + categoryId).toURL();
		URLConnection connection = InternetUtil.openConnection(url, configuration.getSidplay2Section());

		ContentEntrySearchResult contentEntries = OBJECT_MAPPER.readValue(connection.getInputStream(),
				ContentEntrySearchResult.class);

		File targetDir = new File(configuration.getSidplay2Section().getTmpDir(), UUID.randomUUID().toString());
		targetDir.mkdir();

		File file = new File(fileId);
		boolean mustFetchAttachments = VIDEO_TUNE_FILE_FILTER.accept(file) || DISK_FILE_FILTER.accept(file)
				|| TAPE_FILE_FILTER.accept(file) || CART_FILE_FILTER.accept(file);

		List<ContentEntry> contentEntriesToFetch = contentEntries.getContentEntry().stream()
				.filter(contentEntry -> Objects.equals(contentEntry.getId(), fileId)
						|| (mustFetchAttachments && (DISK_FILE_FILTER.accept(new File(contentEntry.getId()))
								|| TAPE_FILE_FILTER.accept(new File(contentEntry.getId()))
								|| CART_FILE_FILTER.accept(new File(contentEntry.getId())))))
				.collect(Collectors.toList());

		File result = null;
		for (ContentEntry contentEntry : contentEntriesToFetch) {

			File contentEntryFile = new File(targetDir, contentEntry.getId());
			// create file as directory to handle sub-directories (subdir/file.txt)
			contentEntryFile.getParentFile().mkdirs();

			fetchAssembly64File(configuration, itemId, categoryId, contentEntry.getId(), contentEntryFile);

			if (Objects.equals(contentEntry.getId(), fileId)) {
				result = contentEntryFile;
			}
		}
		return result;
	}

	private void fetchAssembly64File(Configuration configuration, String itemId, String categoryId, String fileId,
			File contentEntryFile) throws IOException, URISyntaxException {
		String assembly64Url = configuration.getOnlineSection().getAssembly64Url();
		String encodedItemId = new String(Base64.getEncoder().encode(itemId.getBytes()));
		String encodedContentEntryId = new String(Base64.getEncoder().encode(fileId.getBytes()));
		URL url = new URI(assembly64Url + "/leet/search/v2/binary/" + encodedItemId + "/" + categoryId + "/"
				+ encodedContentEntryId).toURL();
		URLConnection connection = InternetUtil.openConnection(url, configuration.getSidplay2Section());

		try (OutputStream outputStream = new FileOutputStream(contentEntryFile)) {
			IOUtils.copy(connection.getInputStream(), outputStream);
		}
	}

	public abstract String getFilePath();

	public abstract String getItemId();

	public abstract String getCategoryId();
}
