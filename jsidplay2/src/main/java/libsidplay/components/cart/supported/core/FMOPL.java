package libsidplay.components.cart.supported.core;

import java.util.function.IntConsumer;

import libsidplay.common.Event;
import libsidplay.common.EventScheduler;

/**
 *
 * File: fmopl.c - software implementation of FM sound generator types OPL and
 * OPL2
 *
 * license:GPL-2.0+
 *
 * Copyright Jarek Burczynski (bujar at mame dot net) Copyright Tatsuyuki Satoh
 * , MultiArcadeMachineEmulator development
 *
 * Version 0.72
 *
 * 
 * Adapted for use in VICE by Marco van den Heuvel
 * &lt;blackystardust68@yahoo.com&gt;
 *
 * XXX Unused and untested code!!!
 * 
 * @author ken
 *
 */
public abstract class FMOPL {

	private static final int FINAL_SH = 0;

	/* 16.16 fixed point (frequency calculations) */
	private static final int FREQ_SH = 16;
	/* 16.16 fixed point (EG timing) */
	private static final int EG_SH = 16;
	/* 8.24 fixed point (LFO calculations) */
	private static final int LFO_SH = 24;

	private static final int FREQ_MASK = ((1 << FREQ_SH) - 1);

	/* envelope output entries */
	private static final int ENV_BITS = 10;
	private static final int ENV_LEN = (1 << ENV_BITS);
	private static final double ENV_STEP = (128.0 / ENV_LEN);

	/* 511 */
	private static final int MAX_ATT_INDEX = ((1 << (ENV_BITS - 1)) - 1);
	private static final int MIN_ATT_INDEX = (0);

	/* sinwave entries */
	private static final int SIN_BITS = 10;
	private static final int SIN_LEN = (1 << SIN_BITS);
	private static final int SIN_MASK = (SIN_LEN - 1);

	/* 8 bits addressing (real chip) */
	private static final int TL_RES_LEN = (256);

	/* register number to channel number , slot offset */
	private static final int SLOT1 = 0;
	private static final int SLOT2 = 1;

	/* Envelope Generator phases */
	private static final int EG_ATT = 4;
	private static final int EG_DEC = 3;
	private static final int EG_SUS = 2;
	private static final int EG_REL = 1;
	private static final int EG_OFF = 0;

	/* waveform select */
	public static final int OPL_TYPE_WAVESEL = 0x01;
	/* DELTA-T ADPCM unit */
	public static final int OPL_TYPE_ADPCM = 0x02;
	/* keyboard interface */
	public static final int OPL_TYPE_KEYBOARD = 0x04;
	/* I/O port */
	public static final int OPL_TYPE_IO = 0x08;

	/* ---------- Generic interface section ---------- */
	private static final int OPL_TYPE_YM3526 = (0);
	private static final int OPL_TYPE_YM3812 = (OPL_TYPE_WAVESEL);

	/* mapping of register number (offset) to slot number used by the emulator */
	private static final int slot_array[] = new int[] { 0, 2, 4, 1, 3, 5, -1, -1, 6, 8, 10, 7, 9, 11, -1, -1, 12, 14,
			16, 13, 15, 17, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1 };

	/* key scale level */
	/* table is 3dB/octave , DV converts this into 6dB/octave */
	/*
	 * 0.1875 is bit 0 weight of the envelope counter (volume) expressed in the
	 * 'decibel' scale
	 */
	private static final double DV = (0.1875 / 2.0);

	private static final double ksl_tab[] = {
			/* OCT 0 */
			0.000 / DV, 0.000 / DV, 0.000 / DV, 0.000 / DV, 0.000 / DV, 0.000 / DV, 0.000 / DV, 0.000 / DV, 0.000 / DV,
			0.000 / DV, 0.000 / DV, 0.000 / DV, 0.000 / DV, 0.000 / DV, 0.000 / DV, 0.000 / DV,

			/* OCT 1 */
			0.000 / DV, 0.000 / DV, 0.000 / DV, 0.000 / DV, 0.000 / DV, 0.000 / DV, 0.000 / DV, 0.000 / DV, 0.000 / DV,
			0.750 / DV, 1.125 / DV, 1.500 / DV, 1.875 / DV, 2.250 / DV, 2.625 / DV, 3.000 / DV,

			/* OCT 2 */
			0.000 / DV, 0.000 / DV, 0.000 / DV, 0.000 / DV, 0.000 / DV, 1.125 / DV, 1.875 / DV, 2.625 / DV, 3.000 / DV,
			3.750 / DV, 4.125 / DV, 4.500 / DV, 4.875 / DV, 5.250 / DV, 5.625 / DV, 6.000 / DV,

			/* OCT 3 */
			0.000 / DV, 0.000 / DV, 0.000 / DV, 1.875 / DV, 3.000 / DV, 4.125 / DV, 4.875 / DV, 5.625 / DV, 6.000 / DV,
			6.750 / DV, 7.125 / DV, 7.500 / DV, 7.875 / DV, 8.250 / DV, 8.625 / DV, 9.000 / DV,

			/* OCT 4 */
			0.000 / DV, 0.000 / DV, 3.000 / DV, 4.875 / DV, 6.000 / DV, 7.125 / DV, 7.875 / DV, 8.625 / DV, 9.000 / DV,
			9.750 / DV, 10.125 / DV, 10.500 / DV, 10.875 / DV, 11.250 / DV, 11.625 / DV, 12.000 / DV,

			/* OCT 5 */
			0.000 / DV, 3.000 / DV, 6.000 / DV, 7.875 / DV, 9.000 / DV, 10.125 / DV, 10.875 / DV, 11.625 / DV,
			12.000 / DV, 12.750 / DV, 13.125 / DV, 13.500 / DV, 13.875 / DV, 14.250 / DV, 14.625 / DV, 15.000 / DV,

			/* OCT 6 */
			0.000 / DV, 6.000 / DV, 9.000 / DV, 10.875 / DV, 12.000 / DV, 13.125 / DV, 13.875 / DV, 14.625 / DV,
			15.000 / DV, 15.750 / DV, 16.125 / DV, 16.500 / DV, 16.875 / DV, 17.250 / DV, 17.625 / DV, 18.000 / DV,

			/* OCT 7 */
			0.000 / DV, 9.000 / DV, 12.000 / DV, 13.875 / DV, 15.000 / DV, 16.125 / DV, 16.875 / DV, 17.625 / DV,
			18.000 / DV, 18.750 / DV, 19.125 / DV, 19.500 / DV, 19.875 / DV, 20.250 / DV, 20.625 / DV, 21.000 / DV };

	/* sustain level table (3dB per step) */
	/* 0 - 15: 0, 3, 6, 9,12,15,18,21,24,27,30,33,36,39,42,93 (dB) */
	private static long SC(double db) {
		return (long) (db * (2.0 / ENV_STEP));
	}

	private static long sl_tab[];
	{
		sl_tab = new long[16];
		for (int i = 0; i < 15; i++) {
			sl_tab[i] = SC(i);
		}
		sl_tab[15] = SC(31);
	}

	private static final int RATE_STEPS = (8);

	private static final int eg_inc[] = {
			/* cycle: 0 1 2 3 4 5 6 7 */

			/* 0 */ 0, 1, 0, 1, 0, 1, 0, 1, /* rates 00..12 0 (increment by 0 or 1) */
			/* 1 */ 0, 1, 0, 1, 1, 1, 0, 1, /* rates 00..12 1 */
			/* 2 */ 0, 1, 1, 1, 0, 1, 1, 1, /* rates 00..12 2 */
			/* 3 */ 0, 1, 1, 1, 1, 1, 1, 1, /* rates 00..12 3 */

			/* 4 */ 1, 1, 1, 1, 1, 1, 1, 1, /* rate 13 0 (increment by 1) */
			/* 5 */ 1, 1, 1, 2, 1, 1, 1, 2, /* rate 13 1 */
			/* 6 */ 1, 2, 1, 2, 1, 2, 1, 2, /* rate 13 2 */
			/* 7 */ 1, 2, 2, 2, 1, 2, 2, 2, /* rate 13 3 */

			/* 8 */ 2, 2, 2, 2, 2, 2, 2, 2, /* rate 14 0 (increment by 2) */
			/* 9 */ 2, 2, 2, 4, 2, 2, 2, 4, /* rate 14 1 */
			/* 10 */ 2, 4, 2, 4, 2, 4, 2, 4, /* rate 14 2 */
			/* 11 */ 2, 4, 4, 4, 2, 4, 4, 4, /* rate 14 3 */

			/* 12 */ 4, 4, 4, 4, 4, 4, 4, 4, /* rates 15 0, 15 1, 15 2, 15 3 (increment by 4) */
			/* 13 */ 8, 8, 8, 8, 8, 8, 8, 8, /* rates 15 2, 15 3 for attack */
			/* 14 */ 0, 0, 0, 0, 0, 0, 0, 0, /* infinity rates for attack and decay(s) */
	};

	public static final int O(int a) {
		return (a * RATE_STEPS);
	};

