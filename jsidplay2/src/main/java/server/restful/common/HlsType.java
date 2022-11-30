package server.restful.common;

public enum HlsType {
	VIDEO_JS("/static/video.js@7.19.2/dist/video.min.js"), HLS_JS("/static/hls.js/dist/hls.min.js");

	private String script;

	private HlsType(String script) {
		this.script = script;
	}

	public String getScript() {
		return script;
	}
}