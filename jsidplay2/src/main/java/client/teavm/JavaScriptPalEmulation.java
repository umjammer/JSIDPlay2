package client.teavm;

import java.nio.Buffer;
import java.nio.IntBuffer;

import libsidplay.components.mos656x.IPALEmulation;
import libsidplay.components.mos656x.IPalette;
import libsidplay.components.mos656x.VIC;

public class JavaScriptPalEmulation implements IPALEmulation {

	/** ABGR pixel data. */
	private static final int[] VIC_PALETTE_NO_PAL = new int[] { 0xFF000000, 0xFFFFFFFF, 0xFF2B3768, 0xFFB2A470,
			0xFF863D6F, 0xFF438D58, 0xFF792835, 0xFF6FC7B8, 0xFF254F6F, 0xFF003943, 0xFF59679A, 0xFF444444, 0xFF6C6C6C,
			0xFF84D29A, 0xFFB55E6C, 0xFF959595, };

	/** Previous sequencer data */
	private int oldGraphicsData;

	private final IntBuffer pixels = IntBuffer.allocate(VIC.MAX_WIDTH * VIC.MAX_HEIGHT);
	private int n, nthFrame;
	
	public JavaScriptPalEmulation(int nthFrame) {
		this.nthFrame = nthFrame;
		n = 0;
	}

	@Override
	public void determineCurrentPalette(int rasterY, boolean isFrameStart) {
		oldGraphicsData = 0;
		if (isFrameStart) {
			((Buffer) pixels).clear();
			if (++n == nthFrame) {
				n = 0;
			}
		}
	}

	@Override
	public void drawPixels(int graphicsDataBuffer) {
		if (n == 0) {
			/* Pixels arrive in 0x12345678 order. */
			for (int j = 0; j < 2; j++) {
				oldGraphicsData |= graphicsDataBuffer >>> 16;
				for (int i = 0; i < 4; i++) {
					oldGraphicsData <<= 4;
					pixels.put(VIC_PALETTE_NO_PAL[(oldGraphicsData >>> 16) & 0x0f]);
				}
				graphicsDataBuffer <<= 16;
			}
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

	/**
	 * @return Output ABGR screen buffer as int32 array. MSB to LSB -&gt; alpha,
	 *         blue, green, red
	 */
	@Override
	public IntBuffer getPixels() {
		return pixels;
	}

	@Override
	public void reset() {
		// clear the screen
		((Buffer) pixels).clear();
		for (int i = 0; i < pixels.capacity(); i++) {
			pixels.put(0xFF000000);
		}
		((Buffer) pixels).clear();
	}
}