	/* note that there is no O(13) in this table - it's directly in the code */
	public static int eg_rate_select[];
	{
		/* Envelope Generator rates (16 + 64 rates + 16 RKS) */
		eg_rate_select = new int[16 + 64 + 16];

		int off = 0;
		/* 16 infinite time rates */
		for (int i = 0; i < 16; i++) {
			eg_rate_select[off + i] = O(14);
		}
		off += 16;
		/* rates 00-12 */
		for (int i = 0; i < 13; i++) {
			for (int j = 0; j < 4; j++) {
				eg_rate_select[off + i * 4 + j] = O(j);
			}
		}
		off += 13 * 4;

		/* rate 13 */
		for (int i = 0; i < 4; i++) {
			eg_rate_select[off + i] = O(4 + i);
		}
		off += 4;

		/* rate 14 */
		for (int i = 0; i < 4; i++) {
			eg_rate_select[off + i] = O(8 + i);
		}
		off += 4;

		/* rate 15 */
		for (int i = 0; i < 4; i++) {
			eg_rate_select[off + i] = O(12);
		}
		off += 4;

		/* 16 dummy rates (same as 15 3) */
		for (int i = 0; i < 16; i++) {
			eg_rate_select[off + i] = O(12);
		}
	};

	/* rate 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 */
	/* shift 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1, 0, 0, 0, 0 */
	/* mask 4095, 2047, 1023, 511, 255, 127, 63, 31, 15, 7, 3, 1, 0, 0, 0, 0 */

	private static final int P(int a) {
		return (a * 1);
	}

	private static int eg_rate_shift[];
	{
		/* Envelope Generator counter shifts (16 + 64 rates + 16 RKS) */
		eg_rate_shift = new int[16 + 64 + 16];

		/* 16 infinite time rates */
		int off = 0;
		for (int i = 0; i < 16; i++) {
			eg_rate_shift[off + i] = P(0);
		}
		off += 16;

		/* rates 00-12 */
		for (int i = 0; i <= 12; i++) {
			for (int j = 0; j < 4; j++) {
				eg_rate_shift[off + i * 4 + j] = P(12 - i);
			}
		}
		off += 13 * 4;

		/* rate 13 */
		for (int i = 0; i < 4; i++) {
			eg_rate_shift[off + i] = P(0);
		}
		off += 4;

		/* rate 14 */
		for (int i = 0; i < 4; i++) {
			eg_rate_shift[off + i] = P(0);
		}
		off += 4;

		/* rate 15 */
		for (int i = 0; i < 4; i++) {
			eg_rate_shift[off + i] = P(0);
		}
		off += 4;

		/* 16 dummy rates (same as 15 3) */
		for (int i = 0; i < 16; i++) {
			eg_rate_shift[off + i] = P(0);
		}
	};

	/* multiple table */
	private static final int ML = 2;

	private static final double mul_tab[] = {
			/* 1/2, 1, 2, 3, 4, 5, 6, 7, 8, 9,10,10,12,12,15,15 */
			0.50 * ML, 1.00 * ML, 2.00 * ML, 3.00 * ML, 4.00 * ML, 5.00 * ML, 6.00 * ML, 7.00 * ML, 8.00 * ML,
			9.00 * ML, 10.00 * ML, 10.00 * ML, 12.00 * ML, 12.00 * ML, 15.00 * ML, 15.00 * ML };

	/*
	 * TL_TAB_LEN is calculated as: 12 - sinus amplitude bits (Y axis) 2 - sinus
	 * sign bit (Y axis) TL_RES_LEN - sinus resolution (X axis)
	 */
	private static final int TL_TAB_LEN = (12 * 2 * TL_RES_LEN);
	private static final int tl_tab[] = new int[TL_TAB_LEN];

	private static final int ENV_QUIET = (TL_TAB_LEN >> 4);

	/* sin waveform table in 'decibel' scale */
	/* four waveforms on OPL2 type chips */
	private static final int sin_tab[] = new int[SIN_LEN * 4];

	/*
	 * LFO Amplitude Modulation table (verified on real YM3812) 27 output levels
	 * (triangle waveform); 1 level takes one of: 192, 256 or 448 samples
	 * 
	 * Length: 210 elements.
	 * 
	 * Each of the elements has to be repeated exactly 64 times (on 64 consecutive
	 * samples). The whole table takes: 64 * 210 = 13440 samples.
	 * 
	 * When AM = 1 data is used directly When AM = 0 data is divided by 4 before
	 * being used (loosing precision is important)
	 */
	private static final long LFO_AM_TAB_ELEMENTS = 210;

	private static final int lfo_am_table[] = { 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 2, 2, 2, 2, 3, 3, 3, 3, 4, 4, 4, 4, 5,
			5, 5, 5, 6, 6, 6, 6, 7, 7, 7, 7, 8, 8, 8, 8, 9, 9, 9, 9, 10, 10, 10, 10, 11, 11, 11, 11, 12, 12, 12, 12, 13,
			13, 13, 13, 14, 14, 14, 14, 15, 15, 15, 15, 16, 16, 16, 16, 17, 17, 17, 17, 18, 18, 18, 18, 19, 19, 19, 19,
			20, 20, 20, 20, 21, 21, 21, 21, 22, 22, 22, 22, 23, 23, 23, 23, 24, 24, 24, 24, 25, 25, 25, 25, 26, 26, 26,
			25, 25, 25, 25, 24, 24, 24, 24, 23, 23, 23, 23, 22, 22, 22, 22, 21, 21, 21, 21, 20, 20, 20, 20, 19, 19, 19,
			19, 18, 18, 18, 18, 17, 17, 17, 17, 16, 16, 16, 16, 15, 15, 15, 15, 14, 14, 14, 14, 13, 13, 13, 13, 12, 12,
			12, 12, 11, 11, 11, 11, 10, 10, 10, 10, 9, 9, 9, 9, 8, 8, 8, 8, 7, 7, 7, 7, 6, 6, 6, 6, 5, 5, 5, 5, 4, 4, 4,
			4, 3, 3, 3, 3, 2, 2, 2, 2, 1, 1, 1, 1 };

	/* LFO Phase Modulation table (verified on real YM3812) */
	private static final byte lfo_pm_table[] = {
			/* FNUM2/FNUM = 00 0xxxxxxx (0x0000) */
			0, 0, 0, 0, 0, 0, 0, 0, /* LFO PM depth = 0 */
			0, 0, 0, 0, 0, 0, 0, 0, /* LFO PM depth = 1 */

			/* FNUM2/FNUM = 00 1xxxxxxx (0x0080) */
			0, 0, 0, 0, 0, 0, 0, 0, /* LFO PM depth = 0 */
			1, 0, 0, 0, -1, 0, 0, 0, /* LFO PM depth = 1 */

			/* FNUM2/FNUM = 01 0xxxxxxx (0x0100) */
			1, 0, 0, 0, -1, 0, 0, 0, /* LFO PM depth = 0 */
			2, 1, 0, -1, -2, -1, 0, 1, /* LFO PM depth = 1 */

			/* FNUM2/FNUM = 01 1xxxxxxx (0x0180) */
			1, 0, 0, 0, -1, 0, 0, 0, /* LFO PM depth = 0 */
			3, 1, 0, -1, -3, -1, 0, 1, /* LFO PM depth = 1 */

			/* FNUM2/FNUM = 10 0xxxxxxx (0x0200) */
			2, 1, 0, -1, -2, -1, 0, 1, /* LFO PM depth = 0 */
			4, 2, 0, -2, -4, -2, 0, 2, /* LFO PM depth = 1 */

			/* FNUM2/FNUM = 10 1xxxxxxx (0x0280) */
			2, 1, 0, -1, -2, -1, 0, 1, /* LFO PM depth = 0 */
			5, 2, 0, -2, -5, -2, 0, 2, /* LFO PM depth = 1 */

			/* FNUM2/FNUM = 11 0xxxxxxx (0x0300) */
			3, 1, 0, -1, -3, -1, 0, 1, /* LFO PM depth = 0 */
			6, 3, 0, -3, -6, -3, 0, 3, /* LFO PM depth = 1 */

			/* FNUM2/FNUM = 11 1xxxxxxx (0x0380) */
			3, 1, 0, -1, -3, -1, 0, 1, /* LFO PM depth = 0 */
			7, 3, 0, -3, -7, -3, 0, 3 /* LFO PM depth = 1 */
	};

	/* lock level of common table */
	private int num_lock = 0;

	private FmOPL cur_chip = null; /* current chip pointer */
	private OPLSlot SLOT7_1, SLOT7_2, SLOT8_1, SLOT8_2;

	private int phase_modulation; /* phase modulation input (SLOT 2) */
	private int output[] = new int[1];

	private int LFO_AM;
	private long LFO_PM;

	/* --------------------------------------------------------------------- */
	/* timer support functions */

	private long fmopl_timer_80 = 0;
	private long fmopl_timer_320 = 0;

//VERIFIED
	public void fmopl_set_machine_parameter(long clock_rate) {
		fmopl_timer_80 = (clock_rate * 80 / 1000000);
		fmopl_timer_320 = (clock_rate * 320 / 1000000);
	}

