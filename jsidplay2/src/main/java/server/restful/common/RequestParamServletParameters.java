package server.restful.common;

import java.util.UUID;

import com.beust.jcommander.Parameter;

import server.restful.common.converter.UUIDConverter;

public abstract class RequestParamServletParameters {

	/**
	 * Video stream identified by UUID.
	 * 
	 * @author ken
	 *
	 */
	public static class VideoRequestParamServletParameters {

		private UUID uuid;

		public UUID getUuid() {
			return uuid;
		}

		@Parameter(names = {
				"--name" }, descriptionKey = "NAME", converter = UUIDConverter.class, required = true, order = Integer.MIN_VALUE)
		public void setUuid(UUID uuid) {
			this.uuid = uuid;
		}

	}
}
