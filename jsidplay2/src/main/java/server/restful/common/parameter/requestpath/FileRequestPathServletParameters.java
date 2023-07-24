package server.restful.common.parameter.requestpath;

import com.beust.jcommander.Parameter;

/**
 * Local file (filePath) or an Assemble64 file (filePath, itemId, categoryId).
 * 
 * @author ken
 *
 */
public class FileRequestPathServletParameters implements IFileRequestPathServletParameters {

	private String itemId;

	@Override
	public String getItemId() {
		return itemId;
	}

	@Parameter(names = { "--itemId" }, descriptionKey = "ITEM_ID", order = Integer.MIN_VALUE)
	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	private String categoryId;

	@Override
	public String getCategoryId() {
		return categoryId;
	}

	@Parameter(names = { "--categoryId" }, descriptionKey = "CATEGORY_ID", order = Integer.MIN_VALUE + 1)
	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
	}

	private String filePath;

	@Override
	public String getFilePath() {
		return filePath;
	}

	@Parameter(descriptionKey = "FILE_PATH")
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

}