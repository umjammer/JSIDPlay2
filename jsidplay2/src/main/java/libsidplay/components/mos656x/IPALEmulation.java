package libsidplay.components.mos656x;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public interface IPALEmulation {

	/**
	 * Determine palette for current raster line.
	 *
	 * @param rasterY      current raster line
	 * @param isFrameStart a new frame is about to start?
	 */
	void determineCurrentPalette(int rasterY, boolean isFrameStart);

	/**
	 * Draw eight pixels at once. Pixels arrive in 0x12345678 order (MSB to LSB).
	 *
	 * @param graphicsDataBuffer eight pixels each of 4 bits (VIC color value range
	 *                           0x0-0xF)
	 * @param pixelConsumer      consumer of the corresponding RGBA pixels
	 */
	void drawPixels(int graphicsDataBuffer);

	/**
	 * Updates the palette using the current palette settings.
	 */
	void updatePalette();

	void setPalEmulationEnable(boolean palEmulationEnable);

	void setVicPaletteNoPal(int[] vicPaletteNoPal);

	/**
	 * Gets the currently used palette.
	 *
	 * @return The currently used palette.
	 */
	IPalette getPalette();

	void reset();

	/**
	 * @return Output ARGB screen buffer as byte array. alpha, red, green, blue for
	 *         each pixel.
	 */
	ByteBuffer getPixels();

	IntBuffer getPixelsAsIntBuffer();

}
