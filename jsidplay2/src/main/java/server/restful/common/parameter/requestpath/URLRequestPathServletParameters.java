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

	private URL url;

	public URL getUrl() {
		return url;
	}

	@Parameter(descriptionKey = "URL", converter = RequestPathURLConverter.class, required = true)
	public void setUrl(URL url) {
		this.url = url;
	}

}