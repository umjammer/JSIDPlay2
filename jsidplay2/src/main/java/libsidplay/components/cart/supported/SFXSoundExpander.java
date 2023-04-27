package libsidplay.components.cart.supported;

import java.io.DataInputStream;

import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.components.cart.Cartridge;
import libsidplay.components.cart.supported.core.FMOPL_072;
import libsidplay.components.pla.Bank;
import libsidplay.components.pla.PLA;

/**
 * 
 * @author ken
 *
 */
public class SFXSoundExpander extends Cartridge {

	private final int type;

	private final EventScheduler context;

	private FMOPL_072.FM_OPL fmOpl;

	private long lastTime;

	/**
	 * <B>sizeKB misused as type 0: OPL1 (YM3526), 1: OPL2 (YM3812)</B>
	 */
	public SFXSoundExpander(DataInputStream dis, PLA pla, int sizeKB) {
		super(pla);
		type = sizeKB;
		context = pla.getCPU().getEventScheduler();
	}

	@Override
	public Bank getIO2() {
		return io2Bank;
	}

	@Override
	public void reset() {
		super.reset();
		pla.setGameExrom(true, true);

		fmOpl = FMOPL_072.init(type, 3579545, (int) pla.getCpuClock().getCpuFrequency());
	}

	private final Bank io2Bank = new Bank() {

		@Override
		public byte read(int addr) {
			clock();
			return (byte) ((addr & 0xff) == 0x60 ? FMOPL_072.read(fmOpl, 0) : pla.getDisconnectedBusBank().read(addr));
		}

		@Override
		public void write(int addr, byte val) {
			clock();
			FMOPL_072.write(fmOpl, (addr & 0xff) == 0x40 ? 0 : 1, val & 0xff);
		}
	};

	@Override
	public boolean isCreatingSamples() {
		return true;
	}

	@Override
	public void mixerStart() {
		clocksSinceLastAccess();
	}

	@Override
	public void clock() {
		FMOPL_072.update_one(fmOpl, sampler, clocksSinceLastAccess());
	}

	private int clocksSinceLastAccess() {
		final long now = context.getTime(Event.Phase.PHI2);
		int diff = (int) (now - lastTime);
		lastTime = now;
		return diff;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + (type == 0 ? "OPL1" : "OPL2");
	}
}
