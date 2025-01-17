package libsidplay.components.cart;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import builder.resid.SampleMixer;
import builder.resid.SampleMixer.NoOpSampleMixer;
import libsidplay.common.Event;
import libsidplay.components.cart.supported.ActionReplay;
import libsidplay.components.cart.supported.AtomicPower;
import libsidplay.components.cart.supported.Comal80;
import libsidplay.components.cart.supported.EasyFlash;
import libsidplay.components.cart.supported.EpyxFastLoad;
import libsidplay.components.cart.supported.Expert;
import libsidplay.components.cart.supported.FinalV1;
import libsidplay.components.cart.supported.FinalV3;
import libsidplay.components.cart.supported.GMod2;
import libsidplay.components.cart.supported.GeoRAM;
import libsidplay.components.cart.supported.MagicDesk;
import libsidplay.components.cart.supported.MikroAss;
import libsidplay.components.cart.supported.Normal;
import libsidplay.components.cart.supported.OceanType1;
import libsidplay.components.cart.supported.REU;
import libsidplay.components.cart.supported.Rex;
import libsidplay.components.cart.supported.SFXSoundExpander;
import libsidplay.components.cart.supported.Zaxxon;
import libsidplay.components.pla.Bank;
import libsidplay.components.pla.PLA;

/**
 * Cartridge base class.
 *
 * @author Antti Lankila
 */
public class Cartridge {
	private static final Charset ISO88591 = Charset.forName("ISO-8859-1");

	/** CCS64 cartridge type map */
	public enum CRTType {
		NORMAL(0), ACTION_REPLAY(1), KCS_POWER_CARTRIDGE(2), FINAL_CARTRIDGE_III(3), SIMONS_BASIC(4), OCEAN_TYPE_1(5),
		EXPERT_CARTRIDGE(6), FUN_PLAY__POWER_PLAY(7),

		SUPER_GAMES(8), ATOMIC_POWER(9), EPYX_FASTLOAD(10), WESTERMANN_LEARNING(11), REX_UTILITY(12),
		FINAL_CARTRIDGE_I(13), MAGIC_FORMEL(14), C64_GAME_SYSTEM__SYSTEM_3(15),

		WARPSPEED(16), DINAMIC(17), ZAXXON__SUPER_ZAXXON(18), MAGIC_DESK__DOMARK__HES_AUSTRALIA(19),
		SUPER_SNAPSHOT_5(20), COMAL_80(21), STRUCTURED_BASIC(22), ROSS(23),

		DELA_EP64(24), DELA_EP7X8(25), DELA_EP256(26), REX_EP256(27), MIKRO_ASSEMBLER(28), FINAL_PLUS(29),
		ACTION_REPLAY_4(30), STARDOS(31),

		EASYFLASH(32), GMOD2(60);

		private int magic;

		private CRTType(int magic) {
			this.magic = magic;
		}

		public static CRTType getType(byte[] header) {
			if (!new String(header, 0, 0x10, ISO88591).equals("C64 CARTRIDGE   ")) {
				throw new RuntimeException("File is not a .CRT file");
			}
			CRTType cart = getType((header[0x16] & 0xff) << 8 | header[0x17] & 0xff);
			if (cart == null) {
				throw new RuntimeException("Cartridge magic value: "
						+ ((header[0x16] & 0xff) << 8 | header[0x17] & 0xff) + " unsupported!");
			}
			return cart;
		}

		private static CRTType getType(int magic) {
			return Arrays.asList(values()).stream().filter(crt -> crt.magic == magic).findFirst().orElse(null);
		}
	}

	protected Cartridge(final PLA pla) {
		this.pla = pla;
	}

	/**
	 * Instance of the system's PLA chip.
	 */
	public final PLA pla;

	/** Current state of cartridge-asserted NMI */
	private boolean nmiState;

	/** Current state of cartridge-asserted IRQ */
	private boolean irqState;

	/** Consumes samples of the cartridge while clocking. */
	protected SampleMixer sampler = new NoOpSampleMixer();

	/**
	 * Get currently active ROML bank.
	 *
	 * @return ROML bank
	 */
	public Bank getRoml() {
		return pla.getDisconnectedBusBank();
	}

	/**
	 * Get currently active ROMH bank.
	 *
	 * @return ROMH bank
	 */
	public Bank getRomh() {
		return pla.getDisconnectedBusBank();
	}

	/**
	 * In Ultimax mode, the main memory between 0x1000-0xffff is disconnected. This
	 * allows carts to export their own memory for those regions, excluding the
	 * areas that will be mapped to ROML, IO and ROMH, though.
	 *
	 * @return Memory bank for Ultimax mode
	 */
	public Bank getUltimaxMemory() {
		return pla.getDisconnectedBusBank();
	}

	/**
	 * Acquire the IO1 bank
	 *
	 * @return The bank responding to IO1 line.
	 */
	public Bank getIO1() {
		return pla.getDisconnectedBusBank();
	}

	/**
	 * Acquire the IO2 bank.
	 *
	 * @return The bank responding to IO2 line.
	 */
	public Bank getIO2() {
		return pla.getDisconnectedBusBank();
	}

	/**
	 * Create a cartridge.
	 *
	 * @param pla      Instance of the system's PLA chip
	 * @param cartType cartridge type
	 * @param sizeKB   size in KB
	 * @return a cartridge instance
	 */
	public static final Cartridge create(final PLA pla, final CartridgeType cartType, final int sizeKB)
			throws IOException {
		switch (cartType) {
		case GEORAM:
			return new GeoRAM(null, pla, sizeKB);
		case REU:
			return new REU(null, pla, sizeKB);
		case SOUNDEXPANDER:
			return new SFXSoundExpander(null, pla, sizeKB);
		default:
			throw new RuntimeException("Cartridge is unsupported");
		}
	}

