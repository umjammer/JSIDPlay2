package sidplay.audio.sidreg;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import sidplay.audio.SIDRegDriver;

public class SidRegWrite {

	public static final String DESCRIPTION[] = new String[] { "VOICE_1_FREQ_L", "VOICE_1_FREQ_H", "VOICE_1_PULSE_L",
			"VOICE_1_PULSE_H", "VOICE_1_CTRL", "VOICE_1_AD", "VOICE_1_SR", "VOICE_2_FREQ_L", "VOICE_2_FREQ_H",
			"VOICE_2_PULSE_L", "VOICE_2_PULSE_H", "VOICE_2_CTRL", "VOICE_2_AD", "VOICE_2_SR", "VOICE_3_FREQ_L",
			"VOICE_3_FREQ_H", "VOICE_3_PULSE_L", "VOICE_3_PULSE_H", "VOICE_3_CTRL", "VOICE_3_AD", "VOICE_3_SR",
			"FCUT_L", "FCUT_H", "FRES", "FVOL", "PADDLE1", "PADDLE2", "OSC3", "ENV3", "UNUSED", "UNUSED", "UNUSED" };

	private long absCycles, relCycles;
	private int address, value;

	public SidRegWrite(long absCycles, long relCycles, int address, byte value) {
		this.absCycles = absCycles;
		this.relCycles = relCycles;
		this.address = address;
		this.value = value & 0xff;
	}

	public Long getAbsCycles() {
		return absCycles;
	}

	public long getRelCycles() {
		return relCycles;
	}

	public String getAddress() {
		return String.format("$%04X", address);
	}

	public String getValue() {
		return String.format("$%02X", value);
	}

	public String getDescription() {
		return SIDRegDriver.BUNDLE.getString(DESCRIPTION[address & 0x1f]);
	}

	public void writeSidRegister(OutputStream out, SIDRegDriver.Format format, boolean firstFrame) throws IOException {
		switch (format) {
		case NORMAL:
		default:
			out.write(String.format("\"%d\", \"%d\", \"$%04X\", \"$%02X\", \"%s\"\n", absCycles, relCycles, address,
					value, getDescription()).getBytes(StandardCharsets.ISO_8859_1));
			break;
		case APP:
			out.write(String.format(",\"%d\",\"$%04X\",\"$%02X\",\n", relCycles, address, value)
					.getBytes(StandardCharsets.ISO_8859_1));
			break;
		case C64_JUKEBOX:
			out.write(String.format("\n%d,%04X,%02X", relCycles, address, value).getBytes(StandardCharsets.ISO_8859_1));
			break;
		}
	}

}