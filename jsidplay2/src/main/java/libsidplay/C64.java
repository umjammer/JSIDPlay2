package libsidplay;

import static libsidplay.common.SIDEmu.NONE;
import static libsidplay.components.pla.PLA.MAX_SIDS;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;

import libsidplay.common.CIAChipModel;
import libsidplay.common.CPUClock;
import libsidplay.common.Event;
import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDEmu;
import libsidplay.common.SIDListener;
import libsidplay.common.VICChipModel;
import libsidplay.components.c1530.DatasetteEnvironment;
import libsidplay.components.c1541.C1541Environment;
import libsidplay.components.c1541.IParallelCable;
import libsidplay.components.cart.Cartridge;
import libsidplay.components.cart.CartridgeType;
import libsidplay.components.joystick.IJoystick;
import libsidplay.components.keyboard.Keyboard;
import libsidplay.components.mos6510.IMOS6510Extension;
import libsidplay.components.mos6510.MOS6510;
import libsidplay.components.mos6526.MOS6526;
import libsidplay.components.mos656x.MOS6567;
import libsidplay.components.mos656x.MOS6569;
import libsidplay.components.mos656x.PALEmulation;
import libsidplay.components.mos656x.VIC;
import libsidplay.components.pla.Bank;
import libsidplay.components.pla.PLA;
import libsidplay.components.printer.UserportPrinterEnvironment;
import libsidplay.components.ram.SystemRAMBank;

/**
 * Commodore 64 emulation core.
 *
 * It consists of the following chips: PLA, MOS6510, MOS6526(a), VIC
 * 6569(PAL)/6567(NTSC), RAM/ROM.<BR>
 * Some connectors exist additionally: Keyboard, two Joysticks, parallel cable
 * and some memory expansions and other cartridges can be plugged in.
 *
 * @author Antti Lankila
 * @author Ken Händel
 *
 */
public abstract class C64 implements DatasetteEnvironment, C1541Environment, UserportPrinterEnvironment {

	/** System clock */
	protected CPUClock clock = CPUClock.PAL;

	/** MMU chip */
	private final PLA pla;

	/** CPU */
	private final MOS6510 cpu;

	/** CIA1 */
	protected final MOS6526 cia1;

	/** CIA2 */
	protected final MOS6526 cia2;

	/** Keyboard */
	protected final Keyboard keyboard;

	/** Attached parallel cable */
	protected IParallelCable parallelCable;

	/**
	 * Specific VIC used for PAL.
	 */
	protected final VIC palVic;

	/**
	 * Specific VIC used for NTSC.
	 */
	protected final VIC ntscVic;

	/** System memory array */
	protected final SystemRAMBank ramBank = new SystemRAMBank();

	/** System event context */
	protected final EventScheduler context;

	/** Number of entrances to play routine to determine tune speed */
	protected int callsToPlayRoutine;
	/** Last time tune speed has been measured */
	private long lastUpdate;
	/** detected tune speed */
	private double tuneSpeed;

	/** The interested party for playroutine entrances. */
	protected IMOS6510Extension playRoutineObserver;

	/** Joystick port devices */
	protected final IJoystick[] joystickPort = new IJoystick[2];

	/** Implementation of a disconnected Joystick */
	private final IJoystick disconnectedJoystick = () -> (byte) 0xff;

	/**
	 * Area backed by RAM, including cpu port addresses 0 and 1.
	 *
	 * This is bit of a fake. We know that the CPU port is an internal detail of the
	 * CPU, and therefore CPU should simply pay the price for reading/writing to
	 * 0/1.
	 *
	 * However, that would slow down all accesses, which is suboptimal. Therefore we
	 * install this little hook to the 4k 0 region to deal with this.
	 *
	 * @author Antti Lankila
	 */
	protected class ZeroRAMBank extends Bank {
		/** Value written to processor port. */
		private byte dir;
		private byte data;

		/** Value read from processor port. */
		private byte dataRead;

		/** State of processor port pins. */
		private byte dataOut;

		/**
		 * $01 bits 6 and 7 fall-off cycles (1-&gt;0), average is about 350 msec
		 */
		private static final long C64_CPU_DATA_PORT_FALL_OFF_CYCLES = 350000;