	private void fmopl_alarm_A(long offset, FmOPL OPL) {
		long new_start = /* maincpu_clk() - offset + */ ((256 - OPL.T[0]) * fmopl_timer_80);

		alarm_unset(OPL.fmopl_alarm[0]);
		alarm_set(OPL.fmopl_alarm[0], new_start);
		OPLTimerOver(OPL, 0);
	}

	private void fmopl_alarm_B(long offset, FmOPL OPL) {
		long new_start = /* maincpu_clk() - offset + */((256 - OPL.T[1]) * fmopl_timer_320);

		alarm_unset(OPL.fmopl_alarm[1]);
		alarm_set(OPL.fmopl_alarm[1], new_start);
		OPLTimerOver(OPL, 1);
	}

	/* --------------------------------------------------------------------- */

	/* status set and IRQ handling */
	private void OPL_STATUS_SET(FmOPL OPL, byte flag) {
		/* set status flag */
		OPL.status |= flag;
		if ((OPL.status & 0x80) == 0) {
			if ((OPL.status & OPL.statusmask) != 0) { /* IRQ on */
				OPL.status |= 0x80;
			}
		}
	}

	/* status reset and IRQ handling */
	private void OPL_STATUS_RESET(FmOPL OPL, byte flag) {
		/* reset status flag */
		OPL.status &= ~flag;
		if ((OPL.status & 0x80) != 0) {
			if ((OPL.status & OPL.statusmask) == 0) {
				OPL.status &= 0x7f;
			}
		}
	}

	/* IRQ mask set */
	private void OPL_STATUSMASK_SET(FmOPL OPL, byte flag) {
		OPL.statusmask = flag;

		/* IRQ handling check */
		OPL_STATUS_SET(OPL, (byte) 0);
		OPL_STATUS_RESET(OPL, (byte) 0);
	}

	/* advance LFO to next sample */
	private void advance_lfo(FmOPL OPL) {
		/* LFO */
		OPL.lfo_am_cnt += OPL.lfo_am_inc;
		if (OPL.lfo_am_cnt >= LFO_AM_TAB_ELEMENTS << LFO_SH) { /* lfo_am_table is 210 elements long */
			OPL.lfo_am_cnt -= LFO_AM_TAB_ELEMENTS << LFO_SH;
		}

		int tmp = lfo_am_table[(int) (OPL.lfo_am_cnt >> LFO_SH)];

		if ((OPL.lfo_am_depth) != 0) {
			LFO_AM = tmp;
		} else {
			LFO_AM = tmp >> 2;
		}

		OPL.lfo_pm_cnt += OPL.lfo_pm_inc;
		LFO_PM = (int) (((OPL.lfo_pm_cnt >> LFO_SH) & 7) | OPL.lfo_pm_depth_range);
	}

	/* advance to next sample */
	private void advance(FmOPL OPL) {
		OPL.eg_timer += OPL.eg_timer_add;

		while (OPL.eg_timer >= OPL.eg_timer_overflow) {
			OPL.eg_timer -= OPL.eg_timer_overflow;

			OPL.eg_cnt++;

			for (int i = 0; i < 9 * 2; i++) {
				OPLCh CH = OPL.pCh[i / 2];
				OPLSlot op = CH.slot[i & 1];

				/* Envelope Generator */
				switch (op.state) {
				case EG_ATT: /* attack phase */
					if ((OPL.eg_cnt & ((1 << op.eg_sh_ar) - 1)) == 0) {
						op.volume += (~op.volume
								* (eg_inc[(int) (op.eg_sel_ar + ((OPL.eg_cnt >> op.eg_sh_ar) & 7))])) >> 3;

						if (op.volume <= MIN_ATT_INDEX) {
							op.volume = MIN_ATT_INDEX;
							op.state = EG_DEC;
						}
					}
					break;
				case EG_DEC: /* decay phase */
					if ((OPL.eg_cnt & ((1 << op.eg_sh_dr) - 1)) == 0) {
						op.volume += eg_inc[(int) (op.eg_sel_dr + ((OPL.eg_cnt >> op.eg_sh_dr) & 7))];

						if ((op.volume) >= op.sl) {
							op.state = EG_SUS;
						}
					}
					break;
				case EG_SUS: /* sustain phase */

					/*
					 * this is important behaviour: one can change percusive/non-percussive modes on
					 * the fly and the chip will remain in sustain phase - verified on real YM3812
					 */

					if (op.eg_type != 0) { /* non-percussive mode */
						/* do nothing */
					} else { /* percussive mode */
						/* during sustain phase chip adds Release Rate (in percussive mode) */
						if ((OPL.eg_cnt & ((1 << op.eg_sh_rr) - 1)) == 0) {
							op.volume += eg_inc[(int) (op.eg_sel_rr + ((OPL.eg_cnt >> op.eg_sh_rr) & 7))];

							if (op.volume >= MAX_ATT_INDEX) {
								op.volume = MAX_ATT_INDEX;
							}
						}
						/* else do nothing in sustain phase */
					}
					break;
				case EG_REL: /* release phase */
					if ((OPL.eg_cnt & ((1 << op.eg_sh_rr) - 1)) == 0) {
						op.volume += eg_inc[(int) (op.eg_sel_rr + ((OPL.eg_cnt >> op.eg_sh_rr) & 7))];

						if (op.volume >= MAX_ATT_INDEX) {
							op.volume = MAX_ATT_INDEX;
							op.state = EG_OFF;
						}
					}
					break;
				default:
					break;
				}
			}
		}

		for (int i = 0; i < 9 * 2; i++) {
			OPLCh CH = OPL.pCh[i / 2];
			OPLSlot op = CH.slot[i & 1];

			/* Phase Generator */
			if (op.vib != 0) {
				long block_fnum = CH.block_fnum;
				long fnum_lfo = (block_fnum & 0x0380) >> 7;
				byte lfo_fn_table_index_offset = lfo_pm_table[(int) (LFO_PM + 16 * fnum_lfo)];

				if (lfo_fn_table_index_offset != 0) { /* LFO phase modulation active */
					block_fnum += lfo_fn_table_index_offset;
					long block = (block_fnum & 0x1c00) >> 10;
					op.Cnt += (OPL.fn_tab[(int) (block_fnum & 0x03ff)] >> (7 - block)) * op.mul;
				} else { /* LFO phase modulation = zero */
					op.Cnt += op.Incr;
				}
			} else { /* LFO phase modulation disabled for this operator */
				op.Cnt += op.Incr;
			}
		}

		/*
		 * The Noise Generator of the YM3812 is 23-bit shift register. Period is equal
		 * to 2^23-2 samples. Register works at sampling frequency of the chip, so
		 * output can change on every sample.
		 *
		 * Output of the register and input to the bit 22 is: bit0 XOR bit14 XOR bit15
		 * XOR bit22
		 *
		 * Simply use bit 22 as the noise output.
		 */

		OPL.noise_p += OPL.noise_f;
		long i = OPL.noise_p >> FREQ_SH; /* number of events (shifts of the shift register) */
		OPL.noise_p &= FREQ_MASK;
		while (i != 0) {
			/*
			 * Instead of doing all the logic operations above, we use a trick here (and use
			 * bit 0 as the noise output). The difference is only that the noise bit changes
			 * one step ahead. This doesn't matter since we don't know what is real state of
			 * the noise_rng after the reset.
			 */

			if ((OPL.noise_rng & 1) != 0) {
				OPL.noise_rng ^= 0x800302;
			}
			OPL.noise_rng >>= 1;

			i--;
		}
	}

	private int op_calc(long phase, long env, int pm, int wave_tab) {
		long p = (env << 4)
				+ sin_tab[(int) (wave_tab + (((((phase & ~FREQ_MASK) + (pm << 16))) >> FREQ_SH) & SIN_MASK))];

		if (p >= TL_TAB_LEN) {
			return 0;
		}
		return tl_tab[(int) p];
	}

	private int op_calc1(long phase, long env, int pm, int wave_tab) {
		long p = (env << 4) + sin_tab[(int) (wave_tab + (((((phase & ~FREQ_MASK) + pm)) >> FREQ_SH) & SIN_MASK))];

		if (p >= TL_TAB_LEN) {
			return 0;
		}
		return tl_tab[(int) p];
	}

	private long volume_calc(OPLSlot OP) {
		return OP.TLL + OP.volume + (LFO_AM & OP.AMmask);
	}

	/* calculate output */
	private void OPL_CALC_CH(OPLCh CH) {
		phase_modulation = 0;

		/* SLOT 1 */
		OPLSlot SLOT = CH.slot[SLOT1];
		long env = volume_calc(SLOT);
		int out = SLOT.op1_out[0] + SLOT.op1_out[1];
		SLOT.op1_out[0] = SLOT.op1_out[1];
		SLOT.connect1 += SLOT.op1_out[0];
		SLOT.op1_out[1] = 0;
		if (env < ENV_QUIET) {
			if (SLOT.FB == 0) {
				out = 0;
			}
			SLOT.op1_out[1] = op_calc1(SLOT.Cnt, env, (out << SLOT.FB), SLOT.wavetable);
		}

		/* SLOT 2 */
		SLOT = CH.slot[SLOT2];
		env = volume_calc(SLOT);
		if (env < ENV_QUIET) {
			output[0] += op_calc(SLOT.Cnt, env, phase_modulation, SLOT.wavetable);
		}
	}

