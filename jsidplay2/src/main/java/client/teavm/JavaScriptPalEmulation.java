package client.teavm;

import java.util.function.Consumer;

import libsidplay.components.mos656x.IPALEmulation;
import libsidplay.components.mos656x.IPalette;

public class JavaScriptPalEmulation implements IPALEmulation {
	/** Alpha channel of ARGB pixel data. */
	private static final int ALPHA = 0xff000000;

	private static final int[] VIC_PALETTE_NO_PAL = new int[] { 0x000000, 0xFFFFFF, 0x68372B, 0x70A4B2, 0x6F3D86,
			0x588D43, 0x352879, 0xB8C76F, 0x6F4F25, 0x433900, 0x9A6759, 0x444444, 0x6C6C6C, 0x9AD284, 0x6C5EB5,
			0x959595, };

	/** Previous sequencer data */
	private int oldGraphicsData;

	@Override
	public void determineCurrentPalette(int rasterY, boolean isFrameStart) {
		oldGraphicsData = 0;
	}

	@Override
	public void drawPixels(int graphicsDataBuffer, Consumer<Integer> pixelConsumer) {
		/* Pixels arrive in 0x12345678 order. */
		for (int j = 0; j < 2; j++) {
			oldGraphicsData |= graphicsDataBuffer >>> 16;
			for (int i = 0; i < 4; i++) {
				oldGraphicsData <<= 4;
				final int vicColor = oldGraphicsData >>> 16;
				pixelConsumer.accept(ALPHA | VIC_PALETTE_NO_PAL[vicColor & 0x0f]);
			}
			graphicsDataBuffer <<= 16;
		}
	}

	@Override
	public void updatePalette() {
	}

	@Override
	public void setPalEmulationEnable(boolean palEmulationEnable) {
	}

	@Override
	public void setVicPaletteNoPal(int[] vicPaletteNoPal) {
	}

	@Override
	public IPalette getPalette() {
		return new IPalette() {

			@Override
			public void setTint(float tint) {
			}

			@Override
			public void setSaturation(float saturation) {
			}

			@Override
			public void setPhaseShift(float phaseShift) {
			}

			@Override
			public void setOffset(float offset) {
			}

			@Override
			public void setLuminanceC(float luminanceC) {
			}

			@Override
			public void setGamma(float gamma) {
			}

			@Override
			public void setDotCreep(float dotCreep) {
			}

			@Override
			public void setContrast(float contrast) {
			}

			@Override
			public void setBrightness(float brightness) {
			}
		};
	}

}
