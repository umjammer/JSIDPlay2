package ui.common;

import java.io.File;

import libsidplay.components.cart.CartridgeType;

public class ConvenienceResult {

	private boolean success;

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	private File attatchedCartridge;

	public File getAttatchedCartridge() {
		return attatchedCartridge;
	}

	public void setAttatchedCartridge(File attatchedCartridge) {
		this.attatchedCartridge = attatchedCartridge;
	}

	private CartridgeType attachedCartridgeType;

	public CartridgeType getAttachedCartridgeType() {
		return attachedCartridgeType;
	}

	public void setAttachedCartridgeType(CartridgeType attachedCartridgeType) {
		this.attachedCartridgeType = attachedCartridgeType;
	}
}
