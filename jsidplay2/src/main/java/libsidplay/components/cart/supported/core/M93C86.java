/**
 * m93c86.c
 *
 * Written by
 *  Groepaz/Hitmen <groepaz@gmx.net>
 *
 * This file is part of VICE, the Versatile Commodore Emulator.
 * See README for copyright notice.
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA
 *  02111-1307  USA.
 *
 */
package libsidplay.components.cart.supported.core;

import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public class M93C86 {

	private static final Logger LOG = Logger.getLogger(M93C86.class.getName());

	/* FIXME get rid of this */
	private static final int M93C86_SIZE = 2048;

	private int m93c86_data[] = new int[M93C86_SIZE];

	private int eeprom_cs = 0;
	private int eeprom_clock = 0;
	private int eeprom_data_in = 0;
	private int eeprom_data_out = 0;

	private int input_shiftreg = 0;
	private int input_count = 0;
	private int output_shiftreg = 0;
	private int output_count = 0;

	private int command = 0;
	private int addr = 0;
	private int data0 = 0;
	private int data1 = 0;

	private int write_enable_status = 0;
	@SuppressWarnings("unused")
	private int ready_busy_status = 1;

	private static final int CMD00 = 1;
	private static final int CMDWRITE = 2;
	private static final int CMDREAD = 3;
	private static final int CMDERASE = 4;
	private static final int CMDWEN = 5;
	private static final int CMDWDS = 6;
	private static final int CMDERAL = 7;
	private static final int CMDWRAL = 8;
	private static final int CMDREADDUMMY = 9;
	private static final int CMDREADDATA = 10;
	private static final int CMDISBUSY = 11;
	private static final int CMDISREADY = 12;

	private static final int STATUSREADY = 1;
	private static final int STATUSBUSY = 0;

	public void reset_input_shiftreg() {
		/* clear input shift register */
		input_shiftreg = 0;
		input_count = 0;
	}

	public int m93c86_read_data() {
		if (eeprom_cs == 1) {
			switch (command) {
			case CMDISBUSY:
				/*
				 * the software will see one busy state for one read, this is not quite what
				 * really happens
				 */
				LOG.finest(("busy status is 1"));
				command = CMDISREADY;
				return STATUSBUSY;

			case CMDISREADY:
				LOG.finest(("busy status is 0, end of command"));
				ready_busy_status = STATUSREADY;
				command = 0;
				return STATUSREADY;

			default:
				return eeprom_data_out;

			}
		} else {
			return 0;
		}
	}

	public void m93c86_write_data(int value) {
		if (eeprom_cs == 1) {
			eeprom_data_in = value;
		}
	}

	public void m93c86_write_select(int value) {
		/*
		 * Each instruction is preceded by a rising edge on Chip Select Input with
		 * Serial Clock being held low.
		 */
		if ((eeprom_cs == 0) && (value == 1) && (eeprom_clock == 0)) {
			reset_input_shiftreg();
		} else if ((eeprom_cs == 1) && (value == 0)) {
			/*
			 * a write or erase command kicks off on falling edge on CS and then signals
			 * busy state
			 */
			switch (command) {
			case CMDWRITE:
			case CMDWRAL:
			case CMDERAL:
				command = CMDISBUSY;
				break;
			}
		}
		eeprom_cs = value;
		if (eeprom_cs == 0) {
			/* read command is aborted when CS goes low */
			if ((command == CMDREAD) || (command == CMDREADDUMMY) || (command == CMDREADDATA)) {
				command = 0;
			}
		}
	}

	public void m93c86_write_clock(int value) {
		/* rising edge of clock will read one bit from data input */
		if ((eeprom_cs == 1) && (value == 1) && (eeprom_clock == 0)) {
			if (command == CMDREADDUMMY) {
				/* FIXME: this is kinda hackery, but works. *shrug* */
				output_shiftreg = m93c86_data[(addr << 1)];

				eeprom_data_out = 0;
				output_count = 0;

				eeprom_data_out = (output_shiftreg >> 7) & 1;
				LOG.finest(String.format("output %d pos %d addr %04x", eeprom_data_out, output_count, addr));
				output_shiftreg <<= 1;
				output_count++;

				command = CMDREADDATA;
				LOG.finest(String.format("load output from %04x with %02x", addr, output_shiftreg));
			} else if (command == CMDREADDATA) {
				eeprom_data_out = (output_shiftreg >> 7) & 1;
				output_shiftreg <<= 1;
				output_count++;
				switch (output_count) {
				case 8:
					output_shiftreg = m93c86_data[(addr << 1) + 1];
					LOG.finest(String.format("reload output from %04x with %02x", addr, output_shiftreg));
					break;
				case 16:
					addr = (addr + 1) & ((M93C86_SIZE / 2) - 1);
					output_shiftreg = m93c86_data[(addr << 1)];
					output_count = 0;
					LOG.finest(String.format("reload output from %04x with %02x", addr, output_shiftreg));
					break;
				}
			} else {
				/* shift internal shift register */
				input_shiftreg <<= 1;
				/* put bit from input to shift register */
				input_shiftreg |= eeprom_data_in;
				input_count++;
				switch (input_count) {
				case 1: /* start bit */
					if (eeprom_data_in == 0) {
						reset_input_shiftreg();
					}
					break;
				case 3: /* 2 command bits recieved */
					switch (input_shiftreg) {
					case 0x04: /* 100 */
						command = CMD00;
						break;
					case 0x05: /* 101 */
						command = CMDWRITE;
						break;
					case 0x06: /* 110 */
						command = CMDREAD;
						break;
					case 0x07: /* 111 */
						command = CMDERASE;
						break;
					}
					LOG.finest(String.format("first three command bits are: %x", input_shiftreg));
					break;
				case 5: /* 5 command bits recieved */
					if (command == CMD00) {
						switch (input_shiftreg) {
						case 0x10: /* 10000 */
							command = CMDWDS;
							break;
						case 0x11: /* 10001 */
							command = CMDWRAL;
							break;
						case 0x12: /* 10010 */
							command = CMDERAL;
							break;
						case 0x13: /* 10011 */
							command = CMDWEN;
							write_enable_status = 1;
							break;
						}
						LOG.finest(String.format("first five command bits are: %x", input_shiftreg));
					}
					break;
				case 13:
					switch (command) {
					case CMDREAD:
						command = CMDREADDUMMY;
						addr = ((input_shiftreg >> 0) & 0x3ff);
						reset_input_shiftreg();
						LOG.finest(String.format("CMD: read addr %04x", addr));
						break;
					case CMDWDS:
						write_enable_status = 0;
						reset_input_shiftreg();
						command = 0;
						LOG.finest(String.format("CMD: write disable"));
						break;
					case CMDWEN:
						write_enable_status = 1;
						reset_input_shiftreg();
						command = 0;
						LOG.finest(String.format("CMD: write enable"));
						break;
					case CMDERASE:
						if (write_enable_status == 0) {
							LOG.log(Level.SEVERE, "EEPROM: write not permitted for CMD 'erase'");
							reset_input_shiftreg();
							command = 0;
						} else {
							addr = ((input_shiftreg >> 0) & 0x3ff);
							ready_busy_status = STATUSBUSY;
							reset_input_shiftreg();
							m93c86_data[(addr << 1)] = 0xff;
							m93c86_data[(addr << 1) + 1] = 0xff;
							LOG.finest(String.format("CMD: erase addr %04x", addr));
						}
						break;
					case CMDERAL:
						if (write_enable_status == 0) {
							LOG.log(Level.SEVERE, "EEPROM: write not permitted for CMD 'erase all'");
							reset_input_shiftreg();
							command = 0;
						} else {
							ready_busy_status = STATUSBUSY;
							reset_input_shiftreg();
							Arrays.fill(m93c86_data, 0xff);
							LOG.finest(String.format("CMD: erase all"));
						}
						break;
					}
					break;
				case 29:
					switch (command) {
					case CMDWRITE:
						if (write_enable_status == 0) {
							LOG.log(Level.SEVERE, "EEPROM: write not permitted for CMD 'write'");
							reset_input_shiftreg();
							command = 0;
						} else {
							addr = ((input_shiftreg >> 16) & 0x3ff);
							data0 = ((input_shiftreg >> 8) & 0xff);
							data1 = ((input_shiftreg >> 0) & 0xff);
							ready_busy_status = STATUSBUSY;
							reset_input_shiftreg();
							m93c86_data[(addr << 1)] = data0;
							m93c86_data[(addr << 1) + 1] = data1;
							LOG.finest(String.format("CMD: write addr %04x %02x %02x", addr, data0, data1));
						}
						break;
					case CMDWRAL:
						if (write_enable_status == 0) {
							LOG.log(Level.SEVERE, "EEPROM: write not permitted for CMD 'write all'");
							reset_input_shiftreg();
							command = 0;
						} else {
							data0 = ((input_shiftreg >> 8) & 0xff);
							data1 = ((input_shiftreg >> 0) & 0xff);
							ready_busy_status = STATUSBUSY;
							reset_input_shiftreg();
							for (addr = 0; addr < (M93C86_SIZE / 2); addr++) {
								m93c86_data[(addr << 1)] = data0;
								m93c86_data[(addr << 1) + 1] = data1;
							}
							LOG.finest(String.format("CMD: write all %02x %02x", data0, data1));
						}
						break;
					}
					break;
				}
			}
		}
		eeprom_clock = value;
	}

}