	/**
	 * <pre>
	operators used in the rhythm sounds generation process:
	
	Envelope Generator:
	
	channel  operator  register number   Bass  High  Snare Tom  Top
	/ slot   number    TL ARDR SLRR Wave Drum  Hat   Drum  Tom  Cymbal
	6 / 0   12        50  70   90   f0  +
	6 / 1   15        53  73   93   f3  +
	7 / 0   13        51  71   91   f1        +
	7 / 1   16        54  74   94   f4              +
	8 / 0   14        52  72   92   f2                    +
	8 / 1   17        55  75   95   f5                          +
	
	Phase Generator:
	
	channel  operator  register number   Bass  High  Snare Tom  Top
	/ slot   number    MULTIPLE          Drum  Hat   Drum  Tom  Cymbal
	6 / 0   12        30                +
	6 / 1   15        33                +
	7 / 0   13        31                      +     +           +
	7 / 1   16        34                -----  n o t  u s e d -----
	8 / 0   14        32                                  +
	8 / 1   17        35                      +                 +
	
	channel  operator  register number   Bass  High  Snare Tom  Top
	number   number    BLK/FNUM2 FNUM    Drum  Hat   Drum  Tom  Cymbal
	6     12,15     B6        A6      +
	
	7     13,16     B7        A7            +     +           +
	
	8     14,17     B8        A8            +           +     +
	 * </pre>
	 */

	public static final class OPLSlot {
		/* attack rate: AR<<2 **/
		private long ar;
		/* decay rate: DR<<2 **/
		private long dr;
		/* release rate:RR<<2 **/
		private long rr;
		/* key scale rate **/
		private int KSR;
		/* keyscale level **/
		private int ksl;
		/* key scale rate: kcode>>KSR **/
		private int ksr;
		/* multiple: mul_tab[ML] **/
		private int mul;

		//
		// Phase Generator
		//

		/* frequency counter **/
		private long Cnt;
		/* frequency counter step **/
		private long Incr;
		/* feedback shift value **/
		private int FB;
		/* slot1 output pointer **/
		@SuppressWarnings("unused")
		private int connect1;
		/* slot1 output for feedback **/
		private int op1_out[] = new int[2];
		/* connection (algorithm) type **/
		private int CON;

		// Envelope Generator

		/* percussive/non-percussive mode **/
		private int eg_type;
		/* phase type **/
		private int state;
		/* total level: TL << 2 **/
		private long TL;
		/* adjusted now TL **/
		private int TLL;
		/* envelope counter **/
		private int volume;
		/* sustain level: sl_tab[SL] **/
		private long sl;
		/* (attack state) **/
		private int eg_sh_ar;
		/* (attack state) **/
		private int eg_sel_ar;
		/* (decay state) **/
		private int eg_sh_dr;
		/* (decay state) **/
		private int eg_sel_dr;
		/* (release state) **/
		private int eg_sh_rr;
		/* (release state) **/
		private int eg_sel_rr;
		/* 0 = KEY OFF, >0 = KEY ON **/
		private byte key;

		//
		// LFO
		//

		/* LFO Amplitude Modulation enable mask **/
		private long AMmask;
		/* LFO Phase Modulation enable flag (active high) **/
		private int vib;

		/* waveform select **/
		private int wavetable;
	}

	private static class OPLCh {
		private OPLSlot slot[] = new OPLSlot[2];

		//
		// phase generator state
		//

		/* block+fnum **/
		private long block_fnum;
		/* Freq. Increment base **/
		private long fc;
		/* KeyScaleLevel Base step **/
		private long ksl_base;
		/* key code (for key scaling) **/
		private int kcode;

		public OPLCh() {
			for (int i = 0; i < slot.length; i++) {
				slot[i] = new OPLSlot();
			}
		}
	}

	/* OPL state */
	public static class FmOPL {
		//
		// FM channel slots
		//

		/* OPL/OPL2 chips have 9 channels **/
		private OPLCh pCh[] = new OPLCh[9];

		/* global envelope generator counter **/
		private long eg_cnt;
		/* global envelope generator counter works at frequency = chipclock/72 **/
		private long eg_timer;
		/* step of eg_timer **/
		private long eg_timer_add;
		/* envelope generator timer overlfows every 1 sample (on real chip) **/
		private long eg_timer_overflow;

		/* Rhythm mode **/
		private int rhythm;

		/* fnumber.increment counter **/
		private long fn_tab[] = new long[1024];

		/* LFO */
		private long lfo_am_depth;
		private long lfo_pm_depth_range;
		private long lfo_am_cnt;
		private long lfo_am_inc;
		private long lfo_pm_cnt;
		private long lfo_pm_inc;

		/* 23 bit noise shift register **/
		private long noise_rng;
		/* current noise 'phase' **/
		private long noise_p;
		/* current noise period **/
		private long noise_f;

		/* waveform select enable flag **/
		private int wavesel;

		/* timer counters **/
		private long T[] = new long[2];
		/* timer enable **/
		private int st[] = new int[2];
		/* timer alarms **/
		private Event fmopl_alarm[] = new Event[2];
		/* timer alarms pending **/
		private int fmopl_alarm_pending[] = new int[2];

		/* chip type **/
		private int type;
		/* address register **/
		private int address;
		/* status flag **/
		private byte status;
		/* status mask **/
		private byte statusmask;
		/* Reg.08 : CSM,notesel,etc. **/
		private byte mode;

		/* master clock (Hz) **/
		private long clock;
		/* sampling rate (Hz) **/
		private long rate;
		/* frequency base **/
		private double freqbase;

		public FmOPL() {
			for (int i = 0; i < pCh.length; i++) {
				pCh[i] = new OPLCh();
			}
		}
	};

	/* calculate rhythm */

