package libsidplay.common;

public enum SidReads {
	FIRST_SID(0), SECOND_SID(1), THIRD_SID(2);

	private int sidNum;

	private SidReads(int sidNum) {
		this.sidNum = sidNum;
	}

	public int getSidNum() {
		return sidNum;
	}
}
