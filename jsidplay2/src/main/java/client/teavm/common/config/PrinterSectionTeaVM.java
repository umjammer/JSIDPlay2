package client.teavm.common.config;

import libsidplay.config.IPrinterSection;

public final class PrinterSectionTeaVM implements IPrinterSection {

	@Override
	public void setPrinterOn(boolean on) {
	}

	@Override
	public boolean isPrinterOn() {
		return false;
	}
}