package server.restful.common.barcode;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

public class BarCode {

	private static final MatrixToImageConfig DEFAULT_CONFIG = new MatrixToImageConfig();

	public static String createBarCodeImage(String data, String charset, int height, int width)
			throws IOException, WriterException {
		BufferedImage image = BarCode.createQR(data, charset, height, width);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(image, "png", baos);
		String imgData = DatatypeConverter.printBase64Binary(baos.toByteArray());
		String imageString = "data:image/png;base64," + imgData;
		return "<img src='" + imageString + "'>";
	}

	private static BufferedImage createQR(String data, String charset, int height, int width)
			throws WriterException, IOException {

		BitMatrix matrix = new MultiFormatWriter().encode(new String(data.getBytes(charset), charset),
				BarcodeFormat.QR_CODE, width, height);

		return toBufferedImage(matrix);
	}

	private static BufferedImage toBufferedImage(BitMatrix matrix) {
		return toBufferedImage(matrix, DEFAULT_CONFIG);
	}

	private static BufferedImage toBufferedImage(BitMatrix matrix, MatrixToImageConfig config) {
		int width = matrix.getWidth();
		int height = matrix.getHeight();
		BufferedImage image = new BufferedImage(width, height, config.getBufferedImageColorModel());
		int onColor = config.getPixelOnColor();
		int offColor = config.getPixelOffColor();
		int[] pixels = new int[width * height];
		int index = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				pixels[index++] = matrix.get(x, y) ? onColor : offColor;
			}
		}
		image.setRGB(0, 0, width, height, pixels, 0, width);
		return image;
	}
}
