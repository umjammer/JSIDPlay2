package server.restful.common;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;
import static libsidutils.IOUtils.getFileSize;
import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.HttpHeaders.CONTENT_LENGTH;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_XML;
import static server.restful.common.IServletSystemProperties.UNCAUGHT_EXCEPTION_HANDLER_EXCEPTIONS;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.xml.bind.JAXBContext;

import org.apache.tomcat.util.http.fileupload.FileItemIterator;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;
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
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.IOUtils;
import libsidutils.siddatabase.SidDatabase;
import server.restful.common.parameter.ServletUsageFormatter;
import sidplay.filefilter.AudioTuneFileFilter;
import sidplay.filefilter.VideoTuneFileFilter;
import ui.common.filefilter.CartFileFilter;
import ui.common.filefilter.DiskFileFilter;
import ui.common.filefilter.TapeFileFilter;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public abstract class JSIDPlay2Servlet extends HttpServlet {

	public static final AudioTuneFileFilter AUDIO_TUNE_FILE_FILTER = new AudioTuneFileFilter();
	public static final VideoTuneFileFilter VIDEO_TUNE_FILE_FILTER = new VideoTuneFileFilter();
	public static final DiskFileFilter DISK_FILE_FILTER = new DiskFileFilter();
	public static final TapeFileFilter TAPE_FILE_FILTER = new TapeFileFilter();
	public static final CartFileFilter CART_FILE_FILTER = new CartFileFilter();
	public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule())
			.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	public static final String C64_MUSIC = "/C64Music";
	public static final String CGSC = "/CGSC";

	protected Configuration configuration;

	protected Properties directoryProperties;

	protected JSIDPlay2Servlet(Configuration configuration, Properties directoryProperties) {
		this.configuration = configuration;
		this.directoryProperties = directoryProperties;
	}

	public Configuration getConfiguration() {
		return configuration;
	}

	public Properties getDirectoryProperties() {
		return directoryProperties;
	}

	public abstract String getServletPath();

	public abstract boolean isSecured();

	public String getURLPattern() {
		return getServletPath() + "/*";
	}

	public List<Filter> getServletFilters() {
		return Collections.emptyList();
	}

	public Map<String, String> getServletFiltersParameterMap() {
		return Collections.emptyMap();
	}

	protected void doGet(HttpServletRequest request) {
		log(thread() + user(request) + remoteAddr(request) + localAddr(request) + request(request) + memory());
	}

	protected void doPost(HttpServletRequest request) {
		log(thread() + user(request) + remoteAddr(request) + localAddr(request) + request(request) + memory());
	}

	protected void doPut(HttpServletRequest request) {
		log(thread() + user(request) + remoteAddr(request) + localAddr(request) + request(request) + memory());
	}

	protected void info(String msg, Thread... parentThreads) {
		log(threads(parentThreads) + thread() + msg);
	}

	protected void warn(String msg, Thread... parentThreads) {
		log(threads(parentThreads) + thread() + msg, null);
	}

	protected void error(Throwable t, Thread... parentThreads) {
		log(threads(parentThreads) + thread() + t.getMessage(), t);
	}

	protected void uncaughtExceptionHandler(Throwable t, Thread thread, Thread... parentThreads) {
		log(threads(parentThreads) + thread(thread) + t.getMessage(), UNCAUGHT_EXCEPTION_HANDLER_EXCEPTIONS ? t : null);
	}

	private StringBuilder threads(Thread... threads) {
		return of(threads).map(this::thread).collect(StringBuilder::new, StringBuilder::append, StringBuilder::append);
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

	private String remoteAddr(HttpServletRequest request) {
		StringBuilder result = new StringBuilder();
		result.append("from ");
		result.append(request.getRemoteAddr());
		result.append(" (");
		result.append(request.getRemotePort());
		result.append(") ");
		return result.toString();
	}

	private String localAddr(HttpServletRequest request) {
		StringBuilder result = new StringBuilder();
		result.append("to ");
		result.append(request.getLocalAddr());
		result.append(" (");
		result.append(request.getLocalPort());
		result.append("), ");
		return result.toString();
	}

	private String user(HttpServletRequest request) {
		StringBuilder result = new StringBuilder();
		result.append("user ");
		result.append(Optional.ofNullable(request.getRemoteUser()).orElse("<anonymous>"));
		result.append(", ");
		return result.toString();
	}

	private String request(HttpServletRequest request) {
		StringBuilder result = new StringBuilder();
		result.append(request.getMethod());
		result.append(" ");
		result.append(request.getRequestURI());
		if (request.getQueryString() != null) {
			result.append("?");
			result.append(request.getQueryString());
		}
		if (request.getContentType() != null) {
			result.append(" ");
			result.append(CONTENT_TYPE);
			result.append("=");
			result.append(request.getContentType());
		}
		if (request.getContentLengthLong() != -1L) {
			result.append(", ");
			result.append(CONTENT_LENGTH);
			result.append("=");
			result.append(getFileSize(request.getContentLengthLong()));
		}
		result.append(", ");
		return result.toString();
	}

	private String memory() {
		StringBuilder result = new StringBuilder();
		Runtime runtime = Runtime.getRuntime();
		result.append(getFileSize(runtime.totalMemory() - runtime.freeMemory()));
		result.append("/");
		result.append(getFileSize(runtime.maxMemory()));
		return result.toString();
	}

	private String[] getRequestParameters(HttpServletRequest request) {
		return concat(
				Collections.list(request.getParameterNames()).stream()
						.flatMap(name -> asList(request.getParameterValues(name)).stream()
								.filter(v -> !"null".equals(v) && !"undefined".equals(v))
								.map(v -> of((name.length() > 1 ? "--" : "-") + name, v)))
						.flatMap(Function.identity()),
				ofNullable(request.getPathInfo()).map(Stream::of).orElse(empty())).toArray(String[]::new);
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
		String[] requestParameters = getRequestParameters(request);
		ServletUsageFormatter usageFormatter = new ServletUsageFormatter(commander, request, response,
				requestParameters);
		commander.setUsageFormatter(usageFormatter);
		try {
			commander.parse(requestParameters);
		} catch (ParameterException e) {
			usageFormatter.setException(e);
		}
		return commander;
	}

	protected String getCollectionName(File file) throws IOException, SidTuneError {
		String result = "";

		File hvscRoot = configuration.getSidplay2Section().getHvsc();
		if (hvscRoot != null) {
			// 1st Try from full path name...
			result = IOUtils.getCollectionName(hvscRoot, file);
			if (result.isEmpty()) {
				// ... then try from MD5
				result = new SidDatabase(hvscRoot).getPath(SidTune.load(file));
			}
		}
		return result;
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
						IOUtils.copy(itemInputStream, result);
					}
					// just the first file
					break;
				}
				Constructor<T> constructor = tClass.getConstructor(new Class[] { byte[].class, boolean.class });
				return constructor.newInstance(result.toByteArray(), true);
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
			Optional<String> optionalContentType = ofNullable(request.getHeader(ACCEPT))
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
				ofNullable(ct.getCharset()).map(Charset::toString).orElse(StandardCharsets.UTF_8.name()))) {
			out.println(e.getClass().getSimpleName() + ": " + e.getMessage());
		}
	}

	protected void setOutput(HttpServletResponse response, ContentTypeAndFileExtensions ct, String string)
			throws JsonProcessingException, IOException {
		response.setContentType(ct.toString());
		try (PrintStream out = new PrintStream(response.getOutputStream(), true,
				ofNullable(ct.getCharset()).map(Charset::toString).orElse(StandardCharsets.UTF_8.name()))) {
			out.print(string);
		}
	}

}
