package libsidplay;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Function;

import libsidplay.common.CPUClock;
import libsidplay.common.Event;
import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;
import libsidplay.common.Ultimate64Mode;
import libsidplay.components.c1530.Datasette;
import libsidplay.components.c1541.C1541;
import libsidplay.components.c1541.C1541Runner;
import libsidplay.components.c1541.DisconnectedParallelCable;
import libsidplay.components.c1541.DiskImage;
import libsidplay.components.c1541.FloppyType;
import libsidplay.components.c1541.IExtendImageListener;
import libsidplay.components.c1541.IParallelCable;
import libsidplay.components.c1541.SameThreadC1541Runner;
import libsidplay.components.c1541.VIACore;
import libsidplay.components.cart.CartridgeType;
import libsidplay.components.iec.IECBus;
import libsidplay.components.iec.SerialIECDevice;
import libsidplay.components.mos6510.MOS6510;
import libsidplay.components.mos656x.IPALEmulation;
import libsidplay.components.mos656x.IPalette;
import libsidplay.components.mos656x.PALEmulation;
import libsidplay.components.printer.mps803.MPS803;
import libsidplay.config.IC1541Section;
import libsidplay.config.IConfig;
import libsidplay.config.ISidPlay2Section;
import libsidplay.sidtune.SidTune;
import libsidplay.sidtune.SidTuneError;
import libsidutils.IOUtils;
import libsidutils.prg2tap.PRG2TAP;
import libsidutils.prg2tap.PRG2TAPProgram;

/**
 * The HardwareEnsemble contains a C64 computer and additional peripherals.<BR>
 * It is meant as a complete hardware setup (C64, tape/disk drive, printer and
 * more).
 *
 * @author Ken Händel
 *
 */
public class HardwareEnsemble implements Ultimate64 {

	private final byte[] JIFFYDOS_C64;
	private final byte[] JIFFYDOS_C1541;
	private static byte[] EOD_HACK = new byte[] { (byte) 0xEE, (byte) 0xAD, 0x56, 0x30, (byte) 0x90, 0x46, 0x37,
			(byte) 0xCD, 0x3D, 0x4C, (byte) 0x85, (byte) 0x8F, 0x50, (byte) 0x86, 0x51, (byte) 0x92, };
	private static final byte[] EOD_HACK2 = new byte[] { 0x49, (byte) 0xB1, (byte) 0xAD, 0x2D, 0x1A, 0x01, 0x26,
			(byte) 0xFF, 0x25, (byte) 0xDB, (byte) 0xD3, 0x5E, 0x77, 0x3B, (byte) 0x93, (byte) 0xB2, };

	/**
	 * Configuration.
	 */
	protected IConfig config;

	/**
	 * C64 computer.
	 */
	protected C64 c64;
	/**
	 * C1530 datasette.
	 */
	protected Datasette datasette;
	/**
	 * IEC bus.
	 */
	protected IECBus iecBus;
	/**
	 * Additional serial devices like a printer (except of the floppies).
	 */
	protected SerialIECDevice[] serialDevices;
	/**
	 * C1541 floppy disk drives.
	 */
	protected C1541[] floppies;
	/**
	 * Responsible to keep C64 and C1541 in sync.
	 */
	protected C1541Runner c1541Runner;
	/**
	 * MPS803 printer.
	 */
	protected MPS803 printer;

	/**
	 * Disk image extension policy (handle track number greater than 35).
	 */
	private IExtendImageListener policy;

