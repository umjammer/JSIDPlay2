package server.restful.common.parameter.requestpath;

import com.beust.jcommander.Parameter;

/**
 * Local directory name.
 * 
 * @author ken
 *
 */
public class DirectoryRequestPathServletParameters implements IDirectoryRequestPathServletParameters {

	private String directoryPath;

	@Override
	public String getDirectoryPath() {
		return directoryPath;
	}

	@Parameter(descriptionKey = "DIRECTORY_PATH")
	public void setDirectoryPath(String directory) {
		this.directoryPath = directory;
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

}