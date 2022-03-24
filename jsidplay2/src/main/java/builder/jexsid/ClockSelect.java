package builder.jexsid;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Clock selection values for exSID_clockselect() */
public enum ClockSelect {
	/**
	 * select PAL clock
	 */
	XS_CL_PAL(0),
	/**
	 * select NTSC clock
	 */
	XS_CL_NTSC(1),
	/**
	 * select 1MHz clock
	 */
	XS_CL_1MHZ(2),;

	private int clockSelect;

	private ClockSelect(int clockSelect) {
		this.clockSelect = clockSelect;
	}

	public int getClockSelect() {
		return clockSelect;
	}

	private static final Map<Integer, ClockSelect> lookup = Collections.unmodifiableMap(
			Arrays.asList(ClockSelect.values()).stream().collect(Collectors.toMap(ClockSelect::getClockSelect, Function.identity())));

	public static ClockSelect get(int clockSelect) {
		return lookup.get(clockSelect);
	}
}