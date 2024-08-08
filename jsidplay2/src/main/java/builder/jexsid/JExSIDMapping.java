package builder.jexsid;

import static libsidplay.components.pla.PLA.MAX_SIDS;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;

public class JExSIDMapping {

	public static Map<Integer, String> mapping(IEmulationSection emulationSection, SidTune tune) {
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
		result.put(-2, String
				.valueOf(emulationSection.isExsidFakeStereo() && SidTune.isFakeStereoSid(emulationSection, tune, 1)));
		// CPUClock
		result.put(-3, cpuClock.name());
		return result;
	}

	private static boolean isChipNumAlreadyUsed(Set<ChipModel> alreadyInUse, final ChipModel chipModel) {
		return alreadyInUse.contains(chipModel);
	}

}
