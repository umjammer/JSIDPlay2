package builder.jhardsid;

import static libsidplay.components.pla.PLA.MAX_SIDS;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import libsidplay.common.ChipModel;
import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;

public class JHardSIDMapping {

	public static Map<Integer, Integer> mapping(IEmulationSection emulationSection, SidTune tune, int chipCount) {
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
		return result;
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
	private static Integer getModelDependantChipNum(IEmulationSection emulationSection, final int chipCount,
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

	private static boolean isChipModelMatching(IEmulationSection emulationSection, final ChipModel chipModel,
			int chipNum) {
		int sid6581 = emulationSection.getHardsid6581();
		int sid8580 = emulationSection.getHardsid8580();

		return chipNum == sid6581 && chipModel == ChipModel.MOS6581
				|| chipNum == sid8580 && chipModel == ChipModel.MOS8580;
	}

	private static boolean isChipNumAlreadyUsed(Set<Integer> alreadyInUse, final int chipNum) {
		return alreadyInUse.contains(chipNum);
	}

}
