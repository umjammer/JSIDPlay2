/**
 *                           ReSid Emulation
 *                           ---------------
 *  begin                : Fri Apr 4 2001
 *  copyright            : (C) 2001 by Simon White
 *  email                : s_a_white@email.com
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 * @author Ken Händel
 *
 */
package resid_builder;

import java.util.logging.Level;
import java.util.logging.Logger;

import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.EventScheduler;
import libsidplay.common.SamplingMethod;
import resid_builder.residfp.Filter6581;
import resid_builder.residfp.Filter8580;
import resid_builder.residfp.SID;
import sidplay.ini.intf.IConfig;
import sidplay.ini.intf.IEmulationSection;
import sidplay.ini.intf.IFilterSection;

public class ReSIDfp extends ReSIDBase {
	private static final Logger RESID = Logger.getLogger(ReSIDfp.class
			.getName());

	private final SID sid = new SID();

	public ReSIDfp(EventScheduler context, final int bufferSize) {
		super(context, bufferSize);
		reset((byte) 0);
	}

	@Override
	public void setFilter(IConfig config, int sidNum) {
		final Filter6581 filter6581 = sid.getFilter6581();
		final Filter8580 filter8580 = sid.getFilter8580();

		String filterName6581 = config.getEmulation().getFilterName(sidNum,
				Emulation.RESIDFP, ChipModel.MOS6581);
		String filterName8580 = config.getEmulation().getFilterName(sidNum,
				Emulation.RESIDFP, ChipModel.MOS8580);
		if (filterName6581 == null) {
			filter6581.setCurveAndDistortionDefaults();
		}
		if (filterName8580 == null) {
			filter8580.setCurveAndDistortionDefaults();
		}
		for (IFilterSection filter : config.getFilter()) {
			if (filter.getName().equals(filterName6581)
					&& filter.isReSIDfpFilter6581()) {
				filter6581.setCurveProperties(filter.getBaseresistance(),
						filter.getOffset(), filter.getSteepness(),
						filter.getMinimumfetresistance());
				filter6581.setDistortionProperties(filter.getAttenuation(),
						filter.getNonlinearity(), filter.getResonanceFactor());
				sid.set6581VoiceNonlinearity(filter.getVoiceNonlinearity());
				filter6581.setNonLinearity(filter.getVoiceNonlinearity());
			} else if (filter.getName().equals(filterName8580)
					&& filter.isReSIDfpFilter8580()) {
				filter8580.setCurveProperties(filter.getK(), filter.getB(), 0,
						0);
				filter8580.setDistortionProperties(0, 0,
						filter.getResonanceFactor());
			}
		}
	}

	@Override
	public void reset(final byte volume) {
		clocksSinceLastAccess();
		sid.reset();
		sid.write(0x18, volume);
	}

	@Override
	public byte read(int addr) {
		addr &= 0x1f;
		clock();
		return sid.read(addr);
	}

	@Override
	public void write(int addr, final byte data) {
		addr &= 0x1f;
		super.write(addr, data);
		if (RESID.isLoggable(Level.FINE)) {
			RESID.fine(String.format("write 0x%02x=0x%02x", addr, data));
		}

		clock();
		sid.write(addr, data);
	}

	@Override
	public void clock() {
		int cycles = clocksSinceLastAccess();
		bufferpos += sid.clock(cycles, buffer, bufferpos);
	}

	@Override
	public void setFilterEnable(IEmulationSection emulation, int sidNum) {
		boolean enable = emulation.isFilterEnable(sidNum);
		sid.getFilter6581().enable(enable);
		sid.getFilter8580().enable(enable);
	}

	@Override
	public void setVoiceMute(final int num, final boolean mute) {
		sid.mute(num, mute);
	}

	@Override
	public void setSampling(final double systemClock, final float freq,
			final SamplingMethod method) {
		sid.setSamplingParameters(systemClock, method, freq, 20000);
	}

	/**
	 * Gets the {@link SID} instance being used.
	 *
	 * @return The {@link SID} instance being used.
	 */
	public SID sid() {
		return sid;
	}

	/**
	 * Set the emulated SID model
	 * 
	 * @param model
	 */
	public void setChipModel(final ChipModel model) {
		sid.setChipModel(model);
	}

	@Override
	public ChipModel getChipModel() {
		return sid.getChipModel();
	}

	@Override
	public void input(int input) {
		sid.input(input);
	}

	// Standard component functions
	public static final String credits() {
		String m_credit = "MOS6581/8580 (SID) - Antti S. Lankila's resid-fp (distortion simulation):\n";
		m_credit += "\tCopyright (©) 1999-2004 Dag Lem <resid@nimrod.no>\n";
		m_credit += "\tCopyright (©) 2005-2011 Antti S. Lankila <alankila@bel.fi>\n";
		return m_credit;
	}

	public int getInputDigiBoost() {
		return sid.getInputDigiBoost();
	}
}