		/** cycle that should invalidate the unused bits of the data port. */
		private long dataSetClkBit6;
		private long dataSetClkBit7;

		/**
		 * indicates if the unused bits of the data port are still valid or should be
		 * read as 0, 1 = unused bits valid, 0 = unused bits should be 0
		 */
		private boolean dataSetBit6;
		private boolean dataSetBit7;

		/** indicated if the unused bits are in the process of falling off. */
		private boolean dataFalloffBit6;
		private boolean dataFalloffBit7;

		/** Tape motor status. */
		private byte oldPortDataOut;
		/** Tape write line status. */
		private byte oldPortWriteBit;

		public void reset() {
			oldPortDataOut = (byte) 0xff;
			oldPortWriteBit = (byte) 0xff;
			data = 0x3f;
			dataOut = 0x3f;
			dataRead = 0x3f;
			dir = 0;
			dataSetBit6 = false;
			dataSetBit7 = false;
			dataFalloffBit6 = false;
			dataFalloffBit7 = false;
			updateCpuPort();
		}

		private void updateCpuPort() {
			dataOut = (byte) (dataOut & ~dir | data & dir);
			dataRead = (byte) ((data | ~dir) & (dataOut | 0x17));
			pla.setCpuPort(dataRead);

			if (0 == (dir & 0x20)) {
				dataRead &= 0xdf;
			}
			if (0 == (dir & 0x10) && C64.this.getTapeSense()) {
				dataRead &= 0xef;
			}

			if ((dir & data & 0x20) != oldPortDataOut) {
				oldPortDataOut = (byte) (dir & data & 0x20);
				C64.this.setMotor(0 == oldPortDataOut);
			}

			if (((~dir | data) & 0x8) != oldPortWriteBit) {
				oldPortWriteBit = (byte) ((~dir | data) & 0x8);
				C64.this.toggleWriteBit(((~dir | data) & 0x8) != 0);
			}
		}

		@Override
		public byte read(final int address) {
			if (address == 0) {
				return dir;
			} else if (address == 1) {
				if (dataFalloffBit6 || dataFalloffBit7) {
					if (dataSetClkBit6 < context.getTime(Phase.PHI2)) {
						dataFalloffBit6 = false;
						dataSetBit6 = false;
					}

					if (dataSetClkBit7 < context.getTime(Phase.PHI2)) {
						dataSetBit7 = false;
						dataFalloffBit7 = false;
					}
				}
				return (byte) (dataRead & 0xff - (((!dataSetBit6 ? 1 : 0) << 6) + ((!dataSetBit7 ? 1 : 0) << 7)));
			} else {
				return ramBank.read(address);
			}
		}

		@Override
		public void write(final int address, byte value) {
			if (address == 0) {
				if (dataSetBit7 && (value & 0x80) == 0 && !dataFalloffBit7) {
					dataFalloffBit7 = true;
					dataSetClkBit7 = context.getTime(Phase.PHI2) + C64_CPU_DATA_PORT_FALL_OFF_CYCLES;
				}
				if (dataSetBit6 && (value & 0x40) == 0 && !dataFalloffBit6) {
					dataFalloffBit6 = true;
					dataSetClkBit6 = context.getTime(Phase.PHI2) + C64_CPU_DATA_PORT_FALL_OFF_CYCLES;
				}
				if (dataSetBit7 && (value & 0x80) != 0 && dataFalloffBit7) {
					dataFalloffBit7 = false;
				}
				if (dataSetBit6 && (value & 0x40) != 0 && dataFalloffBit6) {
					dataFalloffBit6 = false;
				}
				dir = value;
				updateCpuPort();
				value = pla.getDisconnectedBusBank().read(address);
			} else if (address == 1) {
				if ((dir & 0x80) != 0 && (value & 0x80) != 0) {
					dataSetBit7 = true;
				}
				if ((dir & 0x40) != 0 && (value & 0x40) != 0) {
					dataSetBit6 = true;
				}
				data = value;
				updateCpuPort();
				value = pla.getDisconnectedBusBank().read(address);
			}
			ramBank.write(address, value);
		}
	}