	/**
	 * Create a complete hardware setup (C64, tape/disk drive, printer and more).
	 */
	public HardwareEnsemble(IConfig config, Function<EventScheduler, MOS6510> cpuCreator, byte[] charBin,
			byte[] basicBin, byte[] kernalBin, byte[] jiffyDosC64Bin, byte[] jiffyDosC1541Bin, byte[] c1541Bin,
			byte[] c1541_IIBin, byte[] mps803CharBin) {
		JIFFYDOS_C64 = jiffyDosC64Bin;
		JIFFYDOS_C1541 = jiffyDosC1541Bin;

		this.config = config;
		this.iecBus = new IECBus();

		this.printer = new MPS803(this.iecBus, (byte) 4, (byte) 7, mps803CharBin) {
			@Override
			public void setBusy(final boolean flag) {
				c64.cia2.setFlag(flag);
			}

			@Override
			public long clk() {
				return c64.context.getTime(Phase.PHI2);
			}
		};

		this.c64 = new C64(cpuCreator, charBin, basicBin, kernalBin) {
			@Override
			public void printerUserportWriteData(final byte data) {
				if (config.getPrinterSection().isPrinterOn()) {
					printer.printerUserportWriteData(data);
				}
			}

			@Override
			public void printerUserportWriteStrobe(final boolean strobe) {
				if (config.getPrinterSection().isPrinterOn()) {
					printer.printerUserportWriteStrobe(strobe);
				}
			}

			@Override
			public byte readFromIECBus() {
				if (config.getC1541Section().isDriveOn()) {
					c1541Runner.synchronize(0);
				}
				return iecBus.readFromIECBus();
			}

			@Override
			public void writeToIECBus(final byte data) {
				if (config.getC1541Section().isDriveOn()) {
					c1541Runner.synchronize(1);
				}
				iecBus.writeToIECBus(data);
			}

			@Override
			public boolean getTapeSense() {
				return datasette.getTapeSense();
			}

			@Override
			public void setMotor(final boolean state) {
				datasette.setMotor(state);
			}

			@Override
			public void toggleWriteBit(final boolean state) {
				datasette.toggleWriteBit(state);
			}
		};

		this.datasette = new Datasette(c64.getEventScheduler()) {
			@Override
			public void setFlag(final boolean flag) {
				c64.cia1.setFlag(flag);
			}
		};

		final C1541 c1541 = new C1541(iecBus, 8, FloppyType.C1541, c1541Bin, c1541_IIBin);

		this.floppies = new C1541[] { c1541 };
		this.serialDevices = new SerialIECDevice[] { printer };

		this.iecBus.setFloppies(floppies);
		this.iecBus.setSerialDevices(serialDevices);
		this.c1541Runner = new SameThreadC1541Runner(c64.getEventScheduler(), c1541.getEventScheduler());
//		this.c1541Runner = new SeparateThreadC1541Runner(c64.getEventScheduler(), c1541.getEventScheduler());
	}

	/**
	 * Get Configuration
	 *
	 * @return configuration
	 */
	public final IConfig getConfig() {
		return config;
	}

	/**
	 * Get C64.
	 *
	 * @return C64
	 */
	public final C64 getC64() {
		return c64;
	}

	/**
	 * Get C1530 datasette.
	 *
	 * @return C1530 datasette
	 */
	public final Datasette getDatasette() {
		return datasette;
	}

	/**
	 * Get C1541 floppies.
	 *
	 * @return C1541 floppies
	 */
	public final C1541[] getFloppies() {
		return floppies;
	}

	/**
	 * Get MPS803 printer.
	 *
	 * @return MPS803 printer
	 */
	public final MPS803 getPrinter() {
		return printer;
	}

	/**
	 * Set frequency (PAL/NTSC)
	 *
	 * @param cpuFreq frequency (PAL/NTSC)
	 */
	public void setClock(final CPUClock cpuFreq) {
		c64.setClock(cpuFreq);
		c1541Runner.setClockDivider(cpuFreq);
		for (SerialIECDevice device : serialDevices) {
			device.setClock(cpuFreq);
		}
	}

