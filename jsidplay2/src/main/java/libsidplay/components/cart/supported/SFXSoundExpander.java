package libsidplay.components.cart.supported;

import java.io.DataInputStream;
import java.nio.Buffer;
import java.util.function.IntConsumer;

import javax.sound.sampled.LineUnavailableException;

import builder.resid.resample.Resampler;
import libsidplay.common.CPUClock;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SamplingMethod;
import libsidplay.common.SamplingRate;
import libsidplay.components.cart.Cartridge;
import libsidplay.components.cart.supported.core.FMOPL;
import libsidplay.components.cart.supported.core.FMOPL.FmOPL;
import libsidplay.components.pla.Bank;
import libsidplay.components.pla.PLA;
import sidplay.audio.AudioConfig;
import sidplay.audio.JavaSound;

public class SFXSoundExpander extends Cartridge {

	private static final int BUFFER_SIZE = 16384;

	private static final int REGULAR_DELAY = 16384;

	private Resampler resamplerL;

	/* Flag: What type of ym chip is used? */
	private int sfx_soundexpander_chip = 3526;
//	private int sfx_soundexpander_chip = 3812;

	private int sfx_soundexpander_sound_chip_offset = 96;

	private FMOPL fmOpl = new FMOPL() {

		@Override
		public long maincpu_clk() {
			return context.getTime(Event.Phase.PHI1);
		}

		@Override
		public void alarm_unset(Event event) {
			context.cancel(event);

		}

		@Override
		public void alarm_set(Event event, long start) {
			context.schedule(event, start);
		}

	};

	private FmOPL YM3526_chip = null;
	private FmOPL YM3812_chip = null;

	private EventScheduler context;

	private int clock;

	private JavaSound javaSound = new JavaSound();

	public SFXSoundExpander(DataInputStream dis, PLA pla, int sizeKB) {
		super(pla);
		this.context = pla.getCPU().getEventScheduler();
	}

	@Override
	public Bank getIO2() {
		return io2Bank;
	}

