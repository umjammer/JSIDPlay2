package client.teavm.config;

import libsidplay.config.IPrinterSection;

public final class JavaScriptPrinterSection implements IPrinterSection {

	@Override
	public void setPrinterOn(boolean on) {
	}

	@Override
	public boolean isPrinterOn() {
		return false;
	}
}