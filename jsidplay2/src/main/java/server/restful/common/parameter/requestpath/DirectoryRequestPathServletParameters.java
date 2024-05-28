package server.restful.common.parameter.requestpath;

import com.beust.jcommander.Parameter;

import server.restful.common.parameter.requestpath.impl.DirectoryRequestPathServletParametersImpl;

/**
 * Local directory name.
 * 
 * @author ken
 *
 */
public class DirectoryRequestPathServletParameters extends DirectoryRequestPathServletParametersImpl {

	private Boolean help = Boolean.FALSE;

	public Boolean getHelp() {
		return help;
	}
	
	@Parameter(names = { "--help", "-h" }, arity = 1, descriptionKey = "USAGE", help = true, order = -3)
	public void setHelp(Boolean help) {
		this.help = help;
	}
	
	private String filter = ".*\\.(sid|dat|mus|str|mp3|mp4|jpg|prg|d64)$";

	@Override
	public String getFilter() {
		return filter;
	}

	@Parameter(names = { "--filter" }, descriptionKey = "FILTER", order = -2)
	public void setFilter(String filter) {
		this.filter = filter;
	}

	private String directoryPath;

	@Override
	public String getDirectoryPath() {
		return directoryPath;
	}

	@Parameter(descriptionKey = "DIRECTORY_PATH", required = true)
	public void setDirectoryPath(String directory) {
		this.directoryPath = directory;
	}

}