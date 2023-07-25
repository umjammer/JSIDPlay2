package server.restful.common.parameter.requestpath;

import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.of;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.common.JSIDPlay2Servlet.C64_MUSIC;
import static server.restful.common.JSIDPlay2Servlet.CGSC;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import com.beust.jcommander.JCommander;

import jakarta.servlet.http.HttpServletRequest;
import libsidutils.IOUtils;
import libsidutils.ZipFileUtils;
import net.java.truevfs.access.TFile;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.parameter.ServletUsageFormatter;
import ui.common.comparator.FileComparator;
import ui.common.filefilter.FilteredFileFilter;
import ui.entities.config.SidPlay2Section;

public interface IDirectoryRequestPathServletParameters {

	String getDirectoryPath();

	String getFilter();

	default List<String> getDirectory(JSIDPlay2Servlet servlet, JCommander commander, HttpServletRequest request) {
		SidPlay2Section sidplay2Section = servlet.getConfiguration().getSidplay2Section();

		boolean adminRole = !servlet.isSecured() || request.isUserInRole(ROLE_ADMIN);
		ServletUsageFormatter usageFormatter = (ServletUsageFormatter) commander.getUsageFormatter();

		String path = getDirectoryPath();
		if (path == null || usageFormatter.getException() != null) {
			return null;
		}
		File filePath = new File(path);
		if (path.equals("/")) {
			List<String> files = getRoot(servlet.getDirectoryProperties(), adminRole, sidplay2Section.getHvsc(),
					sidplay2Section.getCgsc());
			if (files != null) {
				return files;
			}
		} else if (path.startsWith(C64_MUSIC)) {
			List<String> files = getCollectionFiles(sidplay2Section.getHvsc(), C64_MUSIC, filePath);
			if (files != null) {
				return files;
			}
		} else if (path.startsWith(CGSC)) {
			List<String> files = getCollectionFiles(sidplay2Section.getCgsc(), CGSC, filePath);
			if (files != null) {
				return files;
			}
		} else {
			for (String directoryLogicalName : servlet.getDirectoryProperties().stringPropertyNames()) {
				String[] splitted = servlet.getDirectoryProperties().getProperty(directoryLogicalName).split(",");
				String directoryValue = splitted.length > 0 ? splitted[0] : null;
				boolean needToBeAdmin = splitted.length > 1 ? Boolean.parseBoolean(splitted[1]) : false;
				if ((!needToBeAdmin || adminRole) && path.startsWith(directoryLogicalName) && directoryValue != null) {
					File root = new TFile(directoryValue);
					List<String> files = getCollectionFiles(root, directoryLogicalName, filePath);
					if (files != null) {
						return files;
					}
				}
			}
		}
		usageFormatter.setException(new FileNotFoundException(path));
		return null;
	}

	default List<String> getRoot(Properties directoryProperties, boolean adminRole, File hvscRoot, File cgscRoot) {
		List<String> result = new ArrayList<>();
		if (hvscRoot != null) {
			result.add(C64_MUSIC + "/");
		}
		if (cgscRoot != null) {
			result.add(CGSC + "/");
		}
		directoryProperties.stringPropertyNames().stream().sorted().forEach(directoryLogicalName -> {
			String[] splitted = directoryProperties.getProperty(directoryLogicalName).split(",");
			boolean needToBeAdmin = splitted.length > 1 ? Boolean.parseBoolean(splitted[1]) : false;
			if (!needToBeAdmin || adminRole) {
				result.add(directoryLogicalName + "/");
			}
		});
		return result;
	}

	default List<String> getCollectionFiles(File rootFile, String virtualCollectionRoot, File filePath) {
		if (rootFile == null) {
			return null;
		}
		File parentFile = ZipFileUtils.newFile(rootFile, filePath.toString().substring(virtualCollectionRoot.length()));

		String virtualParentFile = virtualCollectionRoot + IOUtils.getCollectionName(rootFile, parentFile);

		return concat(of(virtualParentFile + "/../"),
				stream(ofNullable(parentFile.listFiles(new FilteredFileFilter(getFilter()))).orElse(new File[0]))
						.sorted(new FileComparator())
						.map(file -> new File(virtualParentFile, file.getName()) + (file.isDirectory() ? "/" : "")))
				.collect(Collectors.toList());
	}

}
