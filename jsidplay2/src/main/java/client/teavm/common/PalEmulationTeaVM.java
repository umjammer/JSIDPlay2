package client.teavm.common;

import static client.teavm.compiletime.PaletteTeaVM.COMBINED_LINES_EVEN;
import static client.teavm.compiletime.PaletteTeaVM.COMBINED_LINES_ODD;
import static client.teavm.compiletime.PaletteTeaVM.LINE_PALETTE_EVEN;
import static client.teavm.compiletime.PaletteTeaVM.LINE_PALETTE_ODD;
import static java.util.Arrays.stream;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Base64.Decoder;
import java.util.Map;

import client.teavm.compiletime.PaletteTeaVM;
import libsidplay.components.mos656x.IPALEmulation;
import libsidplay.components.mos656x.IPalette;
import libsidplay.components.mos656x.VIC;

/**
 * JavaScript needs BGRA (MSB to LSB) big endian.
 */
public class PalEmulationTeaVM implements IPALEmulation {

	/**
	 * RGBA pixel data (MSB to LSB). VIC colors without PAL emulation. Use this
	 * palette for VIC colors 0-15. https://www.pepto.de/projects/colorvic/2001/
	 */
	private static final int[] VIC_PALETTE_NO_PAL = new int[] { 0x000000FF, 0xFFFFFFFF, 0x68372BFF, 0x70A4B2FF,
			0x6F3D86FF, 0x588D43FF, 0x352879FF, 0xB8C76FFF, 0x6F4F25FF, 0x433900FF, 0x9A6759FF, 0x444444FF, 0x6C6C6CFF,
			0x9AD284FF, 0x6C5EB5FF, 0x959595FF, };

	/** Table for looking up color using a packed 2x8 value for even rasterlines */
	private final int[] combinedLinesEven;
	/** Table for looking up color using a packed 2x8 value for odd rasterlines */
	private final int[] combinedLinesOdd;
	/** VIC color palette for even rasterlines */
	private final byte[] linePaletteEven;
	/** VIC color palette for odd rasterlines */
	private final byte[] linePaletteOdd;
	/** Last line's color */
	private final byte[] previousLineDecodedColor = new byte[65 * 8];
	/** Prevailing table for looking up color for current line (odd/even) */
	private int[] combinedLinesCurrent;
	/** Prevailing VIC color palette for current line (odd/even) */
	private byte[] linePaletteCurrent;
	/** Index into last line */
	private int previousLineIndex;
	/** Previous sequencer data */
	private int oldGraphicsData;

	private final ByteBuffer pixels = ByteBuffer.allocate(VIC.MAX_WIDTH * VIC.MAX_HEIGHT << 2)
			.order(ByteOrder.BIG_ENDIAN);
	private final int nthFrame;
	private int n;

	private boolean palEmulationEnable;

	public PalEmulationTeaVM(int nthFrame, Decoder decoder) {
		this.nthFrame = nthFrame;
		Map<String, String> palette = PaletteTeaVM.getPalette(false);
		combinedLinesEven = stream(palette.get(COMBINED_LINES_EVEN).split(",")).mapToInt(Integer::parseInt)
				.toArray();
		combinedLinesOdd = stream(palette.get(COMBINED_LINES_ODD).split(",")).mapToInt(Integer::parseInt)
				.toArray();
		linePaletteEven = decoder.decode(palette.get(LINE_PALETTE_EVEN));
		linePaletteOdd = decoder.decode(palette.get(LINE_PALETTE_ODD));
		n = 0;
	}

	/**
	 * Determine palette for current raster line.
	 *
	 * @param rasterY      current raster line
	 * @param isFrameStart a new frame is about to start?
	 */
	@Override
	public void determineCurrentPalette(int rasterY, boolean isFrameStart) {
		if (isFrameStart) {
			((Buffer) pixels).clear();
			if (++n == nthFrame) {
				n = 0;
				if (palEmulationEnable) {
					/* current row odd? -> start with even, init, swap */
					linePaletteCurrent = (rasterY & 1) != 0 ? linePaletteEven : linePaletteOdd;
					combinedLinesCurrent = (rasterY & 1) != 0 ? combinedLinesEven : combinedLinesOdd;
					for (int i = 0; i < previousLineDecodedColor.length; i++) {
						previousLineDecodedColor[i] = linePaletteCurrent[0];
					}
				}
			}
		}
		if (palEmulationEnable && n == 0) {
			linePaletteCurrent = linePaletteCurrent == linePaletteOdd ? linePaletteEven : linePaletteOdd;
			combinedLinesCurrent = combinedLinesCurrent == combinedLinesOdd ? combinedLinesEven : combinedLinesOdd;
			oldGraphicsData = 0;
			previousLineIndex = 0;
		}
	}

	/**
	 * Draw eight pixels at once. Pixels arrive in 0x12345678 order (MSB to LSB).
	 *
	 * @param graphicsDataBuffer eight pixels each of 4 bits (VIC color value range
	 *                           0x0-0xF)
	 * @param pixelConsumer      consumer of the corresponding RGBA pixels
	 */
	@Override
	public void drawPixels(int graphicsDataBuffer) {
		if (n == 0) {
			/* Pixels arrive in 0x12345678 order. */
			for (int j = 0; j < 2; j++) {
				oldGraphicsData |= graphicsDataBuffer >>> 16;
				for (int i = 0; i < 4; i++) {
					oldGraphicsData <<= 4;
					if (palEmulationEnable) {
						final int vicColor = oldGraphicsData >>> 16;
						final byte lineColor = linePaletteCurrent[vicColor];
						final byte previousLineColor = previousLineDecodedColor[previousLineIndex];
						previousLineDecodedColor[previousLineIndex++] = lineColor;
						pixels.putInt(
								(combinedLinesCurrent[lineColor & 0xff | previousLineColor << 8 & 0xff00] << 8) | 0xff);
					} else {
						pixels.putInt(VIC_PALETTE_NO_PAL[(oldGraphicsData >>> 16) & 0x0f]);
					}
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
		this.palEmulationEnable = palEmulationEnable;
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
	 * @return Output RGBA screen buffer as int32 array. MSB to LSB -&gt; red,
	 *         green, blue, alpha
	 */
	@Override
	public ByteBuffer getPixels() {
		return pixels;
	}

	@Override
	public IntBuffer getPixelsAsIntBuffer() {
		return null;
	}

	@Override
	public void reset() {
		// clear the screen
		((Buffer) pixels).clear();
		for (int i = 0; i < pixels.capacity() >> 2; i++) {
			pixels.putInt(0x000000FF);
		}
		((Buffer) pixels).clear();
	}
}
