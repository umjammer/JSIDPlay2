package server.restful.servlets.sidmapping;

import static libsidplay.components.pla.PLA.MAX_SIDS;
import static server.restful.JSIDPlay2Server.CONTEXT_ROOT_SERVLET;
import static server.restful.JSIDPlay2Server.ROLE_ADMIN;
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
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import libsidplay.common.ChipModel;
import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;
import server.restful.common.JSIDPlay2Servlet;
import server.restful.common.parameter.RequestPathServletParameters.FileRequestPathServletParameters;
import sidplay.ini.IniConfig;
import ui.entities.config.Configuration;

@SuppressWarnings("serial")
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

	public static final String HARDSID_MAPPING_PATH = "/hardsid-mapping";

	public HardSIDMappingServlet(Configuration configuration, Properties directoryProperties) {
		super(configuration, directoryProperties);
	}

	@Override
	public String getServletPath() {
		return CONTEXT_ROOT_SERVLET + HARDSID_MAPPING_PATH;
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
			final HardSIDMappingServletParameters servletParameters = new HardSIDMappingServletParameters();

			JCommander commander = parseRequestParameters(request, response, servletParameters, getServletPath());

			final File file = getFile(commander, servletParameters, request.isUserInRole(ROLE_ADMIN));
			if (file == null) {
				commander.usage();
				return;
			}
			final IEmulationSection emulationSection = servletParameters.emulationSection;
			final Integer chipCount = servletParameters.chipCount;

			SidTune tune = SidTune.load(file);

			Set<Integer> alreadyInUse = new HashSet<>();
			Map<Integer, Integer> result = new HashMap<>();
			for (int sidNum = 0; sidNum < MAX_SIDS; sidNum++) {
				if (SidTune.isSIDUsed(emulationSection, tune, sidNum)) {

					int address = SidTune.getSIDAddress(emulationSection, tune, sidNum);

					ChipModel chipModel = ChipModel.getChipModel(emulationSection, tune, sidNum);

					Integer chipNum = getModelDependantChipNum(emulationSection, chipCount, alreadyInUse, chipModel);
					result.put(address, chipNum);
					alreadyInUse.add(chipNum);
				}
			}
			setOutput(response, MIME_TYPE_JSON, OBJECT_MAPPER.writer().writeValueAsString(result));

		} catch (Throwable t) {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			error(t);
			setOutput(response, MIME_TYPE_TEXT, t);
		}
	}

	/**
	 * Get HardSID device index based on the desired chip model.
	 * 
	 * @param emulationSection configuration
	 * @param chipCount        number of available devices
	 * @param alreadyInUse     devices already in use
	 * @param chipModel        desired chip model
	 * 
	 * @return SID index of the desired HardSID device
	 */
	private Integer getModelDependantChipNum(IEmulationSection emulationSection, final int chipCount,
			Set<Integer> alreadyInUse, final ChipModel chipModel) {
		int sid6581 = emulationSection.getHardsid6581();
		int sid8580 = emulationSection.getHardsid8580();

		// use next free slot (prevent wrong type)
		for (int chipNum = 0; chipNum < chipCount; chipNum++) {
			if (!isChipNumAlreadyUsed(alreadyInUse, chipNum)
					&& isChipModelMatching(emulationSection, chipModel, chipNum)) {
				return chipNum;
			}
		}
		// Nothing matched? use next free slot
		for (int chipNum = 0; chipNum < chipCount; chipNum++) {
			if (chipCount > 2 && (chipNum == sid6581 || chipNum == sid8580)) {
				// more SIDs available than configured? still skip wrong type
				continue;
			}
			if (!isChipNumAlreadyUsed(alreadyInUse, chipNum)) {
				return chipNum;
			}
		}
		// no slot left
		return null;
	}

	private boolean isChipModelMatching(IEmulationSection emulationSection, final ChipModel chipModel, int chipNum) {
		int sid6581 = emulationSection.getHardsid6581();
		int sid8580 = emulationSection.getHardsid8580();

		return chipNum == sid6581 && chipModel == ChipModel.MOS6581
				|| chipNum == sid8580 && chipModel == ChipModel.MOS8580;
	}

	private boolean isChipNumAlreadyUsed(Set<Integer> alreadyInUse, final int chipNum) {
		return alreadyInUse.contains(chipNum);
	}

}
