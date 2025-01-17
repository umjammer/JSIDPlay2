/**
 * This file is part of reSID, a MOS6581 SID emulator engine.
 * Copyright (C) 2004  Dag Lem <resid@nimrod.no>
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 * @author Ken Händel
 *
 */
package builder.resid.residfp;

import java.util.function.IntConsumer;

import libsidplay.common.ChipModel;
import libsidplay.common.Potentiometer;
import libsidplay.common.SIDChip;

public class SID implements SIDChip {
	private static final int INPUTDIGIBOOST = -0x9500;

	/**
	 * Output scaler.
	 */
	private static final float OUTPUT_LEVEL = 32768f / (2047.f * 255.f * 3.0f * 2.0f);

	/** SID voices */
	public final Voice[] voice = new Voice[] { new Voice(), new Voice(), new Voice() };

	/** Currently active filter */
	private Filter filter;

	/** Filter used, if model is set to 6581 */
	private final Filter6581 filter6581 = new Filter6581();

	/** Filter used, if model is set to 8580 */
	private final Filter8580 filter8580 = new Filter8580();

	/**
	 * External filter that provides high-pass and low-pass filtering to adjust
	 * sound tone slightly.
	 */
	private final ExternalFilter externalFilter = new ExternalFilter();

	/** Paddle X register support */
	private final Potentiometer potX = new Potentiometer() {
	};

	/** Paddle Y register support */
	private final Potentiometer potY = new Potentiometer() {
	};

	/** Last written value */
	private byte busValue;

	/** Time to live for the last written value */
	private int busValueTtl, databus_ttl;

	/**
	 * Currently active chip model.
	 */
	private ChipModel model;

	/** External audio input. */
	private float ext_in;

	/** 6581 nonlinearity term used for all DACs */
	private float nonLinearity6581;

	private boolean samplesMuted;

	/**
	 * Set DAC nonlinearity for 6581 emulation.
	 *
	 * @param nonLinearity
	 *
	 * @see SID#kinkedDac(int, float, int)
	 */
	public void set6581VoiceNonlinearity(final float nonLinearity) {
		if (nonLinearity == nonLinearity6581) {
			return;
		}
		nonLinearity6581 = nonLinearity;
		if (model == ChipModel.MOS6581) {
			setChipModel(model);
		}
	}

	/**
	 * Estimate DAC nonlinearity. The SID contains R-2R ladder, but the second
	 * resistor is not exactly double the first. The parameter nonLinearity models
	 * the deviation from the resistor lengths. There appears to be about 4 % error
	 * on the 6581, resulting in major kinks on the DAC. The value that the DAC
	 * yields tends to be larger than expected. The output of this method is
	 * normalized such that DAC errors occur both above and below the ideal value
	 * equally.
	 *
	 * @param input        digital value to convert to analog
	 * @param nonLinearity nonlinearity parameter, 1.0 for perfect linearity.
	 * @param maxBit       highest bit that may be set in input.
	 * @return the analog value as modeled from the R-2R network.
	 */
	static float kinkedDac(final int input, final float nonLinearity, final int maxBit) {
		float value = 0f;
		int currentBit = 1;
		float weight = 1f;
		final float dir = 2f * nonLinearity;
		for (int i = 0; i < maxBit; i++) {
			if ((input & currentBit) != 0) {
				value += weight;
			}
			currentBit <<= 1;
			weight *= dir;
		}

		return value / (weight / nonLinearity / nonLinearity) * (1 << maxBit);
	}

	/**
	 * Constructor.
	 */
	public SID() {
		set6581VoiceNonlinearity(0.96f);
		setChipModel(ChipModel.MOS8580);
		reset();
	}

