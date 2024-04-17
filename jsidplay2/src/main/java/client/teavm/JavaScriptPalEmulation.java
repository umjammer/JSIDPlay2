package client.teavm;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import libsidplay.components.mos656x.IPALEmulation;
import libsidplay.components.mos656x.IPalette;
import libsidplay.components.mos656x.VIC;

public class JavaScriptPalEmulation implements IPALEmulation {

	/** RGBA pixel data. */
	private static final int[] VIC_PALETTE_NO_PAL = new int[] { 0x000000FF, 0xFFFFFFFF, 0x68372BFF, 0x70A4B2FF,
			0x6F3D86FF, 0x588D43FF, 0x352879FF, 0xB8C76FFF, 0x6F4F25FF, 0x433900FF, 0x9A6759FF, 0x444444FF, 0x6C6C6CFF,
			0x9AD284FF, 0x6C5EB5FF, 0x959595FF, };

	/** Previous sequencer data */
	private int oldGraphicsData;

	private final ByteBuffer pixelsByteBuffer = ByteBuffer.allocate((VIC.MAX_WIDTH * VIC.MAX_HEIGHT) << 2);

	private final IntBuffer pixelsIntBuffer = pixelsByteBuffer.asIntBuffer();

	@Override
	public void determineCurrentPalette(int rasterY, boolean isFrameStart) {
		oldGraphicsData = 0;
		if (isFrameStart) {
			((Buffer) pixelsIntBuffer).clear();
		}
	}

	@Override
	public void drawPixels(int graphicsDataBuffer) {
		/* Pixels arrive in 0x12345678 order. */
		for (int j = 0; j < 2; j++) {
			oldGraphicsData |= graphicsDataBuffer >>> 16;
			for (int i = 0; i < 4; i++) {
				oldGraphicsData <<= 4;
				final int vicColor = oldGraphicsData >>> 16;
				pixelsIntBuffer.put(VIC_PALETTE_NO_PAL[vicColor & 0x0f]);
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

	@Override
	public IntBuffer getPixels() {
		return pixelsIntBuffer;
	}

	public byte[] getPixelsArray() {
		return pixelsByteBuffer.array();
	}

	@Override
	public void reset() {
		// clear the screen
		((Buffer) pixelsIntBuffer).clear();
		for (int i = 0; i < pixelsIntBuffer.capacity(); i++) {
			pixelsIntBuffer.put(0x000000FF);
		}
		((Buffer) pixelsIntBuffer).clear();
	}
}
