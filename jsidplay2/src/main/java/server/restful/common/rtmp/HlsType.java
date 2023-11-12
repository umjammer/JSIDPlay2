package server.restful.common.rtmp;

public enum HlsType {
	VIDEO_JS("/webjars/video.js/8.5.2/dist/video$min.js", "/webjars/video.js/8.5.2/dist/video-js$min.css"),
	HLS_JS("/webjars/hls.js/1.4.12/dist/hls$min.js", "");

	private String script;
	private String style;

	private HlsType(String script, String style) {
		this.script = script;
		this.style = style;
	}

	public String getScript() {
		return script;
	}

	public String getStyle() {
		return style;
	}
}