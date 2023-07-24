package libsidutils;

public class CBMCodeUtils extends C64FontUtils {
	/**
	 * Petscii to ISO-8859-1 conversion table.
	 */
	private static final int PETSCII_TO_ISO8859_1[] = { 0x00, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, // 0x00
			0x20, 0x20, 0x20, 0x20, 0x20, 0x0d, 0x20, 0x20, // 0x08
			0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, // 0x10
			0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, // 0x18
			0x20, 0x21, 0x20, 0x23, 0x24, 0x25, 0x26, 0x27, // 0x20 !"#$%&'
			0x28, 0x29, 0x2a, 0x2b, 0x2c, 0x2d, 0x2e, 0x2f, // 0x28 ()*+,-./
			0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, // 0x30 01234567
			0x38, 0x39, 0x3a, 0x3b, 0x3c, 0x3d, 0x3e, 0x3f, // 0x38 89:;<=>?
			0x40, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, // 0x40 @ABCDEFG
			0x48, 0x49, 0x4a, 0x4b, 0x4c, 0x4d, 0x4e, 0x4f, // 0x48 HIJKLMNO
			0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, // 0x50 PQRSTUVW
			0x58, 0x59, 0x5a, 0x5b, 0x24, 0x5d, 0x20, 0x20, // 0x58 XYZ[\]^_
			0x2d, 0x23, 0x7c, 0x2d, 0x2d, 0x2d, 0x2d, 0x7c, // 0x60
			0x7c, 0x5c, 0x5c, 0x2f, 0x5c, 0x5c, 0x2f, 0x2f, // 0x68
			0x5c, 0x23, 0x5f, 0x23, 0x7c, 0x2f, 0x58, 0x4f, // 0x70
			0x23, 0x7c, 0x23, 0x2b, 0x7c, 0x7c, 0x26, 0x5c, // 0x78
			0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, // 0x80
			0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, // 0x88
			0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, // 0x90
			0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, // 0x98
			0x20, 0x7c, 0x23, 0x2d, 0x2d, 0x7c, 0x23, 0x7c, // 0xa0 ¡¢£¤¥¦§
			0x23, 0x2f, 0x7c, 0x7c, 0x2f, 0x5c, 0x5c, 0x2d, // 0xa8 ¨©ª«¬­®¯
			0x2f, 0x2d, 0x2d, 0x7c, 0x7c, 0x7c, 0x7c, 0x2d, // 0xb0 °±²³´µ¶·
			0x2d, 0x2d, 0x2f, 0x5c, 0x5c, 0x2f, 0x2f, 0x23, // 0xb8 ¸¹º»¼½¾¿
			0x2d, 0x23, 0x7c, 0x2d, 0x2d, 0x2d, 0x2d, 0x7c, // 0xc0 ÀÁÂÃÄÅÆÇ
			0x7c, 0x5c, 0x5c, 0x2f, 0x5c, 0x5c, 0x2f, 0x2f, // 0xc8 ÈÉÊËÌÍÎÏ
			0x5c, 0x23, 0x5f, 0x23, 0x7c, 0x2f, 0x58, 0x4f, // 0xd0 ÐÑÒÓÔÕÖ×
			0x23, 0x7c, 0x23, 0x2b, 0x7c, 0x7c, 0x26, 0x5c, // 0xd8 ØÙÚÛÜÝÞß
			0x20, 0x7c, 0x23, 0x2d, 0x2d, 0x7c, 0x23, 0x7c, // 0xe0 àáâãäåæç
			0x23, 0x2f, 0x7c, 0x7c, 0x2f, 0x5c, 0x5c, 0x2d, // 0xe8 èéêëìíîï
			0x2f, 0x2d, 0x2d, 0x7c, 0x7c, 0x7c, 0x7c, 0x2d, // 0xf0 ðñòóôõö÷
			0x2d, 0x2d, 0x2f, 0x5c, 0x5c, 0x2f, 0x2f, 0x23 // 0xf8 øùúûüýþÿ
	 };