	private void OPL_CALC_RH(OPLCh[] CH, long noise) {
		/*
		 * Bass Drum (verified on real YM3812): - depends on the channel 6 'connect'
		 * register: when connect = 0 it works the same as in normal (non-rhythm) mode
		 * (op1.op2.out) when connect = 1 _only_ operator 2 is present on output
		 * (op2.out), operator 1 is ignored - output sample always is multiplied by 2
		 */

		phase_modulation = 0;

		/* SLOT 1 */
		OPLSlot SLOT = CH[6].slot[SLOT1];
		long env = volume_calc(SLOT);

		int out = SLOT.op1_out[0] + SLOT.op1_out[1];
		SLOT.op1_out[0] = SLOT.op1_out[1];

		if (SLOT.CON == 0) {
			phase_modulation = SLOT.op1_out[0];
			/* else ignore output of operator 1 */
		}

		SLOT.op1_out[1] = 0;
		if (env < ENV_QUIET) {
			if (SLOT.FB == 0) {
				out = 0;
			}
			SLOT.op1_out[1] = op_calc1(SLOT.Cnt, env, (out << SLOT.FB), SLOT.wavetable);
		}

		/* SLOT 2 */
		SLOT = CH[6].slot[SLOT2];
		env = volume_calc(SLOT);
		if (env < ENV_QUIET) {
			output[0] += op_calc(SLOT.Cnt, env, phase_modulation, SLOT.wavetable) * 2;
		}

		/* Phase generation is based on: */
		/*
		 * HH (13) channel 7.slot 1 combined with channel 8.slot 2 (same combination as
		 * TOP CYMBAL but different output phases)
		 */
		/* SD (16) channel 7.slot 1 */
		/* TOM (14) channel 8.slot 1 */
		/*
		 * TOP (17) channel 7.slot 1 combined with channel 8.slot 2 (same combination as
		 * HIGH HAT but different output phases)
		 */

		/* Envelope generation based on: */
		/* HH channel 7.slot1 */
		/* SD channel 7.slot2 */
		/* TOM channel 8.slot1 */
		/* TOP channel 8.slot2 */

		/*
		 * The following formulas can be well optimized. I leave them in direct form for
		 * now (in case I've missed something).
		 */

		/* High Hat (verified on real YM3812) */
		env = volume_calc(SLOT7_1);
		if (env < ENV_QUIET) {
			/*
			 * high hat phase generation: phase = d0 or 234 (based on frequency only) phase
			 * = 34 or 2d0 (based on noise)
			 */

			/* base frequency derived from operator 1 in channel 7 */
			byte bit7 = (byte) (((SLOT7_1.Cnt >> FREQ_SH) >> 7) & 1);
			byte bit3 = (byte) (((SLOT7_1.Cnt >> FREQ_SH) >> 3) & 1);
			byte bit2 = (byte) (((SLOT7_1.Cnt >> FREQ_SH) >> 2) & 1);

			byte res1 = (byte) ((bit2 ^ bit7) | bit3);

			/* when res1 = 0 phase = 0x000 | 0xd0; */
			/* when res1 = 1 phase = 0x200 | (0xd0>>2); */
			long phase = res1 != 0 ? (0x200 | (0xd0 >> 2)) : 0xd0;

			/* enable gate based on frequency of operator 2 in channel 8 */
			byte bit5e = (byte) (((SLOT8_2.Cnt >> FREQ_SH) >> 5) & 1);
			byte bit3e = (byte) (((SLOT8_2.Cnt >> FREQ_SH) >> 3) & 1);

			byte res2 = (byte) (bit3e ^ bit5e);

			/* when res2 = 0 pass the phase from calculation above (res1); */
			/* when res2 = 1 phase = 0x200 | (0xd0>>2); */
			if (res2 != 0) {
				phase = (0x200 | (0xd0 >> 2));
			}

			/* when phase & 0x200 is set and noise=1 then phase = 0x200|0xd0 */
			/*
			 * when phase & 0x200 is set and noise=0 then phase = 0x200|(0xd0>>2), ie no
			 * change
			 */
			if ((phase & 0x200) != 0) {
				if (noise != 0) {
					phase = 0x200 | 0xd0;
				}
			} else {
				/* when phase & 0x200 is clear and noise=1 then phase = 0xd0>>2 */
				/* when phase & 0x200 is clear and noise=0 then phase = 0xd0, ie no change */
				if (noise != 0) {
					phase = 0xd0 >> 2;
				}
			}

			output[0] += op_calc(phase << FREQ_SH, env, 0, SLOT7_1.wavetable) * 2;
		}

		/* Snare Drum (verified on real YM3812) */
		env = volume_calc(SLOT7_2);
		if (env < ENV_QUIET) {
			/* base frequency derived from operator 1 in channel 7 */
			byte bit8 = (byte) (((SLOT7_1.Cnt >> FREQ_SH) >> 8) & 1);

			/* when bit8 = 0 phase = 0x100; */
			/* when bit8 = 1 phase = 0x200; */
			int phase = bit8 != 0 ? 0x200 : 0x100;

			/* Noise bit XOR'es phase by 0x100 */
			/* when noisebit = 0 pass the phase from calculation above */
			/* when noisebit = 1 phase ^= 0x100; */
			/* in other words: phase ^= (noisebit<<8); */
			if (noise != 0) {
				phase ^= 0x100;
			}

			output[0] += op_calc(phase << FREQ_SH, env, 0, SLOT7_2.wavetable) * 2;
		}

		/* Tom Tom (verified on real YM3812) */
		env = volume_calc(SLOT8_1);
		if (env < ENV_QUIET) {
			output[0] += op_calc(SLOT8_1.Cnt, env, 0, SLOT8_1.wavetable) * 2;
		}

		/* Top Cymbal (verified on real YM3812) */
		env = volume_calc(SLOT8_2);
		if (env < ENV_QUIET) {
			/* base frequency derived from operator 1 in channel 7 */
			byte bit7 = (byte) (((SLOT7_1.Cnt >> FREQ_SH) >> 7) & 1);
			byte bit3 = (byte) (((SLOT7_1.Cnt >> FREQ_SH) >> 3) & 1);
			byte bit2 = (byte) (((SLOT7_1.Cnt >> FREQ_SH) >> 2) & 1);

			byte res1 = (byte) ((bit2 ^ bit7) | bit3);

			/* when res1 = 0 phase = 0x000 | 0x100; */
			/* when res1 = 1 phase = 0x200 | 0x100; */
			int phase = res1 != 0 ? 0x300 : 0x100;

			/* enable gate based on frequency of operator 2 in channel 8 */
			byte bit5e = (byte) (((SLOT8_2.Cnt >> FREQ_SH) >> 5) & 1);
			byte bit3e = (byte) (((SLOT8_2.Cnt >> FREQ_SH) >> 3) & 1);

			byte res2 = (byte) (bit3e ^ bit5e);

			/* when res2 = 0 pass the phase from calculation above (res1); */
			/* when res2 = 1 phase = 0x200 | 0x100; */
			if (res2 != 0) {
				phase = 0x300;
			}

			output[0] += op_calc(phase << FREQ_SH, env, 0, SLOT8_2.wavetable) * 2;
		}
	}

//VERIFIED
	/* generic table initialize */
	private int init_tables() {
		for (int x = 0; x < TL_RES_LEN; x++) {
			double m = (1 << 16) / Math.pow(2, (x + 1) * (ENV_STEP / 4.0) / 8.0);
			m = Math.floor(m);

			/* we never reach (1<<16) here due to the (x+1) */
			/* result fits within 16 bits at maximum */

			int n = (int) m; /* 16 bits here */
			n >>= 4; /* 12 bits here */
			if ((n & 1) != 0) { /* round to nearest */
				n = (n >> 1) + 1;
			} else {
				n = n >> 1;
			}
			/* 11 bits here (rounded) */
			n <<= 1; /* 12 bits here (as in real chip) */
			tl_tab[x * 2 + 0] = n;
			tl_tab[x * 2 + 1] = -tl_tab[x * 2 + 0];

			for (int i = 1; i < 12; i++) {
				tl_tab[x * 2 + 0 + i * 2 * TL_RES_LEN] = tl_tab[x * 2 + 0] >> i;
				tl_tab[x * 2 + 1 + i * 2 * TL_RES_LEN] = -tl_tab[x * 2 + 0 + i * 2 * TL_RES_LEN];
			}
		}

		for (int i = 0; i < SIN_LEN; i++) {
			/* non-standard sinus */
			double m = Math.sin(((i * 2) + 1) * Math.PI / SIN_LEN); /* checked against the real chip */

			/* we never reach zero here due to ((i * 2) + 1) */

			double o;
			if (m > 0.0) {
				o = 8 * Math.log(1.0 / m) / Math.log(2.0); /* convert to 'decibels' */
			} else {
				o = 8 * Math.log(-1.0 / m) / Math.log(2.0); /* convert to 'decibels' */
			}

			o = o / (ENV_STEP / 4);

			int n = (int) (2.0 * o);
			if ((n & 1) != 0) { /* round to nearest */
				n = (n >> 1) + 1;
			} else {
				n = n >> 1;
			}
			sin_tab[i] = n * 2 + (m >= 0.0 ? 0 : 1);
		}

		for (int i = 0; i < SIN_LEN; i++) {
			/* waveform 1: __ __ */
			/* / \____/ \____ */
			/* output only first half of the sinus waveform (positive one) */

			if ((i & (1 << (SIN_BITS - 1))) != 0) {
				sin_tab[1 * SIN_LEN + i] = TL_TAB_LEN;
			} else {
				sin_tab[1 * SIN_LEN + i] = sin_tab[i];
			}

			/* waveform 2: __ __ __ __ */
			/* / \/ \/ \/ \ */
			/* abs(sin) */

			sin_tab[2 * SIN_LEN + i] = sin_tab[i & (SIN_MASK >> 1)];

			/* waveform 3: _ _ _ _ */
			/* / |_/ |_/ |_/ |_ */
			/* abs(output only first quarter of the sinus waveform) */

			if ((i & (1 << (SIN_BITS - 2))) != 0) {
				sin_tab[3 * SIN_LEN + i] = TL_TAB_LEN;
			} else {
				sin_tab[3 * SIN_LEN + i] = sin_tab[i & (SIN_MASK >> 2)];
			}
		}

		return 1;
	}

// VERIFIED
	private void OPL_initalize(FmOPL OPL) {
		/* frequency base */
		OPL.freqbase = (OPL.rate != 0) ? (OPL.clock / 72.0) / OPL.rate : 0;

		/* make fnumber . increment counter table */
		for (int i = 0; i < 1024; i++) {
			/* opn phase increment counter = 20bit */
			OPL.fn_tab[i] = (long) ((double) i * 64 * OPL.freqbase
					* (1 << (FREQ_SH - 10))); /* -10 because chip works with 10.10 fixed point, while we use 16.16 */
		}

		/*
		 * Amplitude modulation: 27 output levels (triangle waveform); 1 level takes one
		 * of: 192, 256 or 448 samples
		 */
		/* One entry from LFO_AM_TABLE lasts for 64 samples */
		OPL.lfo_am_inc = (long) ((1.0 / 64.0) * (1 << LFO_SH) * OPL.freqbase);

		/* Vibrato: 8 output levels (triangle waveform); 1 level takes 1024 samples */
		OPL.lfo_pm_inc = (long) ((1.0 / 1024.0) * (1 << LFO_SH) * OPL.freqbase);

		/* Noise generator: a step takes 1 sample */
		OPL.noise_f = (long) ((1.0 / 1.0) * (1 << FREQ_SH) * OPL.freqbase);

		OPL.eg_timer_add = (long) ((1 << EG_SH) * OPL.freqbase);
		OPL.eg_timer_overflow = (1) * (1 << EG_SH);
	}

	private void FM_KEYON(OPLSlot SLOT, byte key_set) {
		if (SLOT.key != 0) {
			/* restart Phase Generator */
			SLOT.Cnt = 0;

			/* phase . Attack */
			SLOT.state = EG_ATT;
		}
		SLOT.key |= key_set;
	}