	/**
	 * Set chip model.
	 *
	 * @param model chip model to use
	 */
	@Override
	public void setChipModel(final ChipModel model) {
		this.model = model;
		/*
		 * results from real C64 (testprogs/SID/bitfade/delayfrq0.prg):
		 * 
		 * (new SID) (250469/8580R5) (250469/8580R5) delayfrq0 ~7a000 ~108000
		 * 
		 * (old SID) (250407/6581) delayfrq0 ~01d00
		 * 
		 */
		databus_ttl = model == ChipModel.MOS8580 ? 0xa2000 : 0x1d00;

		final float nonLinearity;
		if (model == ChipModel.MOS6581) {
			filter6581.setNonLinearity(nonLinearity6581);
			filter = filter6581;
			nonLinearity = nonLinearity6581;
		} else if (model == ChipModel.MOS8580) {
			filter = filter8580;
			nonLinearity = 1f;
		} else {
			throw new RuntimeException("Don't know how to handle chip type " + model);
		}

		/* calculate waveform-related tables, feed them to the generator */
		final Object[] tables = WaveformCalculator.rebuildWftable(model, nonLinearity);

		/* update voice offsets */
		for (int i = 0; i < 3; i++) {
			voice[i].setChipModel(model);
			voice[i].envelope.setNonLinearity(nonLinearity);
			voice[i].wave.setWftable((float[][]) tables[0], (float[]) tables[1], (byte[][]) tables[2]);
		}
	}

	protected ChipModel getChipModel() {
		return model;
	}

	/**
	 * SID reset.
	 */
	@Override
	public void reset() {
		for (int i = 0; i < 3; i++) {
			voice[i].reset();
		}
		filter6581.reset();
		filter8580.reset();
		externalFilter.reset();

		busValue = 0;
		busValueTtl = 0;
		samplesMuted = false;
	}

	/**
	 * 16-bit input (EXT IN). Write 16-bit sample to audio input. NB! The caller is
	 * responsible for keeping the value within 16 bits. Note that to mix in an
	 * external audio signal, the signal should be resampled to 1MHz first to avoid
	 * sampling noise.
	 *
	 * @param value input level to set
	 */
	@Override
	public void input(final int value) {
		// Voice outputs are 20 bits. Scale up to match three voices in order
		// to facilitate simulation of the MOS8580 "digi boost" hardware hack.
		ext_in = (value << 4) * 3;
	}

	/**
	 * Read registers.
	 * <P>
	 * Reading a write only register returns the last byte written to any SID
	 * register. The individual bits in this value start to fade down towards zero
	 * after a few cycles. All bits reach zero within approximately $2000 - $4000
	 * cycles. It has been claimed that this fading happens in an orderly fashion,
	 * however sampling of write only registers reveals that this is not the case.
	 * NB! This is not correctly modeled. The actual use of write only registers has
	 * largely been made in the belief that all SID registers are readable. To
	 * support this belief the read would have to be done immediately after a write
	 * to the same register (remember that an intermediate write to another register
	 * would yield that value instead). With this in mind we return the last value
	 * written to any SID register for $2000 cycles without modeling the bit fading.
	 *
	 * @param offset SID register to read
	 * @return value read from chip
	 */
	@Override
	public byte read(final int offset) {
		final byte value;
		switch (offset) {
		case 0x19:
			value = potX.readPOT();

			busValueTtl = databus_ttl;
			break;
		case 0x1a:
			value = potY.readPOT();
			busValueTtl = databus_ttl;
			break;
		case 0x1b:
			value = model == ChipModel.MOS6581 ? voice[2].wave.readOSC6581(voice[0].wave)
					: voice[2].wave.readOSC8580(voice[0].wave);
			break;
		case 0x1c:
			value = voice[2].envelope.readENV();
			busValueTtl = databus_ttl;
			break;
		default:
			value = busValue;
			busValueTtl /= 2;
			break;
		}

		busValue = value;
		return value;
	}

