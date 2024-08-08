package server.restful.servlets.sidmapping;

import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.JSIDPlay2Server.ROLE_USER;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ServletUtil.error;

import java.io.File;
import java.io.IOException;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

import builder.jhardsid.JHardSIDMapping;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HttpConstraint;
import jakarta.servlet.annotation.ServletSecurity;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.parameter.requestpath.FileRequestPathServletParameters;
import sidplay.ini.IniConfig;

@SuppressWarnings("serial")
@WebServlet(name = "HardSIDMappingServlet", displayName = "HardSIDMappingServlet", urlPatterns = CONTEXT_ROOT_SERVLET
		+ "/hardsid-mapping/*", description = "Get HardSID 4U, HardSID UPlay and HardSID Uno SID mapping information")
@ServletSecurity(value = @HttpConstraint(rolesAllowed = { ROLE_USER, ROLE_ADMIN }))
public class HardSIDMappingServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.sidmapping.HardSIDMappingServletParameters")
	public static class HardSIDMappingServletParameters extends FileRequestPathServletParameters {

		private Integer chipCount;

		public Integer getChipCount() {
			return chipCount;
		}

		@Parameter(names = { "--chipCount" }, descriptionKey = "CHIP_COUNT", required = true, order = -2)
		public void setChipCount(Integer chipCount) {
			this.chipCount = chipCount;
		}

		@ParametersDelegate
		private IEmulationSection emulationSection = new IniConfig().getEmulationSection();

	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			WebServlet webServlet = getClass().getAnnotation(WebServlet.class);
			ServletSecurity servletSecurity = getClass().getAnnotation(ServletSecurity.class);

			final HardSIDMappingServletParameters servletParameters = new HardSIDMappingServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					webServlet);

			final File file = servletParameters.fetchFile(configuration, directoryProperties, parser, servletSecurity,
					request.isUserInRole(ROLE_ADMIN));
			if (file == null || servletParameters.getHelp() || parser.hasException()) {
				parser.usage();
				return;
			}
			final IEmulationSection emulationSection = servletParameters.emulationSection;
			final Integer chipCount = servletParameters.chipCount;

			SidTune tune = SidTune.load(file);

			setOutput(MIME_TYPE_JSON, response, JHardSIDMapping.mapping(emulationSection, tune, chipCount));

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(getServletContext(), t);
			setOutput(response, t);
		}
	}

}
