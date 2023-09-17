package server.restful.common;

import static org.apache.http.entity.ContentType.create;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

import org.apache.http.entity.ContentType;

public enum ContentTypeAndFileExtensions {
	/**
	 * Picture formats
	 */
	MIME_TYPE_JPG(create("image/jpeg", (Charset) null), false, true, ".jpg", ".jpeg"),
	MIME_TYPE_ICO(create("image/vnd.microsoft.icon", (Charset) null), false, true, ".ico"),
	/**
	 * Audio formats
	 */
	MIME_TYPE_MPEG(create("audio/mpeg", (Charset) null), false, true, ".mpg", ".mpeg", ".mp3"),
	MIME_TYPE_WAV(create("audio/wav", (Charset) null), false, true, ".wav"),
	MIME_TYPE_FLAC(create("audio/flac", (Charset) null), false, true, ".flac"),
	MIME_TYPE_AAC(create("audio/aac", (Charset) null), false, true, ".aac"),
	MIME_TYPE_SID(create("audio/prs.sid", (Charset) null), false, true, ".sid"),
	/**
	 * Video formats
	 */
	MIME_TYPE_FLV(create("video/x-flv", (Charset) null), false, true, ".flv", ".f4v"),
	MIME_TYPE_AVI(create("video/msvideo", (Charset) null), false, true, ".avi"),
	MIME_TYPE_MP4(create("video/mp4", (Charset) null), false, true, ".mp4"),
	/**
	 * Binary format
	 */
	MIME_TYPE_OCTET_STREAM(create("application/octet-stream", (Charset) null), false, true, ".bin"),
	/**
	 * Text formats
	 */
	MIME_TYPE_TEXT(create("text/plain", StandardCharsets.UTF_8), true, false, ".txt"),
	MIME_TYPE_CSV(create("text/csv", (Charset) null), true, false, ".csv"),
	/**
	 * Json
	 */
	MIME_TYPE_JSON(create("application/json", StandardCharsets.UTF_8), true, false, ".json"),
	/**
	 * Xml
	 */
	MIME_TYPE_XML(create("application/xml", StandardCharsets.UTF_8), true, false, ".xml"),
	/**
	 * Html
	 */
	MIME_TYPE_HTML(create("text/html", StandardCharsets.UTF_8), true, false, ".html", ".vue"),
	/**
	 * Javascript
	 */
	MIME_TYPE_JAVASCRIPT(create("application/javascript", StandardCharsets.UTF_8), true, true, ".js"),
	/**
	 * CSS
	 */
	MIME_TYPE_SCSS(create("text/css", StandardCharsets.UTF_8), true, true, ".scss", ".css"),
	/**
	 * FONTS
	 */
	MIME_TYPE_WOFF2(create("font/woff2", (Charset) null), false, true, ".woff2"),
	MIME_TYPE_WOFF(create("font/woff", (Charset) null), false, true, ".woff");

	private final ContentType contentType;
	private boolean isText, isCacheable;
	private final String[] extensions;

	private ContentTypeAndFileExtensions(ContentType contentType, boolean isText, boolean isCacheable,
			String... extensions) {
		this.contentType = contentType;
		this.isText = isText;
		this.isCacheable = isCacheable;
		this.extensions = extensions;
	}

	public String[] getExtensions() {
		return extensions;
	}

	public String getMimeType() {
		return contentType.getMimeType();
	}

	public Charset getCharset() {
		return contentType.getCharset();
	}

	public boolean isText() {
		return isText;
	}

	public boolean isCacheable() {
		return isCacheable;
	}

	@Override
	public String toString() {
		return contentType.toString();
	}

	public boolean isCompatible(String headerValue) {
		ContentType otherContentType = ContentType.parse(headerValue);
		if (otherContentType.getCharset() != null) {
			return Objects.equals(getCharset(), otherContentType.getCharset())
					&& Objects.equals(getMimeType(), otherContentType.getMimeType());
		} else {
			return Objects.equals(getMimeType(), otherContentType.getMimeType());
		}
	}

	public static ContentTypeAndFileExtensions getMimeType(String extension) {
		return Arrays.asList(values()).stream()
				.filter(ct -> extension != null
						&& Arrays.asList(ct.getExtensions()).contains(extension.toLowerCase(Locale.ENGLISH)))
				.findFirst().orElse(MIME_TYPE_OCTET_STREAM);
	}

}
