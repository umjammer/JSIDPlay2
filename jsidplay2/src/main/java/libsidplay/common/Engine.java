package libsidplay.common;

import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.MP3Tune;
import libsidplay.sidtune.SidTune;

public enum Engine {
	/** Software (emulation using RESID or RESIDfp) */
	EMULATION,
	/** Software (Network SID Device via socket connection) */
	NETSID,
	/** Hardware (HardSID4U, HardSID Uno and HardSID UPlay - USB devices) */
	HARDSID,
	/** Hardware (SIDBlaster - USB device) */
	SIDBLASTER,
	/** Hardware (ExSID, ExSID+ - USB devices) */
	EXSID;

	/**
	 * Choose engine to be used (MP3 requires EMULATION).
	 * 
	 * @return engine to be used
	 */
	public static Engine getEngine(IEmulationSection emulationSection, SidTune tune) {
		if (tune instanceof MP3Tune) {
			return EMULATION;
		}
		return emulationSection.getEngine();
	}
}
