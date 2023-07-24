package server.restful.servlets.sidmapping;

import static libsidplay.components.pla.PLA.MAX_SIDS;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.parameter.requestpath.FileRequestPathServletParameters;
import sidplay.ini.IniConfig;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class SIDBlasterMappingServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.sidmapping.SIDBlasterMappingServletParameters")
	public static class SIDBlasterMappingServletParameters extends FileRequestPathServletParameters {

		@ParametersDelegate
		private IEmulationSection emulationSection = new IniConfig().getEmulationSection();

	}

	public static final String SIDBLASTER_MAPPING_PATH = "/sidblaster-mapping";

	public SIDBlasterMappingServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + SIDBLASTER_MAPPING_PATH;
	}

	@Override
	public boolean isSecured() {
		return true;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		super.doGet(request);
		try {
			final SIDBlasterMappingServletParameters servletParameters = new SIDBlasterMappingServletParameters();

			JCommander commander = parseRequestParameters(request, response, servletParameters, getServletPath());

			final File file = servletParameters.getFile(this, commander, request);
			if (file == null) {
				commander.usage();
				return;
			}
			final IEmulationSection emulationSection = servletParameters.emulationSection;

			SidTune tune = SidTune.load(file);

			Map<Integer, String> result = new HashMap<>();
			for (int sidNum = 0; sidNum < MAX_SIDS; sidNum++) {
				if (SidTune.isSIDUsed(emulationSection, tune, sidNum)) {

					int address = SidTune.getSIDAddress(emulationSection, tune, sidNum);
					// base address
					result.put(address, String.valueOf(sidNum));
					break;
				}
			}
			setOutput(response, MIME_TYPE_JSON, OBJECT_MAPPER.writer().writeValueAsString(result));

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

}