	private void FM_KEYOFF(OPLSlot SLOT, byte key_clr) {
		if (SLOT.key != 0) {
			SLOT.key &= key_clr;

			if (SLOT.key == 0) {
				/* phase . Release */
				if (SLOT.state > EG_REL) {
					SLOT.state = EG_REL;
				}
			}
		}
	}

	/*
	 * update phase increment counter of operator (also update the EG rates if
	 * necessary)
	 */
	private void CALC_FCSLOT(OPLCh CH, OPLSlot SLOT) {
		/* (frequency) phase increment counter */
		SLOT.Incr = CH.fc * SLOT.mul;
		int ksr = CH.kcode >> SLOT.KSR;

		if (SLOT.ksr != ksr) {
			SLOT.ksr = ksr;

			/* calculate envelope generator rates */
			if ((SLOT.ar + SLOT.ksr) < 16 + 62) {
				SLOT.eg_sh_ar = eg_rate_shift[(int) (SLOT.ar + SLOT.ksr)];
				SLOT.eg_sel_ar = eg_rate_select[(int) (SLOT.ar + SLOT.ksr)];
			} else {
				SLOT.eg_sh_ar = 0;
				SLOT.eg_sel_ar = 13 * RATE_STEPS;
			}
			SLOT.eg_sh_dr = eg_rate_shift[(int) (SLOT.dr + SLOT.ksr)];
			SLOT.eg_sel_dr = eg_rate_select[(int) (SLOT.dr + SLOT.ksr)];
			SLOT.eg_sh_rr = eg_rate_shift[(int) (SLOT.rr + SLOT.ksr)];
			SLOT.eg_sel_rr = eg_rate_select[(int) (SLOT.rr + SLOT.ksr)];
		}
	}

	/* set multi,am,vib,EG-TYP,KSR,mul */
	private void set_mul(FmOPL OPL, int slot, int v) {
		OPLCh CH = OPL.pCh[slot / 2];
		OPLSlot SLOT = CH.slot[slot & 1];

		SLOT.mul = (int) (mul_tab[v & 0x0f]) & 0xff;
		SLOT.KSR = (v & 0x10) != 0 ? 0 : 2;
		SLOT.eg_type = (v & 0x20);
		SLOT.vib = (v & 0x40);
		SLOT.AMmask = (v & 0x80) != 0 ? ~0 : 0;
		CALC_FCSLOT(CH, SLOT);
	}

	/* set ksl & tl */
	private void set_ksl_tl(FmOPL OPL, int slot, int v) {
		OPLCh CH = OPL.pCh[slot / 2];
		OPLSlot SLOT = CH.slot[slot & 1];
		int ksl = v >> 6; /* 0 / 1.5 / 3.0 / 6.0 dB/OCT */

		SLOT.ksl = ksl != 0 ? 3 - ksl : 31;
		SLOT.TL = (v & 0x3f) << (ENV_BITS - 1 - 7); /* 7 bits TL (bit 6 = always 0) */

		SLOT.TLL = (int) (SLOT.TL + (CH.ksl_base >> SLOT.ksl));
	}

	/* set attack rate & decay rate */
	private void set_ar_dr(FmOPL OPL, int slot, int v) {
		OPLCh CH = OPL.pCh[slot / 2];
		OPLSlot SLOT = CH.slot[slot & 1];

		SLOT.ar = (v >> 4) != 0 ? 16 + ((v >> 4) << 2) : 0;

		if ((SLOT.ar + SLOT.ksr) < 16 + 62) {
			SLOT.eg_sh_ar = eg_rate_shift[(int) (SLOT.ar + SLOT.ksr)];
			SLOT.eg_sel_ar = eg_rate_select[(int) (SLOT.ar + SLOT.ksr)];
		} else {
			SLOT.eg_sh_ar = 0;
			SLOT.eg_sel_ar = 13 * RATE_STEPS;
		}

		SLOT.dr = (v & 0x0f) != 0 ? 16 + ((v & 0x0f) << 2) : 0;
		SLOT.eg_sh_dr = eg_rate_shift[(int) (SLOT.dr + SLOT.ksr)];
		SLOT.eg_sel_dr = eg_rate_select[(int) (SLOT.dr + SLOT.ksr)];
	}

	/* set sustain level & release rate */
	private void set_sl_rr(FmOPL OPL, int slot, int v) {
		OPLCh CH = OPL.pCh[slot / 2];
		OPLSlot SLOT = CH.slot[slot & 1];

		SLOT.sl = (int) sl_tab[v >> 4];

		SLOT.rr = (v & 0x0f) != 0 ? 16 + ((v & 0x0f) << 2) : 0;
		SLOT.eg_sh_rr = eg_rate_shift[(int) (SLOT.rr + SLOT.ksr)];
		SLOT.eg_sel_rr = eg_rate_select[(int) (SLOT.rr + SLOT.ksr)];
	}

