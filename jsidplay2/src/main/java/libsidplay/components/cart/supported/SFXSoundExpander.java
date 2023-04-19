package libsidplay.components.cart.supported;

import java.io.DataInputStream;
import java.util.function.IntConsumer;

import libsidplay.common.CPUClock;
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

	private EventScheduler context;

	private FMOPL_072.FM_OPL fmOpl;

	private IntConsumer sampler;

	private long lastTime;

	public SFXSoundExpander(DataInputStream dis, PLA pla, int sizeKB) {
		super(pla);
		context = pla.getCPU().getEventScheduler();

		fmOpl = FMOPL_072.init(FMOPL_072.OPL_TYPE_YM3812, 3579545, (int) CPUClock.PAL.getCpuFrequency());
	}

	@Override
	public Bank getIO2() {
		return io2Bank;
	}

	@Override
	public void reset() {
		super.reset();
		pla.setGameExrom(true, true);

		FMOPL_072.reset_chip(fmOpl);
	}

	private final Bank io2Bank = new Bank() {

		@Override
		public byte read(int addr) {
			clock(clocksSinceLastAccess());
			return (byte) ((addr & 0xff) == 0x60 ? FMOPL_072.read(fmOpl, 0) : 0xff);
		}

		@Override
		public void write(int addr, byte val) {
			clock(clocksSinceLastAccess());
			FMOPL_072.write(fmOpl, (addr & 0xff) == 0x40 ? 0 : 1, val & 0xff);
		}
	};

	@Override
	public boolean isCreatingSamples() {
		return true;
	}

	@Override
	public void setSampler(IntConsumer sampler) {
		this.sampler = sampler;
	}

	@Override
	public void mixerStart() {
		clocksSinceLastAccess();
	}

	@Override
	public void clock() {
		clock(clocksSinceLastAccess());
	}

	public void clock(int cycles) {
		FMOPL_072.update_one(fmOpl, sampler, cycles);
	}

	private int clocksSinceLastAccess() {
		final long now = context.getTime(Event.Phase.PHI2);
		int diff = (int) (now - lastTime);
		lastTime = now;
		return diff;
	}

}
