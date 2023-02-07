package libsidplay.components.cart.supported;

import java.util.function.IntConsumer;

import libsidplay.common.CPUClock;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.components.cart.supported.core.FMOPL;
import libsidplay.components.cart.supported.core.FMOPL.FmOPL;

@SuppressWarnings("unused")
public class SFXSoundExpander {

	/* Flag: What type of ym chip is used? */
	private int sfx_soundexpander_chip = 3526;

	private int sfx_soundexpander_sound_chip_offset = 0;

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

	private FmOPL YM3526_chip = null;
	private FmOPL YM3812_chip = null;

	int sfx_soundexpander_io_swap = 0;

	private EventScheduler context;

	public SFXSoundExpander(EventScheduler context, CPUClock clock) {
		this.context = context;
		sfx_soundexpander_sound_machine_init((int) clock.getScreenRefresh());
	}

	/* ------------------------------------------------------------------------- */

	public void sfx_soundexpander_sound_machine_calculate_samples(IntConsumer sampleBuffer, int samples) {
		if (sfx_soundexpander_chip == 3812 && YM3812_chip != null) {
			fmOpl.ym3812_update_one(YM3812_chip, sampleBuffer, samples);
		} else if (sfx_soundexpander_chip == 3526 && YM3526_chip != null) {
			fmOpl.ym3526_update_one(YM3526_chip, sampleBuffer, samples);
		}
	}

	private int sfx_soundexpander_sound_machine_init(int speed) {
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
//		snd.command = 0;

		return 1;
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

	public void sfx_soundexpander_sound_reset(CPUClock cpu_clk) {
		if (sfx_soundexpander_chip == 3812 && YM3812_chip != null) {
			fmOpl.ym3812_reset_chip(YM3812_chip);
		} else if (sfx_soundexpander_chip == 3526 && YM3526_chip != null) {
			fmOpl.ym3526_reset_chip(YM3526_chip);
		}
	}

	/* --------------------------------------------------------------------- */

	private void sfx_soundexpander_sound_store(int addr, int value) {
		if (addr == 0x40) {
			if (sfx_soundexpander_chip == 3812 && YM3812_chip != null) {
				fmOpl.ym3812_write(YM3812_chip, 0, value);
			} else if (sfx_soundexpander_chip == 3526 && YM3526_chip != null) {
				fmOpl.ym3526_write(YM3526_chip, 0, value);
			}
		}
		if (addr == 0x50) {
//			sound_store(sfx_soundexpander_sound_chip_offset, value, 0);
		}
	}

	private int sfx_soundexpander_sound_read(int addr) {
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

	private int sfx_soundexpander_sound_peek(int addr) {
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

	/* No piano keyboard is emulated currently, so we return 0xff */
	private byte sfx_soundexpander_piano_read(int addr) {
//		sfx_soundexpander_piano_device.io_source_valid = 0;
		if ((addr & 16) == 0 && (addr & 8) == 8) {
//			sfx_soundexpander_piano_device.io_source_valid = 1;
		}
		return (byte) 0xff;
	}

}
