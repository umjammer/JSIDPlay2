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
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.xml.bind.JAXBContext;

import org.apache.http.HttpHeaders;
import org.apache.tomcat.util.http.fileupload.FileItemIterator;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;

import com.beust.jcommander.JCommander;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidutils.PathUtils;
import libsidutils.ZipFileUtils;
import net.java.truevfs.access.TFile;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public abstract class JSIDPlay2Servlet extends HttpServlet {

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

	private String[] getRequestParameters(HttpServletRequest request) {
		return concat(Collections.list(request.getParameterNames()).stream()
				.flatMap(name -> asList(request.getParameterValues(name)).stream().filter(v -> !"null".equals(v))
						.map(v -> of((name.length() > 1 ? "--" : "-") + name, v)))
				.flatMap(Function.identity()),
				Optional.ofNullable(request.getPathInfo()).map(Stream::of).orElse(empty())).toArray(String[]::new);
	}

	@SuppressWarnings("unchecked")
	protected <T> T getInput(HttpServletRequest request, Class<T> tClass) throws IOException {
		try (ServletInputStream inputStream = request.getInputStream()) {
			String contentType = request.getContentType();
			if (contentType == null || MIME_TYPE_JSON.isCompatible(contentType)) {
				return new ObjectMapper().readValue(inputStream, tClass);
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
				new ObjectMapper().writeValue(out, result);
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
		try (PrintStream out = new PrintStream(response.getOutputStream(), true, ct.getCharset().toString())) {
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

	protected File getAbsoluteFile(String path, boolean adminRole) throws FileNotFoundException {
		if (path == null) {
			return null;
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

	private String thread() {
		StringBuilder result = new StringBuilder();
		result.append(Thread.currentThread().getName());
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

}
