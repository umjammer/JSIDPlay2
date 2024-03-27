package libsidplay.sidtune;

public enum SidTuneType {
	PSID(0, ".sid"), PRG(1, ".prg"), P00(2, ".p00"), T64(3, ".t64");

	private int typeNum;
	private String fileExtension;

	private SidTuneType(int typeNum, String fileExtension) {
		this.typeNum = typeNum;
		this.fileExtension = fileExtension;
	}

	public int getTypeNum() {
		return typeNum;
	}

	public String getFileExtension() {
		return fileExtension;
	}

	public static SidTuneType get(int typeNum) {
		switch (typeNum) {
		case 0:
		default:
			return PSID;
		case 1:
			return PRG;
		case 2:
			return P00;
		case 3:
			return T64;
		}
	}
}
