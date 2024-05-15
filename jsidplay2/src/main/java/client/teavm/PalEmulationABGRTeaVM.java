package client.teavm;

import static client.teavm.PaletteTeaVM.COMBINED_LINES_EVEN;
import static client.teavm.PaletteTeaVM.COMBINED_LINES_ODD;
import static client.teavm.PaletteTeaVM.LINE_PALETTE_EVEN;
import static client.teavm.PaletteTeaVM.LINE_PALETTE_ODD;
import static java.util.Arrays.stream;

import java.nio.Buffer;
import java.nio.IntBuffer;
import java.util.Base64.Decoder;
import java.util.Map;

import libsidplay.components.mos656x.IPALEmulation;
import libsidplay.components.mos656x.IPalette;
import libsidplay.components.mos656x.VIC;

public class PalEmulationABGRTeaVM implements IPALEmulation {

	/**
	 * ABGR pixel data. VIC colors without PAL emulation. Use this palette for VIC
	 * colors 0-15. https://www.pepto.de/projects/colorvic/2001/
	 */
	private static final int[] VIC_PALETTE_NO_PAL = new int[] { 0xFF000000, 0xFFFFFFFF, 0xFF2B3768, 0xFFB2A470,
			0xFF863D6F, 0xFF438D58, 0xFF792835, 0xFF6FC7B8, 0xFF254F6F, 0xFF003943, 0xFF59679A, 0xFF444444, 0xFF6C6C6C,
			0xFF84D29A, 0xFFB55E6C, 0xFF959595, };

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

	private final IntBuffer pixels = IntBuffer.allocate(VIC.MAX_WIDTH * VIC.MAX_HEIGHT);
	private final int nthFrame;
	private int n;

	private boolean palEmulationEnable;

	public PalEmulationABGRTeaVM(int nthFrame, Decoder decoder) {
		this.nthFrame = nthFrame;
		Map<String, String> palette = PaletteTeaVM.getPalette(false);
		this.combinedLinesEven = stream(palette.get(COMBINED_LINES_EVEN).split(",")).mapToInt(Integer::parseInt)
				.toArray();
		this.combinedLinesOdd = stream(palette.get(COMBINED_LINES_ODD).split(",")).mapToInt(Integer::parseInt)
				.toArray();
		this.linePaletteEven = decoder.decode(palette.get(LINE_PALETTE_EVEN));
		this.linePaletteOdd = decoder.decode(palette.get(LINE_PALETTE_ODD));
		n = 0;
	}

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
						// RGB -> ABGR
						int palCol = combinedLinesCurrent[lineColor & 0xff | previousLineColor << 8 & 0xff00];
						int r = ((palCol >> 16) & 0xff);
						int g = ((palCol >> 8) & 0xff);
						int b = palCol & 0xff;
						pixels.put(0xff000000 | (b << 16) | (g << 8) | r);
					} else {
						pixels.put(VIC_PALETTE_NO_PAL[(oldGraphicsData >>> 16) & 0x0f]);
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