	/**
	 * Load a cartridge.
	 *
	 * @param pla      Instance of the system's PLA chip
	 * @param cartType cartridge type
	 * @param file     file to load from
	 * @return a cartridge instance
	 */
	public static Cartridge read(final PLA pla, final CartridgeType cartType, final File file) throws IOException {
		try (final DataInputStream dis = new DataInputStream(new FileInputStream(file))) {
			switch (cartType) {
			case GEORAM:
				return new GeoRAM(dis, pla, (int) (file.length() >> 10));
			case REU:
				return new REU(dis, pla, (int) (file.length() >> 10));
			case CRT:
				return readCRT(pla, dis);
			default:
				throw new RuntimeException("Cartridge unsupported");
			}
		}
	}

	/**
	 * Load a cartridge of type CRT.
	 *
	 * @param pla Instance of the system's PLA chip
	 * @param is  input stream to load from
	 * @return a cartridge instance
	 */
	public static Cartridge readCRT(final PLA pla, final DataInputStream is) throws IOException {
		final byte[] header = new byte[0x40];
		is.readFully(header);

		final CRTType type = CRTType.getType(header);
		switch (type) {
		case ACTION_REPLAY:
			return new ActionReplay(is, pla);
		case NORMAL:
			return new Normal(is, pla);
		case FINAL_CARTRIDGE_III:
			return new FinalV3(is, pla);
		case EXPERT_CARTRIDGE:
			return new Expert(is, pla);
		case ATOMIC_POWER:
			return new AtomicPower(is, pla);
		case EPYX_FASTLOAD:
			return new EpyxFastLoad(is, pla);
		case REX_UTILITY:
			return new Rex(is, pla);
		case FINAL_CARTRIDGE_I:
			return new FinalV1(is, pla);
		case ZAXXON__SUPER_ZAXXON:
			return new Zaxxon(is, pla);
		case COMAL_80:
			return new Comal80(is, pla);
		case MIKRO_ASSEMBLER:
			return new MikroAss(is, pla);
		case EASYFLASH:
			return new EasyFlash(is, pla);
		case MAGIC_DESK__DOMARK__HES_AUSTRALIA:
			return new MagicDesk(is, pla);
		case OCEAN_TYPE_1:
			return new OceanType1(is, pla);
		case GMOD2:
			return new GMod2(is, pla);
		default:
			throw new RuntimeException("Cartridges of format: " + type + " unsupported");
		}
	}

	/**
	 * If the cartridge needs to listen to write activity on specific banks, it can
	 * install the requisite hooks into the bank here.
	 *
	 * @param cpuReadMap
	 * @param cpuWriteMap
	 */
	public void installBankHooks(Bank[] cpuReadMap, Bank[] cpuWriteMap) {
	}

	/**
	 * Return an instance of cartridge when no real cartridge is connected.
	 *
	 * @return the null cartridge
	 */
	public static Cartridge nullCartridge(final PLA pla) {
		return new Cartridge(pla) {
			@Override
			public String toString() {
				return "";
			}
		};
	}

	/**
	 * Bring the cart to power-on state. If overridden, remember to call the
	 * superclass method.
	 */
	public void reset() {
		nmiState = false;
		irqState = false;
	}

	/**
	 * Push cartridge's "freeze" button.
	 *
	 * Because this is an UI-method, we use thread-safe scheduling to delay the
	 * freezing to occur at some safe later time.
	 *
	 * Subclasses need to override doFreeze().
	 */
	public final void freeze() {
		pla.getCPU().getEventScheduler().scheduleThreadSafe(Event.of("Freeze TS", event -> Cartridge.this.doFreeze()));
	}

	/**
	 * Handle pressing of the freeze button.
	 */
	protected void doFreeze() {
	}

	/**
	 * Callback to notify cartridge of current state of NMI signal on the system
	 * bus. The boolean value is active high.
	 *
	 * @param state
	 */
	public void changedNMI(boolean state) {
	}

	/**
	 * Callback to notify cartridge of current state of IRQ signal on the system
	 * bus. The boolean value is active high.
	 *
	 * @param state
	 */
	public void changedIRQ(boolean state) {
	}

	/**
	 * Callback to notify cartridge of current state of BA signal on the system bus.
	 * The boolean value is active high.
	 *
	 * @param state
	 */
	public void changedBA(boolean state) {
	}

	/**
	 * Assert NMI (= electrically pull NMI low) on the system bus. The boolean value
	 * is active high. Method is meant for subclasses only.
	 *
	 * @param state
	 */
	public void setNMI(boolean state) {
		if (state ^ nmiState) {
			pla.setNMI(state);
			nmiState = state;
		}
	}

	/**
	 * Assert IRQ (= electrically pull IRQ low) on the system bus. The boolean value
	 * is active high. Method is meant for subclasses only.
	 *
	 * @param state
	 */
	public void setIRQ(boolean state) {
		if (state ^ irqState) {
			pla.setIRQ(state);
			irqState = state;
		}
	}

	/**
	 * @return is catridge producing sound?
	 */
	public boolean isCreatingSamples() {
		return false;
	}

	/**
	 * Set sampler to put cartridge sound samples into the mix
	 * 
	 * @param sampler
	 */
	public void setSampler(SampleMixer sampler) {
		this.sampler = sampler;
	}

	public SampleMixer getSampler() {
		return sampler;
	}

	/**
	 * Mixer starts mixing
	 */
	public void mixerStart() {
	}

	/**
	 * Clock cartridge chips
	 */
	public void clock() {
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

}