package sidplay;

import java.io.DataInputStream;
import java.io.IOException;

import libsidplay.HardwareEnsemble;
import libsidplay.components.printer.mps803.MPS803;

public class AllRoms {
	private static final String CHAR_ROM = "/libsidplay/roms/char.bin";
	private static final String BASIC_ROM = "/libsidplay/roms/basic.bin";
	private static final String KERNAL_ROM = "/libsidplay/roms/kernal.bin";
	private static final String JIFFYDOS_C64_ROM = "/libsidplay/roms/JiffyDOS_C64_6.01.bin";
	private static final String JIFFYDOS_C1541_ROM = "/libsidplay/roms/JiffyDOS_1541-II_6.00.bin";
	private static final String C1541_ROM = "/libsidplay/roms/c1541.bin";
	private static final String C1541_II_ROM = "/libsidplay/roms/c1541-2.bin";
	private static final String MPS803_CHAR_ROM = "/libsidplay/roms/mps803char.bin";

	private static final int CHAR_LENGTH = 0x1000;
	private static final int BASIC_LENGTH = 0x2000;
	private static final int KERNAL_LENGTH = 0x2000;
	private static final int JIFFYDOS_C64_LENGTH = 0x2000;
	private static final int JIFFYDOS_C1541_LENGTH = 0x4000;
	private static final int C1541_LENGTH = 0x4000;
	private static final int C1541_II_LENGTH = 0x4000;
	private static final int MPS803_CHAR_LENGTH = 3584;

	public static final byte[] CHAR = new byte[CHAR_LENGTH];
	public static final byte[] BASIC = new byte[BASIC_LENGTH];
	public static final byte[] KERNAL = new byte[KERNAL_LENGTH];
	public static final byte[] JIFFYDOS_C64 = new byte[JIFFYDOS_C64_LENGTH];
	public static final byte[] JIFFYDOS_C1541 = new byte[JIFFYDOS_C1541_LENGTH];
	public static final byte[] C1541 = new byte[C1541_LENGTH];
	public static final byte[] C1541_II = new byte[C1541_II_LENGTH];
	public static final byte[] MPS803_CHAR = new byte[MPS803_CHAR_LENGTH];

	static {
		Class<HardwareEnsemble> resClz = HardwareEnsemble.class;
		try (DataInputStream isChar = new DataInputStream(resClz.getResourceAsStream(CHAR_ROM));
				DataInputStream isBasic = new DataInputStream(resClz.getResourceAsStream(BASIC_ROM));
				DataInputStream isKernal = new DataInputStream(resClz.getResourceAsStream(KERNAL_ROM))) {
			isChar.readFully(CHAR);
			isBasic.readFully(BASIC);
			isKernal.readFully(KERNAL);
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
		try (DataInputStream isJiffyDosC64 = new DataInputStream(resClz.getResourceAsStream(JIFFYDOS_C64_ROM));
				DataInputStream isJiffyDosC1541 = new DataInputStream(resClz.getResourceAsStream(JIFFYDOS_C1541_ROM))) {
			isJiffyDosC64.readFully(JIFFYDOS_C64);
			isJiffyDosC1541.readFully(JIFFYDOS_C1541);
		} catch (final IOException e) {
			throw new ExceptionInInitializerError(e);
		}
		try (DataInputStream isC1541 = new DataInputStream(resClz.getResourceAsStream(C1541_ROM));
				DataInputStream isC1541_II = new DataInputStream(resClz.getResourceAsStream(C1541_II_ROM))) {
			isC1541.readFully(C1541);
			isC1541_II.readFully(C1541_II);
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
		try (DataInputStream is = new DataInputStream(MPS803.class.getResourceAsStream(MPS803_CHAR_ROM))) {
			is.readFully(MPS803_CHAR);
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

}