	/* write a value v to register r on OPL chip */
	private void OPLWriteReg(FmOPL OPL, int r, byte v) {
		/* adjust bus to 8 bits */
		r &= 0xff;

		switch (r & 0xe0) {
		case 0x00: /* 00-1f:control */
			switch (r & 0x1f) {
			case 0x01: /* waveform select enable */
				if ((OPL.type & OPL_TYPE_WAVESEL) != 0) {
					OPL.wavesel = v & 0x20;
					/* do not change the waveform previously selected */
				}
				break;
			case 0x02: /* Timer 1 */
				OPL.T[0] = v;
				if (OPL.fmopl_alarm_pending[0] != 0) {
					alarm_unset(OPL.fmopl_alarm[0]);
					alarm_set(OPL.fmopl_alarm[0], /* maincpu_clk() + */ ((256 - v) * fmopl_timer_80));
				}
				break;
			case 0x03: /* Timer 2 */
				OPL.T[1] = v;
				if (OPL.fmopl_alarm_pending[1] != 0) {
					alarm_unset(OPL.fmopl_alarm[1]);
					alarm_set(OPL.fmopl_alarm[1], /* maincpu_clk() + */ ((256 - v) * fmopl_timer_320));
				}
				break;
			case 0x04: /* IRQ clear / mask and Timer enable */
				if ((v & 0x80) != 0) { /* IRQ flag clear */
					OPL_STATUS_RESET(OPL, (byte) (0x7f
							- 0x08)); /*
										 * don't reset BFRDY flag or we will have to call deltat module to set the flag
										 */
				} else { /* set IRQ mask ,timer enable */
					int st1 = v & 1;
					int st2 = (v >> 1) & 1;

					/* IRQRST,T1MSK,t2MSK,EOSMSK,BRMSK,x,ST2,ST1 */
					OPL_STATUS_RESET(OPL, (byte) (v & (0x78 - 0x08)));
					OPL_STATUSMASK_SET(OPL, (byte) ((~v) & 0x78));

					/* timer 2 */
					if (OPL.st[1] != st2) {
						OPL.st[1] = st2;
					}

					/* timer 1 */
					if (OPL.st[0] != st1) {
						OPL.st[0] = st1;
					}

					/* Timer 1 changes */
					if ((v & 0x40) == 0) {
						if ((v & 1) == 0) {
							if (OPL.fmopl_alarm_pending[0] != 0) {
								alarm_unset(OPL.fmopl_alarm[0]);
								OPL.fmopl_alarm_pending[0] = 0;
							}
						} else {
							if (OPL.fmopl_alarm_pending[0] != 0) {
								alarm_unset(OPL.fmopl_alarm[0]);
							}
							alarm_set(OPL.fmopl_alarm[0], /* maincpu_clk() + */ ((256 - OPL.T[0]) * fmopl_timer_80));
							OPL.fmopl_alarm_pending[0] = 1;
						}
					}

					/* Timer 2 changes */
					if ((v & 0x20) == 0) {
						if ((v & 2) == 0) {
							if (OPL.fmopl_alarm_pending[1] != 0) {
								alarm_unset(OPL.fmopl_alarm[1]);
								OPL.fmopl_alarm_pending[1] = 0;
							}
						} else {
							if (OPL.fmopl_alarm_pending[1] != 0) {
								alarm_unset(OPL.fmopl_alarm[1]);
							}
							alarm_set(OPL.fmopl_alarm[1], /* maincpu_clk() + */ ((256 - OPL.T[1]) * fmopl_timer_320));
							OPL.fmopl_alarm_pending[1] = 1;
						}
					}
				}
				break;
			case 0x08: /* MODE,DELTA-T control 2 : CSM,NOTESEL,x,x,smpl,da/ad,64k,rom */
				OPL.mode = v;
				break;
			default:
				break;
			}
			break;
		case 0x20: {/* am ON, vib ON, ksr, eg_type, mul */
			int slot = slot_array[r & 0x1f];
			if (slot < 0) {
				return;
			}
			set_mul(OPL, slot, v & 0xff);
			break;
		}
		case 0x40: {
			int slot = slot_array[r & 0x1f];
			if (slot < 0) {
				return;
			}
			set_ksl_tl(OPL, slot, v & 0xff);
			break;
		}
		case 0x60: {
			int slot = slot_array[r & 0x1f];
			if (slot < 0) {
				return;
			}
			set_ar_dr(OPL, slot, v & 0xff);
			break;
		}
		case 0x80: {
			int slot = slot_array[r & 0x1f];
			if (slot < 0) {
				return;
			}
			set_sl_rr(OPL, slot, v & 0xff);
			break;
		}
		case 0xa0:
			if (r == 0xbd) { /* am depth, vibrato depth, r,bd,sd,tom,tc,hh */
				OPL.lfo_am_depth = v & 0x80;
				OPL.lfo_pm_depth_range = (v & 0x40) != 0 ? 8 : 0;

				OPL.rhythm = v & 0x3f;

				if ((OPL.rhythm & 0x20) != 0) {
					/* BD key on/off */
					if ((v & 0x10) != 0) {
						FM_KEYON(OPL.pCh[6].slot[SLOT1], (byte) 2);
						FM_KEYON(OPL.pCh[6].slot[SLOT2], (byte) 2);
					} else {
						FM_KEYOFF(OPL.pCh[6].slot[SLOT1], (byte) ~2);
						FM_KEYOFF(OPL.pCh[6].slot[SLOT2], (byte) ~2);
					}
					/* HH key on/off */
					if ((v & 0x01) != 0) {
						FM_KEYON(OPL.pCh[7].slot[SLOT1], (byte) 2);
					} else {
						FM_KEYOFF(OPL.pCh[7].slot[SLOT1], (byte) ~2);
					}

					/* SD key on/off */
					if ((v & 0x08) != 0) {
						FM_KEYON(OPL.pCh[7].slot[SLOT2], (byte) 2);
					} else {
						FM_KEYOFF(OPL.pCh[7].slot[SLOT2], (byte) ~2);
					}

					/* TOM key on/off */
					if ((v & 0x04) != 0) {
						FM_KEYON(OPL.pCh[8].slot[SLOT1], (byte) 2);
					} else {
						FM_KEYOFF(OPL.pCh[8].slot[SLOT1], (byte) ~2);
					}

					/* TOP-CY key on/off */
					if ((v & 0x02) != 0) {
						FM_KEYON(OPL.pCh[8].slot[SLOT2], (byte) 2);
					} else {
						FM_KEYOFF(OPL.pCh[8].slot[SLOT2], (byte) ~2);
					}
				} else {
					/* BD key off */
					FM_KEYOFF(OPL.pCh[6].slot[SLOT1], (byte) ~2);
					FM_KEYOFF(OPL.pCh[6].slot[SLOT2], (byte) ~2);

					/* HH key off */
					FM_KEYOFF(OPL.pCh[7].slot[SLOT1], (byte) ~2);

					/* SD key off */
					FM_KEYOFF(OPL.pCh[7].slot[SLOT2], (byte) ~2);

					/* TOM key off */
					FM_KEYOFF(OPL.pCh[8].slot[SLOT1], (byte) ~2);

					/* TOP-CY off */
					FM_KEYOFF(OPL.pCh[8].slot[SLOT2], (byte) ~2);
				}
				return;
			}
			/* keyon,block,fnum */
			if ((r & 0x0f) > 8) {
				return;
			}
			OPLCh CH = OPL.pCh[r & 0x0f];
			long block_fnum;
			if ((r & 0x10) == 0) { /* a0-a8 */
				block_fnum = (CH.block_fnum & 0x1f00) | (v & 0xff);
			} else { /* b0-b8 */
				block_fnum = ((v & 0x1f) << 8) | (CH.block_fnum & 0xff);

				if ((v & 0x20) != 0) {
					FM_KEYON(CH.slot[SLOT1], (byte) 1);
					FM_KEYON(CH.slot[SLOT2], (byte) 1);
				} else {
					FM_KEYOFF(CH.slot[SLOT1], (byte) ~1);
					FM_KEYOFF(CH.slot[SLOT2], (byte) ~1);
				}
			}
			/* update */
			if (CH.block_fnum != block_fnum) {
				long block = block_fnum >> 10;

				CH.block_fnum = block_fnum;

				CH.ksl_base = (long) (ksl_tab[(int) (block_fnum >> 6)]) & 0xffffffff;
				CH.fc = OPL.fn_tab[(int) (block_fnum & 0x03ff)] >> (7 - block);

				/* BLK 2,1,0 bits . bits 3,2,1 of kcode */
				CH.kcode = (int) ((CH.block_fnum & 0x1c00) >> 9);

				/*
				 * the info below is actually opposite to what is stated in the Manuals (verifed
				 * on real YM3812)
				 */
				/* if notesel == 0 . lsb of kcode is bit 10 (MSB) of fnum */
				/* if notesel == 1 . lsb of kcode is bit 9 (MSB-1) of fnum */
				if ((OPL.mode & 0x40) != 0) {
					CH.kcode |= (CH.block_fnum & 0x100) >> 8; /* notesel == 1 */
				} else {
					CH.kcode |= (CH.block_fnum & 0x200) >> 9; /* notesel == 0 */
				}

				/* refresh Total Level in both SLOTs of this channel */
				CH.slot[SLOT1].TLL = (int) (CH.slot[SLOT1].TL + (CH.ksl_base >> CH.slot[SLOT1].ksl));
				CH.slot[SLOT2].TLL = (int) (CH.slot[SLOT2].TL + (CH.ksl_base >> CH.slot[SLOT2].ksl));

				/* refresh frequency counter in both SLOTs of this channel */
				CALC_FCSLOT(CH, CH.slot[SLOT1]);
				CALC_FCSLOT(CH, CH.slot[SLOT2]);
			}
			break;
		case 0xc0:
			/* FB,C */
			if ((r & 0x0f) > 8) {
				return;
			}
			CH = OPL.pCh[r & 0x0f];
			CH.slot[SLOT1].FB = ((v >> 1) & 7) != 0 ? ((v >> 1) & 7) + 7 : 0;
			CH.slot[SLOT1].CON = v & 1;
			CH.slot[SLOT1].connect1 = CH.slot[SLOT1].CON != 0 ? output[0] : phase_modulation;
			break;
		case 0xe0: {/* waveform select */
			/*
			 * simply ignore write to the waveform select register if selecting not enabled
			 * in test register
			 */
			if ((OPL.wavesel) != 0) {
				int slot = slot_array[r & 0x1f];
				if (slot < 0) {
					return;
				}
				CH = OPL.pCh[slot / 2];

				CH.slot[slot & 1].wavetable = (v & 0x03) * SIN_LEN;
			}
			break;
		}
		}
	}

	/* lock/unlock for common table */
	private int OPL_LockTable() {
		num_lock++;
		if (num_lock > 1) {
			return 0;
		}

		/* first time */

		cur_chip = null;
		/* allocate total level table (128kb space) */
		if (init_tables() == 0) {
			num_lock--;
			return -1;
		}

		return 0;
	}

	private void OPL_UnLockTable() {
		if (num_lock != 0) {
			num_lock--;
		}
		if (num_lock != 0) {
			return;
		}

		/* last time */

		cur_chip = null;
	}

	private void OPLResetChip(FmOPL OPL) {
		OPL.eg_timer = 0;
		OPL.eg_cnt = 0;

		OPL.noise_rng = 1; /* noise shift register */
		OPL.mode = 0; /* normal mode */
		OPL_STATUS_RESET(OPL, (byte) 0x7f);

		/* reset with register write */
		OPLWriteReg(OPL, 0x01, (byte) 0); /* wavesel disable */
		OPLWriteReg(OPL, 0x02, (byte) 0); /* Timer1 */
		OPLWriteReg(OPL, 0x03, (byte) 0); /* Timer2 */
		OPLWriteReg(OPL, 0x04, (byte) 0); /* IRQ mask clear */
		for (int i = 0xff; i >= 0x20; i--) {
			OPLWriteReg(OPL, i, (byte) 0);
		}

		/* reset operator parameters */
		for (int c = 0; c < 9; c++) {
			OPLCh CH = OPL.pCh[c];
			for (int s = 0; s < 2; s++) {
				/* wave table */
				CH.slot[s].wavetable = 0;
				CH.slot[s].state = EG_OFF;
				CH.slot[s].volume = MAX_ATT_INDEX;
				CH.slot[s].connect1 = output[0];
			}
		}

		if (OPL.fmopl_alarm_pending[0] != 0) {
			alarm_unset(OPL.fmopl_alarm[0]);
		}

		if (OPL.fmopl_alarm_pending[1] != 0) {
			alarm_unset(OPL.fmopl_alarm[1]);
		}
	}

	/* Create one of virtual YM3812/YM3526 */
	/* 'clock' is chip clock in Hz */
	/* 'rate' is sampling rate */
	private FmOPL OPLCreate(long clock, long rate, int type) {
		if (OPL_LockTable() == -1) {
			return null;
		}

		/* allocate memory block */
		final FmOPL OPL = new FmOPL();

		OPL.type = type;
		OPL.clock = clock;
		OPL.rate = rate;

		OPL.fmopl_alarm[0] = new Event("FMOPL Timer A") {
			@Override
			public void event() throws InterruptedException {
				fmopl_alarm_A(0, OPL);
			};
		};
		OPL.fmopl_alarm[1] = new Event("FMOPL Timer B") {
			@Override
			public void event() throws InterruptedException {
				fmopl_alarm_B(0, OPL);
			};
		};
		OPL.fmopl_alarm_pending[0] = 0;
		OPL.fmopl_alarm_pending[1] = 0;

		/* init global tables */
		OPL_initalize(OPL);

		return OPL;
	}

