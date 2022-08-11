package server.restful.common;

import static java.util.Arrays.asList;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static libsidplay.common.SamplingRate.VERY_LOW;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_XML;
import static server.restful.common.IServletSystemProperties.UPLOAD_MAXIMUM_DURATION;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.bind.JAXBContext;

import org.apache.http.HttpHeaders;
import org.apache.tomcat.util.http.fileupload.FileItemIterator;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;

import com.beust.jcommander.JCommander;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.PathUtils;
import libsidutils.ZipFileUtils;
import net.java.truevfs.access.TFile;
import ui.assembly64.ContentEntry;
import ui.assembly64.ContentEntrySearchResult;
import ui.common.filefilter.AudioTuneFileFilter;
import ui.common.filefilter.CartFileFilter;
import ui.common.filefilter.DiskFileFilter;
import ui.common.filefilter.TapeFileFilter;
import ui.common.filefilter.VideoTuneFileFilter;
import ui.common.util.InternetUtil;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public abstract class JSIDPlay2Servlet extends HttpServlet {

	protected static final AudioTuneFileFilter AUDIO_TUNE_FILE_FILTER = new AudioTuneFileFilter();
	protected static final VideoTuneFileFilter VIDEO_TUNE_FILE_FILTER = new VideoTuneFileFilter();
	protected static final DiskFileFilter DISK_FILE_FILTER = new DiskFileFilter();
	protected static final TapeFileFilter TAPE_FILE_FILTER = new TapeFileFilter();
	protected static final CartFileFilter CART_FILE_FILTER = new CartFileFilter();
	protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule())
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	protected static final String C64_MUSIC = "/C64Music";
	protected static final String CGSC = "/CGSC";

	protected Configuration configuration;

	protected Properties directoryProperties;

	protected JSIDPlay2Servlet(Configuration configuration, Properties directoryProperties) {
		this.configuration = configuration;
		this.directoryProperties = directoryProperties;
	}

	public abstract String getServletPath();

	public Filter createServletFilter() {
		return null;
	}

	protected void doGet(HttpServletRequest request) {
		log(thread() + request(request) + queryString(request) + remoteAddr(request) + localAddr(request) + memory());
	}

	protected void doPost(HttpServletRequest request) {
		log(thread() + request(request) + remoteAddr(request) + localAddr(request) + memory());
	}

	protected void doPut(HttpServletRequest request) {
		log(thread() + request(request) + remoteAddr(request) + localAddr(request) + memory());
	}

	protected void info(String msg) {
		log(thread() + msg);
	}

	protected void error(Throwable t) {
		log(thread() + t.getMessage(), t);
	}

	protected void uncaughtExceptionHandler(Thread thread, Throwable t) {
		log(thread(thread) + t.getMessage());
	}

	private String thread() {
		return thread(Thread.currentThread());
	}

	private String thread(Thread thread) {
		StringBuilder result = new StringBuilder();
		result.append(thread.getName());
		result.append(": ");
		return result.toString();
	}

	private String request(HttpServletRequest request) {
		StringBuilder result = new StringBuilder();
		result.append(request.getMethod());
		result.append(" ");
		result.append(request.getRequestURI());
		return result.toString();
	}

	private String queryString(HttpServletRequest request) {
		StringBuilder result = new StringBuilder();
		if (request.getQueryString() != null) {
			result.append("?");
			result.append(request.getQueryString());
		}
		return result.toString();
	}

	private String remoteAddr(HttpServletRequest request) {
		StringBuilder result = new StringBuilder();
		result.append(", from ");
		result.append(request.getRemoteAddr());
		result.append(" (");
		result.append(request.getRemotePort());
		result.append(")");
		return result.toString();
	}

	private String localAddr(HttpServletRequest request) {
		StringBuilder result = new StringBuilder();
		result.append(", to ");
		result.append(request.getLocalAddr());
		result.append(" (");
		result.append(request.getLocalPort());
		result.append(")");
		return result.toString();
	}

	private String memory() {
		StringBuilder result = new StringBuilder();
		Runtime runtime = Runtime.getRuntime();
		result.append(String.format(", %,dMb/%,dMb", runtime.totalMemory() - runtime.freeMemory() >> 20,
				runtime.maxMemory() >> 20));
		return result.toString();
	}

	private String[] getRequestParameters(HttpServletRequest request) {
		return concat(Collections.list(request.getParameterNames()).stream()
				.flatMap(name -> asList(request.getParameterValues(name)).stream().filter(v -> !"null".equals(v))
						.map(v -> of((name.length() > 1 ? "--" : "-") + name, v)))
				.flatMap(Function.identity()),
				Optional.ofNullable(request.getPathInfo()).map(Stream::of).orElse(empty())).toArray(String[]::new);
	}

	protected JCommander parseRequestParameters(HttpServletRequest request, HttpServletResponse response,
			final Object parameterObject, String programName) throws IOException {
		return parseRequestParameters(request, response, parameterObject, programName, false);
	}

	protected JCommander parseRequestParameters(HttpServletRequest request, HttpServletResponse response,
			final Object parameterObject, String programName, boolean acceptUnknownOptions) throws IOException {
		JCommander commander = JCommander.newBuilder().addObject(parameterObject).programName(programName)
				.columnSize(Integer.MAX_VALUE)
				.console(new PrintStreamConsole(
						new PrintStream(response.getOutputStream(), true, StandardCharsets.UTF_8.toString())))
				.acceptUnknownOptions(acceptUnknownOptions).build();
		commander.setUsageFormatter(new ServletUsageFormatter(commander, request, response));
		commander.parse(getRequestParameters(request));
		return commander;
	}

	@SuppressWarnings("unchecked")
	protected <T> T getInput(HttpServletRequest request, Class<T> tClass) throws IOException {
		try (ServletInputStream inputStream = request.getInputStream()) {
			String contentType = request.getContentType();
			if (contentType == null || MIME_TYPE_JSON.isCompatible(contentType)) {
				return OBJECT_MAPPER.readValue(inputStream, tClass);
			} else if (MIME_TYPE_XML.isCompatible(contentType)) {
				return (T) JAXBContext.newInstance(tClass).createUnmarshaller().unmarshal(inputStream);
			} else if (ServletFileUpload.isMultipartContent(request)) {
				// file upload (multipart/mixed)
				ByteArrayOutputStream result = new ByteArrayOutputStream();
				FileItemIterator itemIterator = new ServletFileUpload().getItemIterator(request);
				while (itemIterator.hasNext()) {
					try (InputStream itemInputStream = itemIterator.next().openStream()) {
						ZipFileUtils.copy(itemInputStream, result);
					}
					// just the first file
					break;
				}
				Constructor<T> constructor = tClass.getConstructor(new Class[] { byte[].class, long.class });
				return constructor.newInstance(result.toByteArray(),
						((long) UPLOAD_MAXIMUM_DURATION * VERY_LOW.getFrequency()));
			} else {
				throw new IOException("Unsupported content type: " + contentType);
			}
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	protected <T> void setOutput(HttpServletRequest request, HttpServletResponse response, T result, Class<T> tClass) {
		try (ServletOutputStream out = response.getOutputStream()) {
			if (result == null) {
				return;
			}
			Optional<String> optionalContentType = Optional.ofNullable(request.getHeader(HttpHeaders.ACCEPT))
					.map(accept -> asList(accept.split(","))).orElse(Collections.emptyList()).stream().findFirst();
			if (!optionalContentType.isPresent() || MIME_TYPE_JSON.isCompatible(optionalContentType.get())) {
				response.setContentType(MIME_TYPE_JSON.toString());
				OBJECT_MAPPER.writeValue(out, result);
			} else if (MIME_TYPE_XML.isCompatible(optionalContentType.get())) {
				response.setContentType(MIME_TYPE_XML.toString());
				JAXBContext.newInstance(tClass).createMarshaller().marshal(result, out);
			}
		} catch (Exception e) {
			// ignore client aborts
		}
	}

	protected void setOutput(HttpServletResponse response, ContentTypeAndFileExtensions ct, Throwable e)
			throws IOException {
		response.setContentType(ct.toString());
		try (PrintStream out = new PrintStream(response.getOutputStream(), true,
				Optional.ofNullable(ct.getCharset()).map(Charset::toString).orElse(StandardCharsets.UTF_8.name()))) {
			e.printStackTrace(out);
		}
	}

	protected void setOutput(HttpServletResponse response, ContentTypeAndFileExtensions ct, String string)
			throws JsonProcessingException, IOException {
		response.setContentType(ct.toString());
		try (PrintStream out = new PrintStream(response.getOutputStream(), true,
				Optional.ofNullable(ct.getCharset()).map(Charset::toString).orElse(StandardCharsets.UTF_8.name()))) {
			out.print(string);
		}
	}

	protected File getAbsoluteFile(ServletBaseParameters servletBaseParameters, boolean adminRole)
			throws FileNotFoundException {
		String path = servletBaseParameters.getFilePath();
		if (path == null) {
			return null;
		}
		if (servletBaseParameters.getItemId() != null && servletBaseParameters.getCategoryId() != null) {
			return fetchAssembly64Files(servletBaseParameters.getItemId(), servletBaseParameters.getCategoryId(),
					path.substring(1));
		}
		if (path.startsWith(C64_MUSIC)) {
			File rootFile = configuration.getSidplay2Section().getHvsc();
			return PathUtils.getFile(path.substring(C64_MUSIC.length()), rootFile, null);
		} else if (path.startsWith(CGSC)) {
			File rootFile = configuration.getSidplay2Section().getCgsc();
			return PathUtils.getFile(path.substring(CGSC.length()), null, rootFile);
		}
		for (String directoryLogicalName : directoryProperties.stringPropertyNames()) {
			String[] splitted = directoryProperties.getProperty(directoryLogicalName).split(",");
			String directoryValue = splitted.length > 0 ? splitted[0] : null;
			boolean needToBeAdmin = splitted.length > 1 ? Boolean.parseBoolean(splitted[1]) : false;
			if ((!needToBeAdmin || adminRole) && path.startsWith(directoryLogicalName) && directoryValue != null) {
				File file = PathUtils.getFile(directoryValue + path.substring(directoryLogicalName.length()),
						new TFile(directoryValue), null);
				if (file.exists()) {
					return file;
				}
			}
		}
		throw new FileNotFoundException(path);
	}

	protected File fetchAssembly64Files(String itemId, String categoryId, String fileId) {
		try {
			String assembly64Url = configuration.getOnlineSection().getAssembly64Url();
			String encodedItemId = new String(Base64.getEncoder().encode(itemId.getBytes()));
			URL url = new URL(assembly64Url + "/leet/search/v2/contententries/" + encodedItemId + "/" + categoryId);
			URLConnection connection = InternetUtil.openConnection(url, configuration.getSidplay2Section());

			ContentEntrySearchResult contentEntries = OBJECT_MAPPER.readValue(connection.getInputStream(),
					ContentEntrySearchResult.class);

			File targetDir = new File(configuration.getSidplay2Section().getTmpDir(), UUID.randomUUID().toString());
			targetDir.deleteOnExit();
			targetDir.mkdirs();

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
				contentEntryFile.mkdirs();
				contentEntryFile.delete();

				fetchAssembly64File(itemId, categoryId, contentEntry.getId(), contentEntryFile);
				contentEntryFile.deleteOnExit();

				if (Objects.equals(contentEntry.getId(), fileId)) {
					result = contentEntryFile;
				}
			}
			return result;
		} catch (IOException e) {
			System.err.println("Unexpected result: " + e.getMessage());
		}
		return null;
	}

	private void fetchAssembly64File(String itemId, String categoryId, String fileId, File contentEntryFile) {
		try {
			String assembly64Url = configuration.getOnlineSection().getAssembly64Url();
			String encodedItemId = new String(Base64.getEncoder().encode(itemId.getBytes()));
			String encodedContentEntryId = new String(Base64.getEncoder().encode(fileId.getBytes()));
			URL url = new URL(assembly64Url + "/leet/search/v2/binary/" + encodedItemId + "/" + categoryId + "/"
					+ encodedContentEntryId);
			URLConnection connection = InternetUtil.openConnection(url, configuration.getSidplay2Section());

			try (OutputStream outputStream = new FileOutputStream(contentEntryFile)) {
				ZipFileUtils.copy(connection.getInputStream(), outputStream);
			}
		} catch (IOException e) {
			System.err.println("Unexpected result: " + e.getMessage());
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
