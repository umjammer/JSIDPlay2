package server.restful.common.parameter.requestpath;

import java.net.URL;

import com.beust.jcommander.Parameter;

import server.restful.common.converter.RequestPathURLConverter;

/**
 * URL.
 * 
 * @author ken
 *
 */
public class URLRequestPathServletParameters {

	private Boolean help = Boolean.FALSE;

	public Boolean getHelp() {
		return help;
	}

	@Parameter(names = { "--help", "-h" }, arity = 1, descriptionKey = "USAGE", help = true, order = Integer.MIN_VALUE)
	public void setHelp(Boolean help) {
		this.help = help;
	}

	private URL url;

	public URL getUrl() {
		return url;
	}

	@Parameter(descriptionKey = "URL", converter = RequestPathURLConverter.class, required = true)
	public void setUrl(URL url) {
		this.url = url;
	}

}