	/**
	 * Reset hardware.
	 */
	public void reset() {
		final ISidPlay2Section sidplay2section = config.getSidplay2Section();
		c64.configureVICs(vic -> {
			IPALEmulation palEmulation = vic.getPalEmulation();
			if (palEmulation != PALEmulation.NONE) {
				palEmulation.setPalEmulationEnable(sidplay2section.isPalEmulation());
				IPalette palette = palEmulation.getPalette();
				palette.setBrightness(sidplay2section.getBrightness());
				palette.setContrast(sidplay2section.getContrast());
				palette.setGamma(sidplay2section.getGamma());
				palette.setSaturation(sidplay2section.getSaturation());
				palette.setPhaseShift(sidplay2section.getPhaseShift());
				palette.setOffset(sidplay2section.getOffset());
				palette.setTint(sidplay2section.getTint());
				palette.setLuminanceC(sidplay2section.getBlur());
				palette.setDotCreep(sidplay2section.getBleed());
			}
		});
		final IC1541Section c1541Section = config.getC1541Section();
		c64.setCustomKernal(c1541Section.isJiffyDosInstalled() ? JIFFYDOS_C64 : null);
		c64.reset();
		iecBus.reset();
		datasette.reset();

		// Reset Floppies
		for (final C1541 floppy : floppies) {
			floppy.setRamExpansion(0, c1541Section.isRamExpansion(0));
			floppy.setRamExpansion(1, c1541Section.isRamExpansion(1));
			floppy.setRamExpansion(2, c1541Section.isRamExpansion(2));
			floppy.setRamExpansion(3, c1541Section.isRamExpansion(3));
			floppy.setRamExpansion(4, c1541Section.isRamExpansion(4));
			floppy.setCustomKernalRom(c1541Section.isJiffyDosInstalled() ? JIFFYDOS_C1541 : null);
			floppy.setFloppyType(c1541Section.getFloppyType());
			floppy.reset();
		}
		enableFloppyDiskDrives(c1541Section.isDriveOn());
		connectC64AndC1541WithParallelCable(c1541Section.isParallelCable());

		// Reset IEC devices
		for (final SerialIECDevice serialDevice : serialDevices) {
			serialDevice.reset();
		}

		enablePrinter(config.getPrinterSection().isPrinterOn());
	}

	/**
	 * Enable floppy disk drives.
	 *
	 * @param on floppy disk drives enable
	 */
	public final void enableFloppyDiskDrives(final boolean on) {
		c64.getEventScheduler().scheduleThreadSafe(Event.of("C64-C1541 sync", event -> {
			if (on) {
				c1541Runner.reset();
			} else {
				c1541Runner.cancel();
			}
			for (C1541 floppy : floppies) {
				floppy.setPowerOn(on);
			}
		}));
	}

	/**
	 * Plug-in a parallel cable between the C64 user port and the C1541 floppy disk
	 * drive.
	 *
	 * @param connected connected enable
	 */
	public final void connectC64AndC1541WithParallelCable(final boolean connected) {
		final IParallelCable cable = connected ? makeCableBetweenC64AndC1541() : new DisconnectedParallelCable();
		c64.setParallelCable(cable);
		for (final C1541 floppy : floppies) {
			floppy.getBusController().setParallelCable(cable);
		}
	}

