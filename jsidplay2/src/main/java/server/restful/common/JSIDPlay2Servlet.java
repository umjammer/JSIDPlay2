package server.restful.common;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Stream.of;
import static org.apache.http.HttpHeaders.ACCEPT;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_XML;
import static server.restful.common.IServletSystemProperties.UNCAUGHT_EXCEPTION_HANDLER_EXCEPTIONS;
import static server.restful.common.IServletSystemProperties.UPLOAD_FILE_SIZE_MAX;
import static server.restful.common.IServletSystemProperties.UPLOAD_SIZE_MAX;

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

import javax.xml.bind.JAXBContext;

import org.apache.tomcat.util.http.fileupload.FileItemIterator;
import org.apache.tomcat.util.http.fileupload.servlet.ServletFileUpload;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.ServletSecurity.EmptyRoleSemantic;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.IOUtils;
import libsidutils.siddatabase.SidDatabase;
import libsidutils.stil.STIL;
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

	protected SidDatabase sidDatabase;

	protected STIL stil;

	protected Properties directoryProperties;

	public Configuration getConfiguration() {
		return configuration;
	}

	public void setConfiguration(Configuration configuration) {
		this.configuration = configuration;
	}

	public Properties getDirectoryProperties() {
		return directoryProperties;
	}

	public void setDirectoryProperties(Properties directoryProperties) {
		this.directoryProperties = directoryProperties;
	}

	public void setSidDatabase(SidDatabase sidDatabase) {
		this.sidDatabase = sidDatabase;
	}

	public void setStil(STIL stil) {
		this.stil = stil;
	}

	public boolean isSecured() {
		ServletSecurity servletSecurity = getClass().getAnnotation(ServletSecurity.class);
		HttpConstraint httpConstraint = servletSecurity != null ? servletSecurity.value() : null;

		return httpConstraint != null
				&& (httpConstraint.rolesAllowed().length > 0 || EmptyRoleSemantic.DENY.equals(httpConstraint.value()));
	}

	public List<Filter> getServletFilters() {
		return Collections.emptyList();
	}

	public Map<String, String> getServletFiltersParameterMap() {
		return Collections.emptyMap();
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
		result.append(" (");
		result.append(thread.getId());
		result.append(")");
		result.append(": ");
		return result.toString();
	}

	protected String getCollectionName(File file) throws IOException, SidTuneError {
		String result = "";

		File hvscRoot = configuration.getSidplay2Section().getHvsc();
		if (hvscRoot != null) {
			// 1st Try from full path name...
			result = IOUtils.getCollectionName(hvscRoot, file);
			if (result.isEmpty()) {
				// ... then try from MD5
				result = sidDatabase != null ? sidDatabase.getPath(SidTune.load(file)) : "";
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
				ServletFileUpload servletFileUpload = new ServletFileUpload();
				servletFileUpload.setSizeMax(UPLOAD_SIZE_MAX);
				servletFileUpload.setFileSizeMax(UPLOAD_FILE_SIZE_MAX);
				FileItemIterator itemIterator = servletFileUpload.getItemIterator(request);
				while (itemIterator.hasNext()) {
					try (InputStream itemInputStream = itemIterator.next().openStream()) {
						IOUtils.copy(itemInputStream, result);
					}
					// just the first file
					break;
				}
				Constructor<T> constructor = tClass.getConstructor(new Class[] { byte[].class });
				return constructor.newInstance(result.toByteArray());
			} else {
				throw new IOException("Unsupported content type: " + contentType);
			}
		} catch (Exception e) {
			throw new IOException(e);
		}
	}

	protected <T> void setOutput(HttpServletRequest request, HttpServletResponse response, T result, Class<T> tClass) {
		if (response != null) {
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
				error(e);
			}
		}
	}

	protected void setOutput(HttpServletResponse response, ContentTypeAndFileExtensions ct, Throwable e)
			throws IOException {
		setOutput(response, ct, e.getClass().getSimpleName() + ": " + e.getMessage());
	}

	protected void setOutput(HttpServletResponse response, ContentTypeAndFileExtensions ct, String message)
			throws JsonProcessingException, IOException {
		if (response != null) {
			response.setContentType(ct.toString());
			try (PrintStream out = new PrintStream(response.getOutputStream(), true,
					ofNullable(ct.getCharset()).map(Charset::toString).orElse(StandardCharsets.UTF_8.name()))) {
				out.print(message);
			}
		}
	}

}
