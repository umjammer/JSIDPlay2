package server.restful.servlets.sidmapping;

import static libsidplay.components.pla.PLA.MAX_SIDS;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_JSON;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.parameter.ServletUsageFormatter;
import server.restful.common.parameter.requestpath.FileRequestPathServletParameters;
import sidplay.ini.IniConfig;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
public class ExSIDMappingServlet extends JSIDPlay2Servlet {

	@Parameters(resourceBundle = "server.restful.servlets.sidmapping.ExSIDMappingServletParameters")
	public static class ExSIDMappingServletParameters extends FileRequestPathServletParameters {

		@ParametersDelegate
		private IEmulationSection emulationSection = new IniConfig().getEmulationSection();

	}

	public static final String EXSID_MAPPING_PATH = "/exsid-mapping";

	public ExSIDMappingServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + EXSID_MAPPING_PATH;
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
			final ExSIDMappingServletParameters servletParameters = new ExSIDMappingServletParameters();

			JCommander commander = parseRequestParameters(request, response, servletParameters, getServletPath());

			final File file = servletParameters.getFile(this, commander, request);
			if (file == null || ((ServletUsageFormatter) commander.getUsageFormatter()).getException() != null) {
				commander.usage();
				return;
			}
			final IEmulationSection emulationSection = servletParameters.emulationSection;

			SidTune tune = SidTune.load(file);

			CPUClock cpuClock = CPUClock.getCPUClock(emulationSection, tune);

			Set<ChipModel> alreadyInUse = new HashSet<>();
			Map<Integer, String> result = new HashMap<>();
			for (int sidNum = 0; sidNum < MAX_SIDS; sidNum++) {
				if (SidTune.isSIDUsed(emulationSection, tune, sidNum)) {

					int address = SidTune.getSIDAddress(emulationSection, tune, sidNum);

					ChipModel chipModel = ChipModel.getChipModel(emulationSection, tune, sidNum);

					if (sidNum == 1 && SidTune.isFakeStereoSid(emulationSection, tune, 1)) {
						continue;
					}
					// stereo SIDs with same chipmodel must be forced to use a different device,
					// therefore:
					if (sidNum == 1 && isChipNumAlreadyUsed(alreadyInUse, chipModel)) {
						chipModel = chipModel == ChipModel.MOS6581 ? ChipModel.MOS8580 : ChipModel.MOS6581;
					}
					// chipModel
					result.put(sidNum, String.valueOf(chipModel));
					// base address
					result.put(address, String.valueOf(sidNum));
					alreadyInUse.add(chipModel);
				}
			}
			// stereo
			result.put(-1, String.valueOf(SidTune.isSIDUsed(emulationSection, tune, 1)));
			// fake-stereo
			result.put(-2, String.valueOf(
					emulationSection.isExsidFakeStereo() && SidTune.isFakeStereoSid(emulationSection, tune, 1)));
			// CPUClock
			result.put(-3, cpuClock.name());

			setOutput(response, MIME_TYPE_JSON, OBJECT_MAPPER.writer().writeValueAsString(result));

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

	private boolean isChipNumAlreadyUsed(Set<ChipModel> alreadyInUse, final ChipModel chipModel) {
		return alreadyInUse.contains(chipModel);
	}

}
