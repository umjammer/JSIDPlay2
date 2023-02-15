package libsidplay.components.cart.supported;

import java.io.DataInputStream;
import java.nio.Buffer;
import java.util.function.IntConsumer;

import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.CPUClock;
import libsidplay.common.Event;
import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;
import libsidplay.components.cart.Cartridge;
import libsidplay.components.cart.supported.core.FMOPL;
import libsidplay.components.cart.supported.core.FMOPL.FmOPL;
import libsidplay.components.pla.Bank;
import libsidplay.components.pla.PLA;
import sidplay.audio.AudioConfig;
import sidplay.audio.JavaSound;

public class SFXSoundExpander extends Cartridge {

	private static final int BUFFER_SIZE = 8;
	/* Flag: What type of ym chip is used? */
	private int sfx_soundexpander_chip = 3526;

//	private int sfx_soundexpander_sound_chip_offset = 0;

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

		@Override
		public void alarm_destroy(Event event) {
		}
	};

	private final class SFXSoundExpanderMixerEvent extends Event {

		private SFXSoundExpanderMixerEvent(String name) {
			super(name);
		}

		@Override
		public void event() throws InterruptedException {
			clock();
			context.schedule(this, BUFFER_SIZE);
		}
	}

	public void clock() {
		int cycles = clocksSinceLastAccess();
		sfx_soundexpander_sound_machine_calculate_samples(sample -> {
			// SOUND OUTPUT
			if (!javaSound.buffer().putShort((short) sample).hasRemaining()) {
				try {
					javaSound.write();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				((Buffer) javaSound.buffer()).clear();
			}

		}, cycles);
	}

	/**
	 * Last time chip was accessed.
	 */
	protected long lastTime;

	protected int clocksSinceLastAccess() {
		final long now = context.getTime(Event.Phase.PHI2);
		int diff = (int) (now - lastTime);
		lastTime = now;
		return diff;
	}

	private FmOPL YM3526_chip = null;
	private FmOPL YM3812_chip = null;

	int sfx_soundexpander_io_swap = 0;

	private EventScheduler context;

	private long clock;
	private int rate;

	private JavaSound javaSound = new JavaSound();

	/**
	 * Mixer clocking SID chips and producing audio output.
	 */
	private final SFXSoundExpanderMixerEvent mixerAudio = new SFXSoundExpanderMixerEvent("SFXExpanderAudio");

	public SFXSoundExpander(DataInputStream dis, PLA pla, int sizeKB) {
		super(pla);
		this.context = pla.getCPU().getEventScheduler();
		// TODO
		/* master clock (Hz) **/
		this.clock = 985248;
		/* sampling rate (Hz) **/
		this.rate = 44100;

		init();

		try {
			javaSound.close();
			AudioConfig audioConfig = new AudioConfig(44100, 2, BUFFER_SIZE);
			javaSound.open(audioConfig, null);
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		lastTime = context.getTime(Phase.PHI2);
		clocksSinceLastAccess();
		context.schedule(mixerAudio, 0, Event.Phase.PHI2);
	}

	@Override
	public Bank getIO2() {
		return io2Bank;
	}

	@Override
	public void reset() {
		super.reset();
		pla.setGameExrom(false, false);
	}

	private final Bank io2Bank = new Bank() {

		@Override
		public byte read(int address) {
			clock();
			return pla.getDisconnectedBusBank().read(address);
		}

		@Override
		public void write(int address, byte value) {
			clock();
			sfx_soundexpander_sound_store(address, value);
		}
	};

	/* ------------------------------------------------------------------------- */

	private void init() {
		fmOpl.fmopl_set_machine_parameter(clock);

		int speed = rate;
		if (sfx_soundexpander_chip == 3812) {
			if (YM3812_chip != null) {
				fmOpl.ym3812_shutdown(YM3812_chip);
			}
			YM3812_chip = fmOpl.ym3812_init(3579545, speed);
		} else {
			if (YM3526_chip != null) {
				fmOpl.ym3526_shutdown(YM3526_chip);
			}
			YM3526_chip = fmOpl.ym3526_init(3579545, speed);
		}
	}

	public void sfx_soundexpander_sound_reset(CPUClock cpu_clk) {
		if (sfx_soundexpander_chip == 3812 && YM3812_chip != null) {
			fmOpl.ym3812_reset_chip(YM3812_chip);
		} else if (sfx_soundexpander_chip == 3526 && YM3526_chip != null) {
			fmOpl.ym3526_reset_chip(YM3526_chip);
		}
	}

	public void sfx_soundexpander_sound_machine_calculate_samples(IntConsumer sampleBuffer, int samples) {
		if (sfx_soundexpander_chip == 3812 && YM3812_chip != null) {
			fmOpl.ym3812_update_one(YM3812_chip, sampleBuffer, samples);
		} else if (sfx_soundexpander_chip == 3526 && YM3526_chip != null) {
			fmOpl.ym3526_update_one(YM3526_chip, sampleBuffer, samples);
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

	public void sfx_soundexpander_sound_machine_store(int addr, int val) {
		if (sfx_soundexpander_chip == 3812 && YM3812_chip != null) {
			fmOpl.ym3812_write(YM3812_chip, 1, val);
		} else if (sfx_soundexpander_chip == 3526 && YM3526_chip != null) {
			fmOpl.ym3526_write(YM3526_chip, 1, val);
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

	public void sfx_soundexpander_sound_store(int addr, int value) {
		if (addr == 0x40) {
			if (sfx_soundexpander_chip == 3812 && YM3812_chip != null) {
				fmOpl.ym3812_write(YM3812_chip, 0, value);
			} else if (sfx_soundexpander_chip == 3526 && YM3526_chip != null) {
				fmOpl.ym3526_write(YM3526_chip, 0, value);
			}
		}
		if (addr == 0x50) {
			sfx_soundexpander_sound_machine_store(addr, value);
			// TODO really?
//			sound_store(sfx_soundexpander_sound_chip_offset, value, 0);
		}
	}

	public int sfx_soundexpander_sound_read(int addr) {
		int value = 0;

//		sfx_soundexpander_sound_device.io_source_valid = 0;

		if (addr == 0x60) {
			if ((sfx_soundexpander_chip == 3812 && YM3812_chip != null)
					|| (sfx_soundexpander_chip == 3526 && YM3526_chip != null)) {
//				sfx_soundexpander_sound_device.io_source_valid = 1;
//				value = sound_read(sfx_soundexpander_sound_chip_offset, 0);
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

}
