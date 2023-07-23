package libsidutils;

public class C64FontUtils {

	/**
	 * Font resource name
	 */
	public static final String FONT_NAME = "/libsidutils/C64_Elite_Mono_v1.0-STYLE.ttf";

	/**
	 * Upper case letters.
	 */
	public static final int TRUE_TYPE_FONT_BIG = 0xe000;
	/**
	 * Lower case letters.
	 */
	public static final int TRUE_TYPE_FONT_SMALL = 0xe100;
	/**
	 * Inverse Upper case letters.
	 */
	public static final int TRUE_TYPE_FONT_INVERSE_BIG = 0xe200;
	/**
	 * Inverse Lower case letters.
	 */
	public static final int TRUE_TYPE_FONT_INVERSE_SMALL = 0xe300;

	public static final String petsciiToFont(final String s, int fontSet) {
		StringBuffer buf = new StringBuffer();
		for (char c : s.toCharArray()) {
			buf.append(petsciiToFont(c, fontSet));
		}
		return buf.toString();
	}

	public static final char petsciiToFont(final char c, int fontSet) {
		if ((c & 0x60) == 0) {
			return (char) (c | 0x40 | fontSet ^ 0x0200);
		} else {
			return (char) (c | fontSet);
		}
	}

}
