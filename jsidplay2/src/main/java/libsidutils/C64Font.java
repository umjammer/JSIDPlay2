package libsidutils;

public interface C64Font {

	/**
	 * Font resource name
	 */
	String FONT_NAME = "/libsidutils/C64_Elite_Mono_v1.0-STYLE.ttf";

	/**
	 * Upper case letters.
	 */
	int TRUE_TYPE_FONT_BIG = 0xe000;
	/**
	 * Lower case letters.
	 */
	int TRUE_TYPE_FONT_SMALL = 0xe100;
	/**
	 * Inverse Upper case letters.
	 */
	int TRUE_TYPE_FONT_INVERSE_BIG = 0xe200;
	/**
	 * Inverse Lower case letters.
	 */
	int TRUE_TYPE_FONT_INVERSE_SMALL = 0xe300;

	default String toC64Chars(final String s, int fontSet) {
		StringBuffer buf = new StringBuffer();
		for (char c : s.toCharArray()) {
			buf.append(toC64Char(c, fontSet));
		}
		return buf.toString();
	}

	default String toC64Char(final char c, int fontSet) {
		if ((c & 0x60) == 0) {
			return String.valueOf((char) (c | 0x40 | fontSet ^ 0x0200));
		} else {
			return String.valueOf((char) (c | fontSet));
		}
	}

}
