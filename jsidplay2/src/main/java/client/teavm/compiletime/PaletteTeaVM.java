package client.teavm.compiletime;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.IntStream.of;
import static org.teavm.metaprogramming.Metaprogramming.emit;
import static org.teavm.metaprogramming.Metaprogramming.exit;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.teavm.metaprogramming.CompileTime;
import org.teavm.metaprogramming.Meta;
import org.teavm.metaprogramming.Value;

import libsidplay.common.VICChipModel;
import libsidplay.components.mos656x.Palette;
import libsidplay.config.ISidPlay2Section;
import sidplay.ini.IniDefaults;

/**
 * Pre- generated PAL emulation color data to be included and used in the
 * JavaScript and web assembly version builds. These color data will be static
 * then and this helps to save time on startup in the browser.
 */
@CompileTime
public class PaletteTeaVM {

	private static final VICChipModel VIC_MODEL = VICChipModel.MOS6567R8;

	public static final String COMBINED_LINES_EVEN = "COMBINED_LINES_EVEN";
	public static final String COMBINED_LINES_ODD = "COMBINED_LINES_ODD";
	public static final String LINE_PALETTE_EVEN = "LINE_PALETTE_EVEN";
	public static final String LINE_PALETTE_ODD = "LINE_PALETTE_ODD";

	@Meta
	public static native Map<String, String> getPalette(boolean b);

	private static void getPalette(Value<Boolean> b) {
		final ISidPlay2Section sidplay2section = IniDefaults.SIDPLAY2_SECTION;

		final Palette palette = new Palette();
		palette.setBrightness(sidplay2section.getBrightness());
		palette.setContrast(sidplay2section.getContrast());
		palette.setGamma(sidplay2section.getGamma());
		palette.setSaturation(sidplay2section.getSaturation());
		palette.setPhaseShift(sidplay2section.getPhaseShift());
		palette.setOffset(sidplay2section.getOffset());
		palette.setTint(sidplay2section.getTint());
		palette.setLuminanceC(sidplay2section.getBlur());
		palette.setDotCreep(sidplay2section.getBleed());
		palette.calculatePalette(Palette.buildPaletteVariant(VIC_MODEL));

		String combinedLinesEven = of(palette.getEvenLines()).mapToObj(i -> format("%X", i)).collect(joining(","));
		String combinedLinesOdd = of(palette.getOddLines()).mapToObj(i -> format("%X", i)).collect(joining(","));
		String linePaletteEven = new String(Base64.getEncoder().encode(palette.getEvenFiltered()));
		String linePaletteOdd = new String(Base64.getEncoder().encode(palette.getOddFiltered()));

		Value<Map<String, String>> result = emit(() -> new HashMap<>());

		Value<String> combinedLinesEvenValue = emit(() -> combinedLinesEven);
		Value<String> combinedLinesOddValue = emit(() -> combinedLinesOdd);
		Value<String> linePaletteEvenValue = emit(() -> linePaletteEven);
		Value<String> linePaletteOddValue = emit(() -> linePaletteOdd);

		emit(() -> result.get().put(COMBINED_LINES_EVEN, combinedLinesEvenValue.get()));
		emit(() -> result.get().put(COMBINED_LINES_ODD, combinedLinesOddValue.get()));
		emit(() -> result.get().put(LINE_PALETTE_EVEN, linePaletteEvenValue.get()));
		emit(() -> result.get().put(LINE_PALETTE_ODD, linePaletteOddValue.get()));

		exit(() -> result.get());
	}
}