	/** Zero page memory bank */
	private final ZeroRAMBank zeroRAMBank = new ZeroRAMBank();

	/**
	 * Set play routine address to watch by CPU emulation.
	 *
	 * @param playAddr Observe calls of SID player (JSR $PlayAddr).
	 */
	public void setPlayAddr(final int playAddr) {
		cpu.setJmpJsrHandler(Register_ProgramCounter -> {
			if (Register_ProgramCounter == playAddr) {
				if (playRoutineObserver != null) {
					playRoutineObserver.jmpJsr();
				}
				callsToPlayRoutine++;
			}
		});
	}

	/**
	 * Determine tune speed (calls of play routine per frame).
	 *
	 * @return current tune speed
	 */
	public final double determineTuneSpeed() {
		final double cpuFreq = clock.getCpuFrequency();
		final long now = context.getTime(Event.Phase.PHI1);
		final double interval = now - lastUpdate;
		if (interval >= cpuFreq) {
			lastUpdate = now;
			tuneSpeed = callsToPlayRoutine * cpuFreq / (interval * clock.getScreenRefresh());
			callsToPlayRoutine = 0;
		}
		return tuneSpeed;
	}

	public final void setParallelCable(final IParallelCable parallelCable) {
		this.parallelCable = parallelCable;
	}

	public C64(byte[] charBin, byte[] basicBin, byte[] kernalBin) {
		this(context -> new MOS6510(context), charBin, basicBin, kernalBin);
	}

	public C64(Function<EventScheduler, MOS6510> cpuCreator, byte[] charBin, byte[] basicBin, byte[] kernalBin) {
		context = new EventScheduler();

		cpu = cpuCreator.apply(context);
		pla = new PLA(context, zeroRAMBank, ramBank, charBin, basicBin, kernalBin);

		cpu.setMemoryHandler(address -> pla.cpuRead(address), (address, value) -> pla.cpuWrite(address, value));
		pla.setCpu(cpu);

		palVic = new MOS6569(pla, context);
		palVic.setPalEmulation(new PALEmulation(VICChipModel.MOS6567R8));

		ntscVic = new MOS6567(pla, context);
		ntscVic.setPalEmulation(new PALEmulation(VICChipModel.MOS6567R8));

		pla.setVic(palVic);

		cia1 = new MOS6526(context, CIAChipModel.MOS6526) {
			@Override
			public void interrupt(final boolean state) {
				pla.setIRQ(state);
			}

			@Override
			public void writePRA(final byte data) {
			}

			@Override
			public void writePRB(final byte data) {
				if ((data & 0x10) == 0) {
					getVIC().triggerLightpen();
				} else {
					getVIC().clearLightpen();
				}
			}

			@Override
			public byte readPRA() {
				byte prbOut = (byte) (regs[PRB] | ~regs[DDRB]);
				prbOut &= joystickPort[0].getValue();
				final byte kbd = keyboard.readColumn(prbOut);
				final byte joy = joystickPort[1].getValue();
				return (byte) (kbd & joy);
			}

			@Override
			public byte readPRB() {
				byte praOut = (byte) (regs[PRA] | ~regs[DDRA]);
				praOut &= joystickPort[1].getValue();
				final byte kbd = keyboard.readRow(praOut);
				final byte joy = joystickPort[0].getValue();
				return (byte) (kbd & joy);
			}

			@Override
			public void pulse() {
				/* Nobody home */
			}
		};
		pla.setCia1(cia1);

		cia2 = new MOS6526(context, CIAChipModel.MOS6526) {
			@Override
			public void interrupt(final boolean state) {
				pla.setNMI(state);
			}

			@Override
			public void writePRA(final byte data) {
				pla.setVicMemBase((~data & 3) << 14);
				C64.this.writeToIECBus((byte) ~data);
				C64.this.printerUserportWriteStrobe((~data & 0x04) != 0);
			}

			@Override
			public void writePRB(final byte data) {
				parallelCable.c64Write(data);
			}

			@Override
			public byte readPRA() {
				return (byte) (C64.this.readFromIECBus() | 0x3f);
			}

			@Override
			public byte readPRB() {
				return parallelCable.c64Read();
			}

			@Override
			public void pulse() {
				parallelCable.pulse();
				C64.this.printerUserportWriteData(regs[PRB]);
			}
		};
		pla.setCia2(cia2);

		keyboard = new Keyboard() {
			@Override
			public void restore() {
				/*
				 * in reality, the PLA chip has a RESTORE line, and it pulls the NMI low. We're
				 * modeling the overall NMI and IRQ state in the PLA, so we can't really do
				 * that. We just pulse NMI instead.
				 */
				pla.setNMI(true);
				pla.setNMI(false);
			}
		};

		setJoystick(0, null);
		setJoystick(1, null);
	}