	/**
	 * Write registers.
	 *
	 * @param offset chip register to write
	 * @param value  value to write
	 */
	@Override
	public void write(final int offset, final byte value) {
		busValue = value;
		busValueTtl = databus_ttl;

		switch (offset) {
		case 0x00:
			voice[0].wave.writeFREQ_LO(value);
			break;
		case 0x01:
			voice[0].wave.writeFREQ_HI(value);
			break;
		case 0x02:
			voice[0].wave.writePW_LO(value);
			break;
		case 0x03:
			voice[0].wave.writePW_HI(value);
			break;
		case 0x04:
			voice[0].writeCONTROL_REG(voice[1].wave, value);
			break;
		case 0x05:
			voice[0].envelope.writeATTACK_DECAY(value);
			break;
		case 0x06:
			voice[0].envelope.writeSUSTAIN_RELEASE(value);
			break;
		case 0x07:
			voice[1].wave.writeFREQ_LO(value);
			break;
		case 0x08:
			voice[1].wave.writeFREQ_HI(value);
			break;
		case 0x09:
			voice[1].wave.writePW_LO(value);
			break;
		case 0x0a:
			voice[1].wave.writePW_HI(value);
			break;
		case 0x0b:
			voice[1].writeCONTROL_REG(voice[2].wave, value);
			break;
		case 0x0c:
			voice[1].envelope.writeATTACK_DECAY(value);
			break;
		case 0x0d:
			voice[1].envelope.writeSUSTAIN_RELEASE(value);
			break;
		case 0x0e:
			voice[2].wave.writeFREQ_LO(value);
			break;
		case 0x0f:
			voice[2].wave.writeFREQ_HI(value);
			break;
		case 0x10:
			voice[2].wave.writePW_LO(value);
			break;
		case 0x11:
			voice[2].wave.writePW_HI(value);
			break;
		case 0x12:
			voice[2].writeCONTROL_REG(voice[0].wave, value);
			break;
		case 0x13:
			voice[2].envelope.writeATTACK_DECAY(value);
			break;
		case 0x14:
			voice[2].envelope.writeSUSTAIN_RELEASE(value);
			break;
		case 0x15:
			filter6581.writeFC_LO(value);
			filter8580.writeFC_LO(value);
			break;
		case 0x16:
			filter6581.writeFC_HI(value);
			filter8580.writeFC_HI(value);
			break;
		case 0x17:
			filter6581.writeRES_FILT(value);
			filter8580.writeRES_FILT(value);
			break;
		case 0x18:
			// samples muted? Fade-in is allowed anyway
			if (!samplesMuted || (value & 0xf) / 15.f >= filter6581.vol) {
				filter6581.writeMODE_VOL(value);
				filter8580.writeMODE_VOL(value);
			}
			break;
		default:
			break;
		}
	}

	/**
	 * SID voice muting.
	 *
	 * @param channel channel to modify
	 * @param mute    is muted?
	 */
	@Override
	public void mute(final int channel, final boolean mute) {
		if (channel < 3) {
			voice[channel].mute(mute);
		} else {
			samplesMuted = mute;
		}
	}

	/**
	 * Setting of clock frequency.
	 *
	 * @param clockFrequency System clock frequency at Hz
	 */
	@Override
	public void setClockFrequency(final double clockFrequency) {
		filter6581.setClockFrequency(clockFrequency);
		filter8580.setClockFrequency(clockFrequency);
		externalFilter.setClockFrequency(clockFrequency);
	}

	private void ageBusValue(final int n) {
		if (busValueTtl != 0) {
			busValueTtl -= n;
			if (busValueTtl <= 0) {
				busValue = 0;
				busValueTtl = 0;
			}
		}
	}

	/**
	 * Clock SID forward using chosen output sampling algorithm.
	 *
	 * @param delta_t c64 clocks to clock
	 * @param sample  sample consumer
	 */
	@Override
	public final void clock(final int delta_t, IntConsumer sample) {
		ageBusValue(delta_t);

		for (int i = 0; i < delta_t; i++) {
			sample.accept((int) (clock() * OUTPUT_LEVEL));
		}
		filter.zeroDenormals();
		externalFilter.zeroDenormals();
	}

	/**
	 * SID clocking - 1 cycle.
	 */
	private float clock() {
		/* clock waveform generators */
		voice[0].wave.clock();
		voice[1].wave.clock();
		voice[2].wave.clock();

		/* emulate SYNC bit */
		voice[0].wave.synchronize(voice[1].wave, voice[2].wave);
		voice[1].wave.synchronize(voice[2].wave, voice[0].wave);
		voice[2].wave.synchronize(voice[0].wave, voice[1].wave);

		/* clock envelope generators */
		voice[0].envelope.clock();
		voice[1].envelope.clock();
		voice[2].envelope.clock();

		return externalFilter.clock(filter.clock(voice[0].output(voice[2].wave), voice[1].output(voice[0].wave),
				voice[2].output(voice[1].wave), ext_in));
	}

	public Filter6581 getFilter6581() {
		return filter6581;
	}

	public Filter8580 getFilter8580() {
		return filter8580;
	}

	@Override
	public void setDigiBoost(boolean digiBoost) {
		if (digiBoost && model.equals(ChipModel.MOS8580)) {
			input(INPUTDIGIBOOST);
		} else {
			input(0);
		}
	}

}
