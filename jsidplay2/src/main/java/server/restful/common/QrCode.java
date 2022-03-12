package server.restful.common;

import static com.google.zxing.BarcodeFormat.QR_CODE;
import static java.awt.image.BufferedImage.TYPE_BYTE_BINARY;

import java.awt.image.BufferedImage;

import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

public class QrCode {

	private static final int BLACK = 0xFF000000;
	private static final int WHITE = 0xFFFFFFFF;

	public static BufferedImage createBarCodeImage(String data, String charset, int width, int height)
			throws WriterException {

		BitMatrix bitMatrix = new MultiFormatWriter().encode(data, QR_CODE, width, height);
		BufferedImage qrImage = new BufferedImage(bitMatrix.getWidth(), bitMatrix.getHeight(), TYPE_BYTE_BINARY);
		qrImage.setRGB(0, 0, bitMatrix.getWidth(), bitMatrix.getHeight(), toRGB(bitMatrix), 0, bitMatrix.getWidth());
		return qrImage;
	}

	private static int[] toRGB(BitMatrix matrix) {
		int[] result = new int[matrix.getWidth() * matrix.getHeight()];
		int i = 0;
		for (int x = 0; x < matrix.getWidth(); x++) {
			for (int y = 0; y < matrix.getHeight(); y++) {
				result[i++] = matrix.get(x, y) ? BLACK : WHITE;
			}
		}
		return result;
	}

}
