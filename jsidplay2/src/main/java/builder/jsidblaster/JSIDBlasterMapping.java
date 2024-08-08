package builder.jsidblaster;

import static libsidplay.components.pla.PLA.MAX_SIDS;

import java.util.HashMap;
import java.util.Map;

import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;

public class JSIDBlasterMapping {
	public static Map<Integer, String> mapping(IEmulationSection emulationSection, SidTune tune) {
		Map<Integer, String> result = new HashMap<>();
		for (int sidNum = 0; sidNum < MAX_SIDS; sidNum++) {
			if (SidTune.isSIDUsed(emulationSection, tune, sidNum)) {

				int address = SidTune.getSIDAddress(emulationSection, tune, sidNum);
				// base address
				result.put(address, String.valueOf(sidNum));
				break;
			}
		}
		return result;
	}

}
