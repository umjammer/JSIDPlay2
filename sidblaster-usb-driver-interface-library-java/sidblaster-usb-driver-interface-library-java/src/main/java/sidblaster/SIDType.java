package sidblaster;

public enum SIDType {
	NONE(""), MOS6581("6581"), MOS8580("8580");

	private String sidType;

	private SIDType(String sidType) {
		this.sidType = sidType;
	}

	public String getSidType() {
		return sidType;
	}
}