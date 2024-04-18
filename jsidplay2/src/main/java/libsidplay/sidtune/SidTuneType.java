package libsidplay.sidtune;

public enum SidTuneType {
	PSID, PRG, P00, T64;

	public static SidTuneType get(String name) {
		if (name.toLowerCase().endsWith(".sid")) {
			return SidTuneType.PSID;
		} else if (name.toLowerCase().endsWith(".prg")) {
			return SidTuneType.PRG;
		} else if (name.toLowerCase().endsWith(".p00")) {
			return SidTuneType.P00;
		} else if (name.toLowerCase().endsWith(".t64")) {
			return SidTuneType.T64;
		} else {
			return SidTuneType.PSID;
		}
	}

}
