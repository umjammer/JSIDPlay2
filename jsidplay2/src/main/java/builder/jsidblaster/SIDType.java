package builder.jsidblaster;

import libsidplay.common.ChipModel;

public enum SIDType {
	NONE(""), MOS6581("6581"), MOS8580("8580");

	private String sidType;

	private SIDType(String sidType) {
		this.sidType = sidType;
	}

	public String getSidType() {
		return sidType;
	}

	public ChipModel asChipModel() {
		return ChipModel.valueOf(sidType);
	}

	public static SIDType to(sidblaster.SIDType hardSID_GetSIDType) {
		switch (hardSID_GetSIDType) {
		case MOS6581:
			return SIDType.MOS6581;
		case MOS8580:
			return SIDType.MOS8580;
		case NONE:
		default:
			return SIDType.NONE;
		}
	}

	public static sidblaster.SIDType from(SIDType sidType) {
		switch (sidType) {
		case MOS6581:
			return sidblaster.SIDType.MOS6581;
		case MOS8580:
			return sidblaster.SIDType.MOS8580;
		case NONE:
		default:
			return sidblaster.SIDType.NONE;
		}
	}

}