	@Override
	public void reset() {
		super.reset();
		pla.setGameExrom(true, true);
		clocksSinceLastAccess();
		context.cancel(event);
		context.schedule(event, 0, Event.Phase.PHI2);
		/* master clock (Hz) **/
		this.clock = (int) CPUClock.PAL.getCpuFrequency();
		init();
		resamplerL = Resampler.createResampler(clock, SamplingMethod.DECIMATE, SamplingRate.MEDIUM.getFrequency(),
				SamplingRate.MEDIUM.getMiddleFrequency());
		try {
			javaSound.open(new AudioConfig(SamplingRate.MEDIUM.getFrequency(), 2, BUFFER_SIZE), null);
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
		sfx_soundexpander_sound_reset();
	}

	private final Bank io2Bank = new Bank() {

		@Override
		public byte read(int address) {
			return pla.getDisconnectedBusBank().read(address);
		}

		@Override
		public void write(int address, byte value) {
			clock();
			sfx_soundexpander_sound_store(address, value);
		}
	};

	private final Event event = new Event("Delay") {
		@Override
		public void event() {
			context.schedule(event, eventuallyDelay(), Event.Phase.PHI2);
		}
	};

	public void clock() {
		int cycles = clocksSinceLastAccess();
		sfx_soundexpander_sound_machine_calculate_samples(sample -> {
			if (resamplerL.input(sample)) {
				if (!javaSound.buffer().putShort((short) resamplerL.output()).hasRemaining()) {
					try {
						javaSound.write();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					((Buffer) javaSound.buffer()).clear();
				}
			}
		}, cycles);
	}

	/* ------------------------------------------------------------------------- */
// VERIFIED
	private void init() {
		fmOpl.fmopl_set_machine_parameter(clock);

		if (sfx_soundexpander_chip == 3812) {
			if (YM3812_chip != null) {
				fmOpl.ym3812_shutdown(YM3812_chip);
			}
			YM3812_chip = fmOpl.ym3812_init(3579545, clock);
		} else {
			if (YM3526_chip != null) {
				fmOpl.ym3526_shutdown(YM3526_chip);
			}
			YM3526_chip = fmOpl.ym3526_init(3579545, clock, context);
		}
	}

	public void sfx_soundexpander_sound_reset() {
		if (sfx_soundexpander_chip == 3812 && YM3812_chip != null) {
			fmOpl.ym3812_reset_chip(YM3812_chip);
		} else if (sfx_soundexpander_chip == 3526 && YM3526_chip != null) {
			fmOpl.ym3526_reset_chip(YM3526_chip, context);
		}
	}

	public void sfx_soundexpander_sound_machine_calculate_samples(IntConsumer sampleBuffer, int samples) {
		if (sfx_soundexpander_chip == 3812 && YM3812_chip != null) {
			fmOpl.ym3812_update_one(YM3812_chip, sampleBuffer, samples);
		} else if (sfx_soundexpander_chip == 3526 && YM3526_chip != null) {
			fmOpl.ym3526_update_one(YM3526_chip, sampleBuffer, samples, context);
		}
	}

	public void sfx_soundexpander_sound_machine_close() {
		if (YM3526_chip != null) {
			fmOpl.ym3526_shutdown(YM3526_chip);
			YM3526_chip = null;
		}
		if (YM3812_chip != null) {
			fmOpl.ym3812_shutdown(YM3812_chip);
			YM3812_chip = null;
		}
	}

	/* --------------------------------------------------------------------- */

	public void sfx_soundexpander_sound_machine_store(int addr, byte val) {
		if (sfx_soundexpander_chip == 3812 && YM3812_chip != null) {
			fmOpl.ym3812_write(YM3812_chip, 1, val);
		} else if (sfx_soundexpander_chip == 3526 && YM3526_chip != null) {
			fmOpl.ym3526_write(YM3526_chip, 1, val, context);
		}
	}

	public int sfx_soundexpander_sound_machine_read(int addr) {
		if (sfx_soundexpander_chip == 3812 && YM3812_chip != null) {
			return fmOpl.ym3812_read(YM3812_chip, 1);
		}
		if (sfx_soundexpander_chip == 3526 && YM3526_chip != null) {
			return fmOpl.ym3526_read(YM3526_chip, 1);
		}
		return 0;
	}

	/* --------------------------------------------------------------------- */

	public void sfx_soundexpander_sound_store(int addr, byte value) {
		addr = (addr & 0xff);
		if (addr == 0x40) {
			if (sfx_soundexpander_chip == 3812 && YM3812_chip != null) {
				fmOpl.ym3812_write(YM3812_chip, 0, value);
			} else if (sfx_soundexpander_chip == 3526 && YM3526_chip != null) {
				fmOpl.ym3526_write(YM3526_chip, 0, value, context);
			}
		}
		if (addr == 0x50) {
			sfx_soundexpander_sound_machine_store(sfx_soundexpander_sound_chip_offset & 0x1f, value);
			// sound_write(sfx_soundexpander_sound_chip_offset & 0x1f, value);
		}
	}

	public int sfx_soundexpander_sound_read(int addr) {
		addr = (addr & 0xff);
		int value = 0;

		if (addr == 0x60) {
			if ((sfx_soundexpander_chip == 3812 && YM3812_chip != null)
					|| (sfx_soundexpander_chip == 3526 && YM3526_chip != null)) {
				value = sfx_soundexpander_sound_read(sfx_soundexpander_sound_chip_offset & 0x1f);
				// value = sound_read(sfx_soundexpander_sound_chip_offset & 0x1f, 0);
			}
		}
		return value;
	}

	public int sfx_soundexpander_sound_peek(int addr) {
		int value = 0;

		if (addr == 0x40) {
			if (sfx_soundexpander_chip == 3812 && YM3812_chip != null) {
				value = fmOpl.ym3812_peek(YM3812_chip, value);
			} else if (sfx_soundexpander_chip == 3526 && YM3526_chip != null) {
				value = fmOpl.ym3526_peek(YM3526_chip, value);
			}
		}
		return value;
	}

	/* --------------------------------------------------------------------- */

	/* No piano keyboard is emulated currently, so we return 0xff */
	public byte sfx_soundexpander_piano_read(int addr) {
		return (byte) 0xff;
	}

	private long lastTime;

	protected int clocksSinceLastAccess() {
		final long now = context.getTime(Event.Phase.PHI2);
		int diff = (int) (now - lastTime);
		lastTime = now;
		return diff;
	}

	long eventuallyDelay() {
		final long now = context.getTime(Event.Phase.PHI2);
		int diff = (int) (now - lastTime);
		if (diff > REGULAR_DELAY) {
			lastTime += REGULAR_DELAY;

			clock();
		}
		return REGULAR_DELAY;
	}

}