	/**
	 * ISO-8859-1 to screen RAM conversion table (Font2: upper case and lower case
	 * letters).
	 */
	private static final int[] ISO8859_1_TO_SCREENRAM = { 0x80, 0x81, 0x82, 0x83, 0x84, 0x85, 0x86, 0x87, // 0x00
			0x88, 0x89, 0x8a, 0x8b, 0x8c, 0x8d, 0x8e, 0x8f, // 0x08
			0x90, 0x91, 0x92, 0x93, 0x94, 0x95, 0x96, 0x97, // 0x10
			0x98, 0x99, 0x9a, 0x9b, 0x9c, 0x9d, 0x9e, 0x9f, // 0x18
			0x20, 0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, // 0x20 !"#$%&'
			0x28, 0x29, 0x2a, 0x2b, 0x2c, 0x2d, 0x2e, 0x2f, // 0x28 ()*+,-./
			0x30, 0x31, 0x32, 0x33, 0x34, 0x35, 0x36, 0x37, // 0x30 01234567
			0x38, 0x39, 0x3a, 0x3b, 0x3c, 0x3d, 0x3e, 0x3f, // 0x38 89:;<=>?
			0x00, 0x41, 0x42, 0x43, 0x44, 0x45, 0x46, 0x47, // 0x40 @ABCDEFG
			0x48, 0x49, 0x4a, 0x4b, 0x4c, 0x4d, 0x4e, 0x4f, // 0x48 HIJKLMNO
			0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x56, 0x57, // 0x50 PQRSTUVW
			0x58, 0x59, 0x5a, 0x1b, 0xbf, 0x1d, 0x1e, 0x64, // 0x58 XYZ[\]^_
			0x27, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, // 0x60 `abcdefg
			0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f, // 0x68 hijklmno
			0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16, 0x17, // 0x70 pqrstuvw
			0x18, 0x19, 0x1a, 0x1b, 0x5d, 0x1d, 0x1f, 0x20, // 0x78 xyz{|}~
			0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, // 0x80
			0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, // 0x88
			0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, // 0x90
			0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, 0x20, // 0x98
			0x20, 0x21, 0x03, 0x1c, 0xbf, 0x59, 0x5d, 0xbf, // 0xa0 ¡¢£¤¥¦§
			0x22, 0x43, 0x01, 0x3c, 0xbf, 0x2d, 0x52, 0x63, // 0xa8 ¨©ª«¬­®¯
			0x0f, 0xbf, 0x32, 0x33, 0x27, 0x15, 0xbf, 0xbf, // 0xb0 °±²³´µ¶·
			0x2c, 0x31, 0x0f, 0x3e, 0xbf, 0xbf, 0xbf, 0x3f, // 0xb8 ¸¹º»¼½¾¿
			0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x43, // 0xc0 ÀÁÂÃÄÅÆÇ
			0x45, 0x45, 0x45, 0x45, 0x49, 0x49, 0x49, 0x49, // 0xc8 ÈÉÊËÌÍÎÏ
			0xbf, 0x4e, 0x4f, 0x4f, 0x4f, 0x4f, 0x4f, 0x18, // 0xd0 ÐÑÒÓÔÕÖ×
			0x4f, 0x55, 0x55, 0x55, 0x55, 0x59, 0xbf, 0xbf, // 0xd8 ØÙÚÛÜÝÞß
			0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x01, 0x03, // 0xe0 àáâãäåæç
			0x05, 0x05, 0x05, 0x05, 0x09, 0x09, 0x09, 0x09, // 0xe8 èéêëìíîï
			0xbf, 0x0e, 0x0f, 0x0f, 0x0f, 0x0f, 0x0f, 0xbf, // 0xf0 ðñòóôõö÷
			0x0f, 0x15, 0x15, 0x15, 0x15, 0x19, 0xbf, 0x19 // 0xf8 øùúûüýþÿ
	};

	/**
	 * Converts PETSCII to ISO-8859-1 characters.
	 *
	 * @param petscii PETSCII bytes. (zero byte terminates the string)
	 *
	 * @return ISO-8859-1 characters.
	 */
	public static final String petsciiToIso88591(final byte[] petscii) {
		StringBuilder result = new StringBuilder();
		for (byte b : petscii) {
			if (b == 0) {
				break;
			}
			result.append((char) PETSCII_TO_ISO8859_1[b & 0xff]);
		}
		return result.toString();
	}

	/**
	 * Converts ISO-8859-1 to screen RAM characters.
	 *
	 * @param iso ISO-8859-1 bytes
	 *
	 * @return screen RAM characters.
	 */
	public static byte[] iso88591ToScreenRam(String iso, int maxLen) {
		byte[] result = new byte[Math.min(iso.length(), maxLen)];
		for (int i = 0; i < result.length; i++) {
			result[i] = (byte) ISO8859_1_TO_SCREENRAM[iso.charAt(i) & 0xff];
		}
		return result;
	}

	/**
	 * Convert ISO-8859-1 to screen RAM character.
	 *
	 * @param iso ISO-8859-1 character
	 * 
	 * @return screen RAM byte
	 */
	public static byte iso88591ToScreenRam(char iso) {
		return (byte) ISO8859_1_TO_SCREENRAM[iso & 0xff];
	}

	/**
	 * Converts PETSCII to screen RAM characters.
	 *
	 * @param petscii PETSCII bytes.
	 *
	 * @return screen RAM characters.
	 */
	public static final byte[] petsciiToScreenRam(String petscii) {
		byte[] result = new byte[petscii.length()];
		for (int i = 0; i < result.length; i++) {
			result[i] = petsciiToScreenRam((byte) petscii.charAt(i));
		}
		return result;
	}

	/**
	 * Convert PETSCII to screen RAM character.
	 * 
	 * https://sta.c64.org/cbm64pettoscr.html
	 *
	 * @param petscii PETSCII byte
	 * @return screen RAM character
	 */
	private static byte petsciiToScreenRam(byte petscii) {
		int code = petscii & 0xff;
		if (code < 0x1F) {
			code += 0x80;
		} else if (code < 0x3F) {
		} else if (code < 0x5F) {
			code -= 0x40;
		} else if (code < 0x7F) {
			code -= 0x20;
		} else if (code < 0x9F) {
			code += 0x40;
		} else if (code < 0xBF) {
			code -= 0x40;
//		} else if (code < 0xDF) {
//			code -= 0x80;
		} else if (code < 0xFE) {
			code -= 0x80;
		} else {
			code = 0x5E;
		}
		return (byte) code;
	}

}
