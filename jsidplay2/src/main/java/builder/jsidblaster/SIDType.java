package builder.jsidblaster;

import libsidplay.common.ChipModel;

public enum SIDType {
	NONE, MOS6581, MOS8580;

	public ChipModel asChipModel() {
		switch (this) {
		case NONE:
		default:
			return ChipModel.AUTO;
		case MOS6581:
			return ChipModel.MOS6581;
		case MOS8580:
			return ChipModel.MOS8580;
		}
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