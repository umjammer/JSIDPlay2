package server.restful.common.parameter.requestpath;

import com.beust.jcommander.Parameter;

import server.restful.common.parameter.requestpath.impl.FileRequestPathServletParametersImpl;

/**
 * Local file (filePath) or an Assemble64 file (filePath, itemId, categoryId).
 * 
 * @author ken
 *
 */
public class FileRequestPathServletParameters extends FileRequestPathServletParametersImpl {

	@Parameter(names = { "--help", "-h" }, arity = 1, descriptionKey = "USAGE", help = true, order = Integer.MIN_VALUE)
	private Boolean help = Boolean.FALSE;
	
	public Boolean getHelp() {
		return help;
	}

	public void setHelp(Boolean help) {
		this.help = help;
	}
	
	private String itemId;

	@Override
	public String getItemId() {
		return itemId;
	}

	@Parameter(names = { "--itemId" }, descriptionKey = "ITEM_ID", order = Integer.MIN_VALUE + 1)
	public void setItemId(String itemId) {
		this.itemId = itemId;
	}

	private String categoryId;

	@Override
	public String getCategoryId() {
		return categoryId;
	}

	@Parameter(names = { "--categoryId" }, descriptionKey = "CATEGORY_ID", order = Integer.MIN_VALUE + 3)
	public void setCategoryId(String categoryId) {
		this.categoryId = categoryId;
	}

	private String filePath;

	@Override
	public String getFilePath() {
		return filePath;
	}

	@Parameter(descriptionKey = "FILE_PATH", required = true)
	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

}