	/* Destroy one of virtual YM3812 */
	private void OPLDestroy(FmOPL OPL) {
		if (OPL.fmopl_alarm_pending[0] != 0) {
			alarm_unset(OPL.fmopl_alarm[0]);
		}

		if (OPL.fmopl_alarm_pending[1] != 0) {
			alarm_unset(OPL.fmopl_alarm[1]);
		}

		OPL_UnLockTable();
	}

	private int OPLWrite(FmOPL OPL, int a, byte v) {
		if ((a & 1) == 0) { /* address port */
			OPL.address = v & 0xff;
		} else { /* data port */
			OPLWriteReg(OPL, OPL.address, v);
		}
		return OPL.status >> 7;
	}

	private byte OPLRead(FmOPL OPL, int a) {
		if ((a & 1) == 0) {
			/* OPL and OPL2 */
			return (byte) (OPL.status & (OPL.statusmask | 0x80));
		}

		return (byte) 0xff;
	}

	/* CSM Key Controll */
	private void CSMKeyControll(OPLCh CH) {
		FM_KEYON(CH.slot[SLOT1], (byte) 4);
		FM_KEYON(CH.slot[SLOT2], (byte) 4);

		/*
		 * The key off should happen exactly one sample later - not implemented
		 * correctly yet
		 */
		FM_KEYOFF(CH.slot[SLOT1], (byte) ~4);
		FM_KEYOFF(CH.slot[SLOT2], (byte) ~4);
	}

	private int OPLTimerOver(FmOPL OPL, int c) {
		if (c != 0) { /* Timer B */
			OPL_STATUS_SET(OPL, (byte) 0x20);
		} else { /* Timer A */
			OPL_STATUS_SET(OPL, (byte) 0x40);
			/* CSM mode key,TL controll */
			if ((OPL.mode & 0x80) != 0) { /* CSM mode total level latch and auto key on */
				int ch;

				for (ch = 0; ch < 9; ch++) {
					CSMKeyControll(OPL.pCh[ch]);
				}
			}
		}
		/* reload timer */
		return OPL.status >> 7;
	}

	public FmOPL ym3812_init(long clock, long rate) {
		/* emulator create */
		FmOPL YM3812 = OPLCreate(clock, rate, OPL_TYPE_YM3812);
		if (YM3812 != null) {
			ym3812_reset_chip(YM3812);
		}
		return YM3812;
	}

	public void ym3812_shutdown(FmOPL chip) {
		OPLDestroy(chip);
	}

	public void ym3812_reset_chip(FmOPL chip) {
		OPLResetChip(chip);
	}

	public int ym3812_write(FmOPL chip, int a, byte v) {
		return OPLWrite(chip, a, v);
	}

	public byte ym3812_read(FmOPL chip, int a) {
		/* YM3812 always returns bit2 and bit1 in HIGH state */
		return (byte) (OPLRead(chip, a) | 0x06);
	}

	public byte ym3812_peek(FmOPL chip, int a) {
		/* YM3812 always returns bit2 and bit1 in HIGH state */
		return (byte) (OPLRead(chip, a) | 0x06);
	}

	/**
	 * @param OPL    virtual YM3812
	 * @param buffer output buffer pointer
	 * @param length number of samples that should be generated
	 */
	public void ym3812_update_one(FmOPL OPL, IntConsumer buffer, int length) {
		int rhythm = OPL.rhythm & 0x20;

		if (OPL != cur_chip) {
			cur_chip = OPL;
			/* rhythm slots */
			SLOT7_1 = OPL.pCh[7].slot[SLOT1];
			SLOT7_2 = OPL.pCh[7].slot[SLOT2];
			SLOT8_1 = OPL.pCh[8].slot[SLOT1];
			SLOT8_2 = OPL.pCh[8].slot[SLOT2];
		}
		for (int i = 0; i < length; i++) {
			output[0] = 0;

			advance_lfo(OPL);

			/* FM part */
			OPL_CALC_CH(OPL.pCh[0]);
			OPL_CALC_CH(OPL.pCh[1]);
			OPL_CALC_CH(OPL.pCh[2]);
			OPL_CALC_CH(OPL.pCh[3]);
			OPL_CALC_CH(OPL.pCh[4]);
			OPL_CALC_CH(OPL.pCh[5]);

			if (rhythm == 0) {
				OPL_CALC_CH(OPL.pCh[6]);
				OPL_CALC_CH(OPL.pCh[7]);
				OPL_CALC_CH(OPL.pCh[8]);
			} else { /* Rhythm part */
				OPL_CALC_RH(OPL.pCh, (OPL.noise_rng >> 0) & 1);
			}

			int lt = output[0];

			lt >>= FINAL_SH;

			/* limit check */
			lt = Math.max(Math.min(lt, Short.MAX_VALUE), Short.MIN_VALUE);

			/* store to sound buffer */
			buffer.accept(lt);

			advance(OPL);
		}
	}

	public FmOPL ym3526_init(long clock, long rate, EventScheduler context) {
		/* emulator create */
		FmOPL YM3526 = OPLCreate(clock, rate, OPL_TYPE_YM3526);
		if (YM3526 != null) {
			ym3526_reset_chip(YM3526, context);
		}
		return YM3526;
	}

	public void ym3526_shutdown(FmOPL chip) {
		OPLDestroy(chip);
	}

//	static int cnt;
//	static long start;
//	static PrintStream fout;

	public void ym3526_reset_chip(FmOPL chip, EventScheduler context) {
		OPLResetChip(chip);
//		cnt = -1;
//		try {
//			fout = new PrintStream(new BufferedOutputStream(new FileOutputStream("/home/ken/java.txt")));
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	public int ym3526_write(FmOPL chip, int a, byte v, EventScheduler context) {
//		if (cnt == -1) {
//			start = context.getTime(Phase.PHI1);
//		}
//		cnt = 0;
//		fout.printf("\nw%08X: %02X,%02X\n", context.getTime(Phase.PHI1) - start, a, v);
		return OPLWrite(chip, a, v);
	}

	public byte ym3526_read(FmOPL chip, int a) {
		/* YM3526 always returns bit2 and bit1 in HIGH state */
		return (byte) (OPLRead(chip, a) | 0x06);
	}

	public byte ym3526_peek(FmOPL chip, int a) {
		/* YM3526 always returns bit2 and bit1 in HIGH state */
		return (byte) (OPLRead(chip, a) | 0x06);
	}

	/**
	 * @param OPL     the virtual YM3526
	 * @param buffer  output buffer pointer
	 * @param length  is the number of samples that should be generated
	 * @param context
	 */
	public void ym3526_update_one(FmOPL OPL, IntConsumer buffer, int length, EventScheduler context) {
		int rhythm = OPL.rhythm & 0x20;

		if (OPL != cur_chip) {
			cur_chip = OPL;
			/* rhythm slots */
			SLOT7_1 = OPL.pCh[7].slot[SLOT1];
			SLOT7_2 = OPL.pCh[7].slot[SLOT2];
			SLOT8_1 = OPL.pCh[8].slot[SLOT1];
			SLOT8_2 = OPL.pCh[8].slot[SLOT2];
		}
		for (int i = 0; i < length; i++) {
			output[0] = 0;

			advance_lfo(OPL);

			/* FM part */
			OPL_CALC_CH(OPL.pCh[0]);
			OPL_CALC_CH(OPL.pCh[1]);
			OPL_CALC_CH(OPL.pCh[2]);
			OPL_CALC_CH(OPL.pCh[3]);
			OPL_CALC_CH(OPL.pCh[4]);
			OPL_CALC_CH(OPL.pCh[5]);

			if (rhythm == 0) {
				OPL_CALC_CH(OPL.pCh[6]);
				OPL_CALC_CH(OPL.pCh[7]);
				OPL_CALC_CH(OPL.pCh[8]);
			} else { /* Rhythm part */
				OPL_CALC_RH(OPL.pCh, (OPL.noise_rng >> 0) & 1);
			}

			int lt = output[0];

			lt >>= FINAL_SH;

			/* limit check */
			if (lt < -32768) {
				lt = -32768;
			}
			if (lt > 32767) {
				lt = 32767;
			}
			short sh = (short) (lt & 0xffff);
//			if (cnt > -1) {
//				/* store to sound buffer */
//				fout.printf("c%04X, ", sh);
//				if (++cnt % 64 == 0) {
//					fout.println();
//				}
//			}
			buffer.accept(sh);

			advance(OPL);
		}
	}

	public abstract long maincpu_clk();

	public abstract void alarm_set(Event event, long start);

	public abstract void alarm_unset(Event event);

}
