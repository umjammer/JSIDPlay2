package ui.oscilloscope;

import javafx.fxml.FXML;
import javafx.scene.control.TitledPane;
import javafx.scene.image.ImageView;
import libsidplay.common.SIDEmu;
import sidplay.Player;
import ui.common.C64Window;

public final class FilterGauge extends SIDGauge {

	@FXML
	private TitledPane border;
	@FXML
	private ImageView area;

	public FilterGauge() {
	}

	public FilterGauge(C64Window window, Player player) {
		super(window, player);
	}

	@Override
	protected ImageView getArea() {
		return area;
	}

	@Override
	protected TitledPane getTitledPane() {
		return border;
	}

	@Override
	public SIDGauge sample(SIDEmu sidemu) {
		int fc = sidemu.readInternalRegister(0x15) & 7;
		fc |= (sidemu.readInternalRegister(0x16) & 0xff) << 3;
		accumulate(fc / 2047.0f);
		return this;
	}

	@Override
	public void addImage(SIDEmu sidemu) {
		if (sidemu != null) {
			final byte vol = sidemu.readInternalRegister(0x18);
			setText(createText(vol));
		}
		super.addImage(sidemu);
	}

	private String createText(final byte vol) {
		StringBuilder result = new StringBuilder();
		result.append(localizer.getString("FILTER"));
		result.append(" ");
		if ((vol & 0x10) != 0) {
			result.append("L"); // LP
		}
		if ((vol & 0x20) != 0) {
			result.append("B"); // BP
		}
		if ((vol & 0x40) != 0) {
			result.append("H"); // HP
		}
		return result.toString();
	}
}