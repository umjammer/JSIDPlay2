package server.restful.common.parameter.requestpath;

import java.io.InputStream;

import com.beust.jcommander.Parameter;

import server.restful.common.converter.WebJarsResourceConverter;

/**
 * WebJar resource.
 * 
 * @author ken
 *
 */
public class WebJarsResourceRequestPathServletParameters {

	private Boolean help = Boolean.FALSE;

	public Boolean getHelp() {
		return help;
	}
	
	@Parameter(names = { "--help", "-h" }, arity = 1, descriptionKey = "USAGE", help = true, order = Integer.MIN_VALUE)
	public void setHelp(Boolean help) {
		this.help = help;
	}
	
	private InputStream resource;

	public InputStream getResource() {
		return resource;
	}

	@Parameter(descriptionKey = "RESOURCE", converter = WebJarsResourceConverter.class, required = true)
	public void setResource(InputStream resource) {
		this.resource = resource;
	}

}