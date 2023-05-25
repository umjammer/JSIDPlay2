package server.restful.common.parameter;

import java.io.InputStream;
import java.net.URL;

import com.beust.jcommander.Parameter;

import server.restful.common.converter.RequestPathURLConverter;
import server.restful.common.converter.WebResourceConverter;

/**
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

		private String filePath;

		public String getFilePath() {
			return filePath;
		}

		@Parameter(descriptionKey = "FILE_PATH")
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

		private String directory;

		public String getDirectory() {
			return directory;
		}

		@Parameter(descriptionKey = "DIRECTORY")
		public void setDirectory(String directory) {
			this.directory = directory;
		}

		private String filter = ".*\\.(sid|dat|mus|str|mp3|mp4|jpg|prg|d64)$";

		public String getFilter() {
			return filter;
		}

		@Parameter(names = { "--filter" }, descriptionKey = "FILTER", order = -2)
		public void setFilter(String filter) {
			this.filter = filter;
		}
	}

	/**
	 * Web resource.
	 * 
	 * @author ken
	 *
	 */
	public static class WebResourceRequestPathServletParameters extends RequestPathServletParameters {

		private InputStream resource;

		public InputStream getResource() {
			return resource;
		}

		@Parameter(descriptionKey = "RESOURCE", converter = WebResourceConverter.class)
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

		private URL url;

		public URL getUrl() {
			return url;
		}

		@Parameter(descriptionKey = "URL", converter = RequestPathURLConverter.class, required = true)
		public void setUrl(URL url) {
			this.url = url;
		}

	}

}
