package server.restful.common;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static org.apache.http.HttpHeaders.ACCEPT;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_XML;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;

import javax.xml.bind.JAXBContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.servlet.ServletInputStream;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
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

	public Map<String, String> getServletFiltersParameterMap() {
		return Collections.emptyMap();
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
			} else /* file upload */ {
				ByteArrayOutputStream result = new ByteArrayOutputStream();
				for (Part part : request.getParts()) {
					try (InputStream itemInputStream = part.getInputStream()) {
						IOUtils.copy(itemInputStream, result);
					}
					Constructor<T> constructor = tClass.getConstructor(new Class[] { byte[].class });
					return constructor.newInstance(result.toByteArray());
				}
			}
			return null;
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
			ServletUtil.error(getServletContext(), e);
		}
	}

	protected void setOutput(HttpServletResponse response, ContentTypeAndFileExtensions ct, Throwable e)
			throws IOException {
		setOutput(response, ct, e.getClass().getSimpleName() + ": " + e.getMessage());
	}

	protected void setOutput(HttpServletResponse response, ContentTypeAndFileExtensions ct, String message)
			throws JsonProcessingException, IOException {
		response.setContentType(ct.toString());
		try (PrintStream out = new PrintStream(response.getOutputStream(), true,
				ofNullable(ct.getCharset()).map(Charset::toString).orElse(StandardCharsets.UTF_8.name()))) {
			out.print(message);
		}
	}

}