	/**
	 * Create a parallel cable between the C64 user port and the C1541 floppy disk
	 * drive.
	 *
	 * @return parallel cable
	 */
	private IParallelCable makeCableBetweenC64AndC1541() {
		return new IParallelCable() {

			protected byte parallelCableCpuValue = (byte) 0xff;
			protected final byte parallelCableDriveValue[] = { (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff };

			@Override
			public void driveWrite(final byte data, final boolean handshake, final int dnr) {
				c64.cia2.setFlag(handshake);
				parallelCableDriveValue[dnr & ~0x08] = data;
			}

			@Override
			public byte driveRead(final boolean handshake) {
				c64.cia2.setFlag(handshake);
				return parallelCableValue();
			}

			/**
			 * Return the current state of the parallel cable.
			 *
			 * @return the current state of the parallel cable
			 */
			private byte parallelCableValue() {
				byte val = parallelCableCpuValue;

				for (final C1541 floppy : floppies) {
					val &= parallelCableDriveValue[floppy.getID() & ~0x08];
				}
				return val;
			}

			@Override
			public void c64Write(final byte data) {
				if (config.getC1541Section().isDriveOn()) {
					c1541Runner.synchronize(0);
				}
				parallelCableCpuValue = data;
			}

			@Override
			public byte c64Read() {
				if (config.getC1541Section().isDriveOn()) {
					c1541Runner.synchronize(0);
				}
				return parallelCableValue();
			}

			@Override
			public void pulse() {
				if (config.getC1541Section().isDriveOn()) {
					c1541Runner.synchronize(0);
				}
				for (final C1541 floppy : floppies) {
					floppy.getBusController().signal(VIACore.VIA_SIG_CB1, VIACore.VIA_SIG_FALL);
				}
			}
		};
	}

	/**
	 * Turn-on printer.
	 *
	 * @param printerOn printer on/off
	 */
	public final void enablePrinter(boolean printerOn) {
		printer.turnPrinterOnOff(printerOn);
	}

	/**
	 * Extend floppy disk strategy (&gt; 35 tracks)
	 *
	 * @param policy extension policy
	 */
	public final void setExtendImagePolicy(IExtendImageListener policy) {
		this.policy = policy;
	}

	/**
	 * Insert a disk into the first floppy disk drive.
	 *
	 * @param file disk file to insert
	 * @throws IOException image read error
	 */
	public final void insertDisk(final File file) throws IOException {
		if (config.getEmulationSection().getUltimate64Mode() != Ultimate64Mode.OFF) {
			sendInsertDisk(config, file);
		}
		config.getSidplay2Section().setLastDirectory(file.getParentFile());
		if (config.getEmulationSection().getUltimate64Mode() != Ultimate64Mode.STANDALONE) {
			// automatically turn drive on
			config.getC1541Section().setDriveOn(true);
			enableFloppyDiskDrives(true);
			// attach selected disk into the first disk drive
			DiskImage disk = floppies[0].getDiskController().insertDisk(file);
			if (policy != null) {
				disk.setExtendImagePolicy(policy);
			}
			installHack(file);
		}
	}

	private void installHack(File file) {
		try {
			MessageDigest messageDigest = MessageDigest.getInstance("MD5");
			try (BufferedInputStream is = new BufferedInputStream(new FileInputStream(file));
					DigestInputStream dis = new DigestInputStream(is, messageDigest)) {
				// read the file and update the hash calculation
				while (dis.read() != -1) {
					;
				}
				// get the hash value as byte array
				byte[] digest = messageDigest.digest();
				boolean hack = Arrays.equals(digest, EOD_HACK) || Arrays.equals(digest, EOD_HACK2);
				if (hack) {
					System.err.println("Edge of Disgrace hack has been installed!");
				}
				c64.getCPU().setEODHack(hack);
			}
		} catch (Exception e) {
			System.err.println("Warning: installHack failed: " + e.getMessage());
		}
	}

	/**
	 * Insert a tape into the datasette.<BR>
	 * Note: If the file is different to the TAP format, it will be converted.
	 *
	 * @param file tape file to insert
	 * @throws IOException image read error
	 */
	public final void insertTape(final File file) throws IOException, SidTuneError {
		config.getSidplay2Section().setLastDirectory(file.getParentFile());
		if (!file.getName().toLowerCase(Locale.ENGLISH).endsWith(".tap")) {
			// Everything, which is not a tape convert to tape first
			final File tmpDir = config.getSidplay2Section().getTmpDir();
			final File convertedTape = new File(tmpDir, file.getName() + ".tap");
			convertedTape.deleteOnExit();
			SidTune prog = SidTune.load(file);
			String name = IOUtils.getFilenameWithoutSuffix(file.getName());
			PRG2TAPProgram program = new PRG2TAPProgram(prog, name);

			PRG2TAP prg2tap = new PRG2TAP();
			prg2tap.setTurboTape(config.getSidplay2Section().isTurboTape());
			prg2tap.open(convertedTape);
			prg2tap.add(program);
			prg2tap.close(convertedTape);

			datasette.insertTape(convertedTape);
		} else {
			datasette.insertTape(file);
		}
	}

	/**
	 * Insert a cartridge of a given size with empty contents.
	 *
	 * @param type   cartridge type
	 * @param sizeKB size in KB
	 * @throws IOException never thrown here
	 */
	public final void insertCartridge(final CartridgeType type, final int sizeKB) throws IOException {
		c64.ejectCartridge();
		c64.setCartridge(type, sizeKB);
	}

	/**
	 * Insert a cartridge loading an image file.
	 *
	 * @param type cartridge type
	 * @param file file to load the RAM contents
	 * @throws IOException image read error
	 */
	public final void insertCartridge(final CartridgeType type, final File file) throws IOException {
		config.getSidplay2Section().setLastDirectory(file.getParentFile());
		c64.ejectCartridge();
		c64.setCartridge(type, file);
	}

	/**
	 * Insert a cartridge of type CRT loading an image.
	 *
	 * @param is input stream to load the RAM contents
	 * @throws IOException image read error
	 */
	public final void insertCartridgeCRT(final InputStream is) throws IOException {
		config.getC1541Section().setJiffyDosInstalled(false);
		c64.ejectCartridge();
		c64.setCartridgeCRT(is);
	}

}
