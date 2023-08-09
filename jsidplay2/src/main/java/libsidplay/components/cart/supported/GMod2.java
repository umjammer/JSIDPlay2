package libsidplay.components.cart.supported;

import java.io.DataInputStream;
import java.io.IOException;

import libsidplay.components.cart.Cartridge;
import libsidplay.components.cart.supported.core.M93C86;
import libsidplay.components.pla.Bank;
import libsidplay.components.pla.PLA;

/**
 * <PRE>
 *     "GMod2" Cartridge (http://wiki.icomp.de/wiki/GMod2)
 *
 *     - this cart comes in 7 sizes, 8Kb, 16Kb, 32Kb, 64Kb, 128Kb, 256Kb and 512Kb.
 *     - ROM is always mapped in at $8000-$9FFF.
 *     
 *     XXX EEPROM functionality currently untested
 *
 * There is one register mapped to $DE00. The register is always active and there is no way to disable it.
 * bit 	r/w 	Flash ROM 	EEPROM 	Expansion Port
 * 7 	rw 	1=write enable (write) 	Data output (read) 	-
 * 6 	w 	- 	Chip select (1=selected) 	EXROM (0=active)
 * 5 	w 	Bank Selection Bit#5 	Clock 	-
 * 4 	w 	Bank Selection Bit#4 	Data input 	-
 * 3 	w 	Bank Selection Bit#3 	- 	-
 * 2 	w 	Bank Selection Bit#2 	- 	-
 * 1 	w 	Bank Selection Bit#1 	- 	-
 * 0 	w 	Bank Selection Bit#0 	- 	-
 * 
 * 
 * bit7 	bit6 	mode
 * 0 	0 	regular 8K Game mode, ROM readable at $8000
 * 0 	1 	EEPROM selected, Flash inactive. EEPROM can be used via bits 4/5/7
 * 1 	0 	illegal, do not use
 * 1 	1 	Flash ROM writing enabled
 * </PRE>
 *
 * @author Ken Händel
 *
 */
public class GMod2 extends Cartridge {

	/**
	 * Currently active flash ROM bank.
	 */
	protected int currentRomBank;

	/**
	 * 512KiB flash ROM - ROML banks 0..6 (each of size 0x2000).
	 */
	protected final byte[][] romlBanks;

	/**
	 * 2kB EEPROM
	 */
	private M93C86 m93c86 = new M93C86();

	private int eeprom_cs = 0, eeprom_data = 0, eeprom_clock = 0;

	public GMod2(final DataInputStream dis, final PLA pla) throws IOException {
		super(pla);
		final byte[] chipHeader = new byte[0x10];

		romlBanks = new byte[64][0x2000];
		for (int i = 0; i < 64 && dis.available() > 0; i++) {
			dis.readFully(chipHeader);
			if (chipHeader[0xb] >= (byte) 0x40
					|| (chipHeader[0xc] & 0xff) != 0x80 && (chipHeader[0xc] & 0xff) != 0xa0) {
				throw new RuntimeException("Unexpected Chip header!");
			}
			int bank = chipHeader[0xb] & 0xff;
			dis.readFully(romlBanks[bank]);
		}
	}

	private final Bank io1Bank = new Bank() {
		@Override
		public byte read(int address) {
			if (eeprom_cs != 0) {
				return (byte) ((m93c86.m93c86_read_data() << 7) | (pla.getDisconnectedBusBank().read(address) & 0x7f));
			}
			return 0;
		}

		@Override
		public void write(int address, byte value) {
			if (address == 0xde00) {
				if ((value & 0x40) != 0 && (value & 0x80) != 0) {
					// ultimax mode
					pla.setGameExrom(true, true, true, false);
				} else {
					pla.setGameExrom(true, (value & 0x80) != 0);
				}

				currentRomBank = value & 0x3f;

				eeprom_cs = (value >> 6) & 1;
				eeprom_data = (value >> 4) & 1;
				eeprom_clock = (value >> 5) & 1;
				m93c86.m93c86_write_select(eeprom_cs);
				if (eeprom_cs != 0) {
					m93c86.m93c86_write_data(eeprom_data);
					m93c86.m93c86_write_clock(eeprom_clock);
				}
			}
		}
	};

	private final Bank romlBank = new Bank() {
		@Override
		public byte read(int address) {
			return romlBanks[currentRomBank][address & 0x1fff];
		}
	};

	private final Bank romhBank = new Bank() {
		@Override
		public byte read(int address) {
			return pla.getDisconnectedBusBank().read(address);
		}

		@Override
		public void write(int address, byte value) {
			romlBanks[currentRomBank][address & 0x1fff] = value;
		};
	};

	@Override
	public Bank getRoml() {
		return romlBank;
	}

	@Override
	public Bank getRomh() {
		return romhBank;
	}

	@Override
	public Bank getIO1() {
		return io1Bank;
	}

	@Override
	public void reset() {
		super.reset();

		eeprom_cs = 0;
		m93c86.m93c86_write_select(eeprom_cs);

		io1Bank.write(0xde00, (byte) 0x00);
		pla.setGameExrom(true, false);
	}

}
