package ui.oscilloscope;

import javafx.fxml.FXML;
import javafx.scene.control.TitledPane;
import javafx.scene.image.ImageView;
import libsidplay.common.SIDEmu;
import sidplay.Player;
import ui.common.C64Window;

public final class FrequencyGauge extends SIDGauge {

	@FXML
	private TitledPane border;
	@FXML
	private ImageView area;

	public FrequencyGauge() {
	}

	public FrequencyGauge(C64Window window, Player player) {
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
		int frqValue = (sidemu.readInternalRegister(1 + getVoice() * 7) & 0xff) << 8
				| sidemu.readInternalRegister(0 + getVoice() * 7) & 0xff;
		float frq = 12 * 7;
		if (frqValue != 0) {
			frq = (float) (Math.log(frqValue / 65535.0f) / Math.log(2) * 12);
		}
		accumulate(1f + frq / (12 * 7));
		return this;
	}
}