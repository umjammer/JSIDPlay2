package server.restful.common;

import java.io.InputStream;

import com.beust.jcommander.Parameter;

import server.restful.common.converter.WebResourceConverter;

public abstract class RequestPathServletParameters {

	/**
	 * Assemble64 file (itemId, categoryId, filePath) or local file (filePath).
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

}
