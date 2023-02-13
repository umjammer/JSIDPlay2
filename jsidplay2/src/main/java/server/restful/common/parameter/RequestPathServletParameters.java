package server.restful.common.parameter;

import java.io.InputStream;
import java.net.URL;

import com.beust.jcommander.Parameter;

import server.restful.common.converter.RequestPathURLConverter;
import server.restful.common.converter.WebResourceConverter;

/**
 * <B>Note:</B> Main parameters have been annotated on field level on purpose to
 * support a better usage message.
 * 
 * @author ken
 *
 */
public abstract class RequestPathServletParameters {

	/**
	 * Local file (filePath) or an Assemble64 file (filePath, itemId, categoryId).
	 * 
	 * @author ken
	 *
	 */
	public static class FileRequestPathServletParameters extends RequestPathServletParameters {

		private String itemId;

		public String getItemId() {
			return itemId;
		}

		@Parameter(names = { "--itemId" }, descriptionKey = "ITEM_ID", order = Integer.MIN_VALUE)
		public void setItemId(String itemId) {
			this.itemId = itemId;
		}

		private String categoryId;

		public String getCategoryId() {
			return categoryId;
		}

		@Parameter(names = { "--categoryId" }, descriptionKey = "CATEGORY_ID", order = Integer.MIN_VALUE + 1)
		public void setCategoryId(String categoryId) {
			this.categoryId = categoryId;
		}

		@Parameter(descriptionKey = "FILE_PATH")
		private String filePath;

		public String getFilePath() {
			return filePath;
		}

		public void setFilePath(String filePath) {
			this.filePath = filePath;
		}

	}

	/**
	 * Local directory name.
	 * 
	 * @author ken
	 *
	 */
	public static class DirectoryRequestPathServletParameters extends RequestPathServletParameters {

		@Parameter(descriptionKey = "DIRECTORY")
		private String directory;

		public String getDirectory() {
			return directory;
		}

		public void setDirectory(String directory) {
			this.directory = directory;
		}

	}

	/**
	 * Web resource.
	 * 
	 * @author ken
	 *
	 */
	public static class WebResourceRequestPathServletParameters extends RequestPathServletParameters {

		@Parameter(descriptionKey = "RESOURCE", converter = WebResourceConverter.class)
		private InputStream resource;

		public InputStream getResource() {
			return resource;
		}

		public void setResource(InputStream resource) {
			this.resource = resource;
		}

	}

	/**
	 * URL.
	 * 
	 * @author ken
	 *
	 */
	public static class URLRequestPathServletParameters extends RequestPathServletParameters {

		@Parameter(descriptionKey = "URL", converter = RequestPathURLConverter.class, required = true)
		private URL url;

		public URL getUrl() {
			return url;
		}

		public void setUrl(URL url) {
			this.url = url;
		}

	}

}