	/**
	 * Perform the equivalent of full power-on reset of C64, re-initializing
	 * everything.
	 */
	public void reset() {
		context.reset();
		keyboard.reset();
		pla.reset();
		cpu.setJmpJsrHandler(null);
		cpu.triggerRST();
		cia1.reset();
		cia2.reset();
		getVIC().reset();
		zeroRAMBank.reset();
		ramBank.reset();

		callsToPlayRoutine = 0;
		lastUpdate = 0;
		tuneSpeed = 0;
	}

	/**
	 * Return the array backing C64 RAM
	 *
	 * @return the RAM
	 */
	public byte[] getRAM() {
		return ramBank.array();
	}

	/**
	 * Return CPU emulator
	 *
	 * @return the cpu
	 */
	public MOS6510 getCPU() {
		return cpu;
	}

	/**
	 * Install a play routine observer to hook the JSR command of the CPU. It gets
	 * called, if the player address gets called.
	 *
	 * @param observer play routine observer
	 */
	public void setPlayRoutineObserver(final IMOS6510Extension observer) {
		playRoutineObserver = observer;
	}

	/**
	 * Get VIC chip emulator (PAL/NTSC).
	 *
	 * @return VIC chip
	 */
	public VIC getVIC() {
		return clock == CPUClock.NTSC ? ntscVic : palVic;
	}

	public int getVicMemBase() {
		return pla.getVicMemBase();
	}

	/**
	 * Insert SID chips to be used.
	 *
	 * @param sidCreator Responsible to decide which SID chips we need (SIDEmu) and
	 *                   which we don't need (NONE). SID number and old SID are
	 *                   mapped to new SID.
	 * @param sidLocator Responsible to determine the base address of the SID chips
	 *                   we need
	 */
	public final void insertSIDChips(BiFunction<Integer, SIDEmu, SIDEmu> sidCreator, IntFunction<Integer> sidLocator) {
		for (int sidNum = 0; sidNum < MAX_SIDS; sidNum++) {
			SIDEmu oldSid = pla.getSIDBank().getSID(sidNum);
			if (oldSid != NONE) {
				pla.getSIDBank().unplugSID(sidNum, oldSid, sidLocator.apply(sidNum));
			}
			SIDEmu newSid = sidCreator.apply(sidNum, oldSid);
			if (newSid != NONE) {
				pla.getSIDBank().plugInSID(sidNum, newSid, sidLocator.apply(sidNum));
			}
		}
	}

	/**
	 * Configure PAL and NTSC VIC.
	 *
	 * @param action VIC consumer
	 */
	public void configureVICs(Consumer<VIC> action) {
		action.accept(palVic);
		action.accept(ntscVic);
	}

	/**
	 * Configure all available SIDs.
	 *
	 * @param action SID chip consumer
	 */
	public final void configureSIDs(BiConsumer<Integer, SIDEmu> action) {
		for (int chipNum = 0; chipNum < MAX_SIDS; chipNum++) {
			final SIDEmu sid = pla.getSIDBank().getSID(chipNum);
			if (sid != NONE) {
				action.accept(chipNum, sid);
			}
		}
	}

	/**
	 * Configure one specific SID.
	 *
	 * @param chipNum SID chip number
	 * @param action  SID chip consumer
	 */
	public final void configureSID(int chipNum, Consumer<SIDEmu> action) {
		final SIDEmu sid = pla.getSIDBank().getSID(chipNum);
		if (sid != NONE) {
			action.accept(sid);
		}
	}

