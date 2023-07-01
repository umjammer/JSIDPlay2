package ui.oscilloscope;

import javafx.fxml.FXML;
import javafx.scene.control.TitledPane;
import javafx.scene.image.ImageView;
import libsidplay.common.SIDEmu;
import sidplay.Player;
import ui.common.C64Window;

public final class ResonanceGauge extends SIDGauge {

	@FXML
	private TitledPane border;
	@FXML
	private ImageView area;

	public ResonanceGauge() {
	}

	public ResonanceGauge(C64Window window, Player player) {
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
		int res = sidemu.readInternalRegister(0x17) >> 4 & 0xf;
		accumulate(res / 15f);
		return this;
	}
}