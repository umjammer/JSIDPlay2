package server.restful.common;

import com.beust.jcommander.Parameter;

public class ServletBaseParameters {

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
