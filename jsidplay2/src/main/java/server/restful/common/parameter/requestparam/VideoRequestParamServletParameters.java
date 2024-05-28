package server.restful.common.parameter.requestparam;

import java.util.UUID;

import com.beust.jcommander.Parameter;

import server.restful.common.converter.UUIDConverter;

/**
 * Video stream identified by UUID.
 * 
 * @author ken
 *
 */
public class VideoRequestParamServletParameters {

	private Boolean help = Boolean.FALSE;

	public Boolean getHelp() {
		return help;
	}

	@Parameter(names = { "--help", "-h" }, arity = 1, descriptionKey = "USAGE", help = true, order = Integer.MIN_VALUE)
	public void setHelp(Boolean help) {
		this.help = help;
	}

	private UUID uuid;

	public UUID getUuid() {
		return uuid;
	}

	@Parameter(names = {
			"--name" }, descriptionKey = "NAME", converter = UUIDConverter.class, required = true, order = Integer.MIN_VALUE
					+ 1)
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

}