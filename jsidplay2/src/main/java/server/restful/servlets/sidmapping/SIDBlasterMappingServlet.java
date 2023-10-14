package server.restful.servlets.sidmapping;

import static libsidplay.components.pla.PLA.MAX_SIDS;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.filters.RequestLogFilter.FILTER_PARAMETER_SERVLET_NAME;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.filters.RequestLogFilter;
import server.restful.common.parameter.ServletParameterParser;
import server.restful.common.parameter.requestpath.FileRequestPathServletParameters;
import sidplay.ini.IniConfig;

@SuppressWarnings("serial")
public class SIDBlasterMappingServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.sidmapping.SIDBlasterMappingServletParameters")
	public static class SIDBlasterMappingServletParameters extends FileRequestPathServletParameters {

		@ParametersDelegate
		private IEmulationSection emulationSection = new IniConfig().getEmulationSection();

	}

	public static final String SIDBLASTER_MAPPING_PATH = "/sidblaster-mapping";

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + SIDBLASTER_MAPPING_PATH;
	}

	@Override
	public List<Filter> getServletFilters() {
		return Arrays.asList(new RequestLogFilter());
	}

	@Override
	public Map<String, String> getServletFiltersParameterMap() {
		Map<String, String> result = new HashMap<>();
		result.put(FILTER_PARAMETER_SERVLET_NAME, getClass().getSimpleName());
		return result;
	}

	@Override
	public boolean isSecured() {
		return true;
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		try {
			final SIDBlasterMappingServletParameters servletParameters = new SIDBlasterMappingServletParameters();

			ServletParameterParser parser = new ServletParameterParser(request, response, servletParameters,
					getServletPath());

			final File file = servletParameters.fetchFile(this, parser, request.isUserInRole(ROLE_ADMIN));
			if (file == null || parser.hasException()) {
				parser.usage();
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