	/**
	 * Get SID register write notifications.
	 */
	public void setSIDListener(SIDListener listener) {
		pla.getSIDBank().setSIDListener(listener);
	}

	/**
	 * Set system clock (PAL/NTSC).
	 *
	 * @param clock system clock (PAL/NTSC)
	 */
	protected void setClock(final CPUClock clock) {
		this.clock = clock;

		context.setCyclesPerSecond(clock.getCpuFrequency());
		cia1.setDayOfTimeRate(clock.getCpuFrequency() / clock.getScreenRefresh());
		cia2.setDayOfTimeRate(clock.getCpuFrequency() / clock.getScreenRefresh());
		pla.setVic(getVIC());
	}

	/**
	 * Get system clock (PAL/NTSC).
	 *
	 * @return system clock (PAL/NTSC)
	 */
	public CPUClock getClock() {
		return clock;
	}

	/**
	 * Get C64's event scheduler
	 *
	 * @return the scheduler
	 */
	public EventScheduler getEventScheduler() {
		return context;
	}

	/**
	 * Installs a custom Kernal ROM.
	 *
	 * @param kernalRom Kernal ROM replacement (null means original Kernal)
	 */
	public void setCustomKernal(final byte[] kernalRom) {
		if (kernalRom == null) {
			pla.setCustomKernalRomBank(null);
		} else {
			pla.setCustomKernalRomBank(new Bank() {
				@Override
				public byte read(final int address) {
					return kernalRom[address & 0x1fff];
				}

				@Override
				public void write(final int address, final byte value) {
					throw new RuntimeException("This bank should never be mapped to W mode");
				}
			});
		}
	}

	/**
	 * Get current keyboard emulation.
	 *
	 * @return current keyboard emulation
	 */
	public Keyboard getKeyboard() {
		return keyboard;
	}

	/**
	 * Set joystick implementation.
	 *
	 * @param portNumber     joystick port (0-1)
	 * @param joystickReader joystick implementation or null (disconnected)
	 */
	public final void setJoystick(final int portNumber, final IJoystick joystickReader) {
		joystickPort[portNumber] = joystickReader == null ? disconnectedJoystick : joystickReader;
	}

	/**
	 * Is joystick connected?
	 *
	 * @param portNumber joystick port (0-1)
	 * @return joystick connected?
	 */
	public final boolean isJoystickConnected(final int portNumber) {
		return !joystickPort[portNumber].equals(disconnectedJoystick);
	}

	/**
	 * Insert a cartridge of a given size with empty contents.
	 *
	 * @param type   cartridge type
	 * @param sizeKB size in KB
	 * @throws IOException never thrown here
	 */
	public final void setCartridge(final CartridgeType type, final int sizeKB) throws IOException {
		pla.setCartridge(Cartridge.create(pla, type, sizeKB));
	}

	/**
	 * Insert a cartridge loading an image file.
	 *
	 * @param type cartridge type
	 * @param file file to load the RAM contents
	 * @throws IOException image read error
	 */
	public final void setCartridge(final CartridgeType type, final File file) throws IOException {
		pla.setCartridge(Cartridge.read(pla, type, file));
	}

	/**
	 * Insert a cartridge of type CRT loading an image.
	 *
	 * @param is input stream to load the RAM contents
	 * @throws IOException image read error
	 */
	public final void setCartridgeCRT(final InputStream is) throws IOException {
		pla.setCartridge(Cartridge.readCRT(pla, new DataInputStream(is)));
	}

	/**
	 * Get current multi purpose cartridge of the expansion port of the C64.
	 *
	 * @return multi purpose cartridge
	 */
	public final Cartridge getCartridge() {
		return pla.getCartridge();
	}

	/**
	 * @return is a multi purpose cartridge inserted into the expansion port of the
	 *         C64?
	 */
	public boolean isCartridge() {
		return pla.isCartridge();
	}

	/**
	 * Eject multi purpose cartridge from the expansion port of the C64.
	 */
	public final void ejectCartridge() {
		pla.setCartridge(null);
	}
}
