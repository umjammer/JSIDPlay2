package server.restful.common.parameter.requestpath;

import java.io.InputStream;

import com.beust.jcommander.Parameter;

import server.restful.common.converter.WebResourceConverter;

/**
 * Web resource.
 * 
 * @author ken
 *
 */
public class WebResourceRequestPathServletParameters {

	private InputStream resource;

	public InputStream getResource() {
		return resource;
	}

	@Parameter(descriptionKey = "RESOURCE", converter = WebResourceConverter.class)
	public void setResource(InputStream resource) {
		this.resource = resource;
	}

}