package server.restful.common;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

import sidplay.ini.IniConfig;

@Parameters(resourceBundle = "server.restful.common.ServletParameters")
public class ServletParameters {

	@Parameter(names = { "--startSong" }, descriptionKey = "START_SONG")
	private Integer song = null;

	@Parameter(names = "--download", arity = 1, descriptionKey = "DOWNLOAD")
	private Boolean download = Boolean.FALSE;

	@Parameter(names = "--jiffydos", arity = 1, descriptionKey = "JIFFYDOS")
	private Boolean jiffydos = Boolean.FALSE;

	@Parameter(names = { "--reuSize" }, descriptionKey = "REU_SIZE")
	private Integer reuSize = null;

	@ParametersDelegate
	private IniConfig config = new IniConfig();

	private volatile boolean started;

	public Integer getSong() {
		return song;
	}

	public Boolean getDownload() {
		return download;
	}

	public Boolean getJiffydos() {
		return jiffydos;
	}

	public Integer getReuSize() {
		return reuSize;
	}

	public IniConfig getConfig() {
		return config;
	}

	public boolean isStarted() {
		return started;
	}

	public void setStarted(boolean started) {
		this.started = started;
	}
}
