package libsidplay.components.cart.supported;

import java.io.DataInputStream;
import java.nio.Buffer;

import javax.sound.sampled.LineUnavailableException;

import builder.resid.resample.Resampler;
import libsidplay.common.CPUClock;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SamplingMethod;
import libsidplay.common.SamplingRate;
import libsidplay.components.cart.Cartridge;
import libsidplay.components.cart.supported.core.FMOPL_072;
import libsidplay.components.pla.Bank;
import libsidplay.components.pla.PLA;
import sidplay.audio.AudioConfig;
import sidplay.audio.JavaSound;

/**
 * 
 * @author ken
 *
 */
public class SFXSoundExpanderFmOPL extends Cartridge {

	private static final int BUFFER_SIZE = 8192;

	private static final int REGULAR_DELAY = BUFFER_SIZE << 1;

	private EventScheduler context;

	private Resampler resamplerL;

	private JavaSound javaSound = new JavaSound();

	private long lastTime;

	private FMOPL_072.FM_OPL opl3;

	public SFXSoundExpanderFmOPL(DataInputStream dis, PLA pla, int sizeKB) {
		super(pla);
		context = pla.getCPU().getEventScheduler();

		opl3 = FMOPL_072.init(FMOPL_072.OPL_TYPE_YM3812, 3579545, (int) CPUClock.PAL.getCpuFrequency());
		resamplerL = Resampler.createResampler(CPUClock.PAL.getCpuFrequency(), SamplingMethod.RESAMPLE,
				SamplingRate.MEDIUM.getFrequency(), SamplingRate.MEDIUM.getMiddleFrequency());
		try {
			javaSound.open(new AudioConfig(SamplingRate.MEDIUM.getFrequency(), 2, BUFFER_SIZE), null);
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
	}

	@Override
	public Bank getIO2() {
		return io2Bank;
	}

	@Override
	public void reset() {
		super.reset();
		pla.setGameExrom(true, true);

		FMOPL_072.reset_chip(opl3);

		clocksSinceLastAccess();
		context.cancel(event);
		context.schedule(event, 0, Event.Phase.PHI2);
	}

	private final Bank io2Bank = new Bank() {

		@Override
		public byte read(int addr) {
			clock(clocksSinceLastAccess());
			return (byte) ((addr & 0xff) == 0x60 ? FMOPL_072.read(opl3, 0) : 0xff);
		}

		@Override
		public void write(int addr, byte val) {
			clock(clocksSinceLastAccess());
			FMOPL_072.write(opl3, (addr & 0xff) == 0x40 ? 0 : 1, val & 0xff);
		}
	};

	public void clock(int cycles) {
		int[] shortsLeft = new int[cycles];
		FMOPL_072.update_one(opl3, shortsLeft, cycles);
		for (int i = 0; i < shortsLeft.length; i++) {
			if (resamplerL.input(shortsLeft[i])) {
				javaSound.buffer().putShort((short) resamplerL.output());
				if (!javaSound.buffer().putShort((short) resamplerL.output()).hasRemaining()) {
					try {
						javaSound.write();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					((Buffer) javaSound.buffer()).clear();
				}
			}
		}
	}

	protected int clocksSinceLastAccess() {
		final long now = context.getTime(Event.Phase.PHI2);
		int diff = (int) (now - lastTime);
		lastTime = now;
		return diff;
	}

	private final Event event = new Event("SFX Regular Delay") {
		@Override
		public void event() {
			context.schedule(event, eventuallyDelay(), Event.Phase.PHI2);
		}
	};

	private long eventuallyDelay() {
		final long now = context.getTime(Event.Phase.PHI2);
		int diff = (int) (now - lastTime);
		if (diff > REGULAR_DELAY) {
			lastTime += REGULAR_DELAY;

			clock(REGULAR_DELAY);
		}
		return REGULAR_DELAY;
	}

	@Override
	public boolean isMultiPurpose() {
		return false;
	}
}
