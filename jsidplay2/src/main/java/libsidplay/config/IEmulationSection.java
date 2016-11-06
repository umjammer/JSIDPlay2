package libsidplay.config;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Engine;

public interface IEmulationSection {

	/**
	 * Getter of the SID engine to be used.
	 * 
	 * @return the emulation to be used
	 */
	Engine getEngine();

	/**
	 * Setter of the SID engine to be used.
	 * 
	 * @param emulation
	 *            emulation to be used
	 */
	void setEngine(Engine engine);

	/**
	 * Getter of the default emulation to be used.
	 * 
	 * @return the default emulation to be used
	 */
	Emulation getDefaultEmulation();

	/**
	 * Setter of the first SIDs user emulation to be used.
	 * 
	 * @param emulation
	 *            first SIDs user emulation to be used
	 */
	void setUserEmulation(Emulation emulation);

	/**
	 * Getter of the first SIDs user emulation to be used.
	 * 
	 * @return the first SIDs user emulation to be used
	 */
	Emulation getUserEmulation();

	/**
	 * Setter of the second SIDs user emulation to be used.
	 * 
	 * @param emulation
	 *            second SIDs user emulation to be used
	 */
	void setStereoEmulation(Emulation emulation);

	/**
	 * Getter of the second SIDs user emulation to be used.
	 * 
	 * @return the second SIDs user emulation to be used
	 */
	Emulation getStereoEmulation();

	/**
	 * Setter of the third SIDs user emulation to be used.
	 * 
	 * @param emulation
	 *            third SIDs user emulation to be used
	 */
	void setThirdEmulation(Emulation emulation);

	/**
	 * Getter of the third SIDs user emulation to be used.
	 * 
	 * @return the third SIDs user emulation to be used
	 */
	Emulation getThirdEmulation();

	/**
	 * Setter of the default emulation to be used.
	 * 
	 * @param emulation
	 *            default emulation to be used
	 */
	void setDefaultEmulation(Emulation emulation);

	/**
	 * Getter of the default clock speed.
	 * 
	 * @return the default clock speed
	 */
	CPUClock getDefaultClockSpeed();

	/**
	 * Setter of the default clock speed.
	 * 
	 * @param speed
	 *            default clock speed
	 */
	void setDefaultClockSpeed(CPUClock speed);

	/**
	 * Getter of user the clock speed.
	 * 
	 * @return the user clock speed
	 */
	CPUClock getUserClockSpeed();

	/**
	 * Setter of the user clock speed.
	 * 
	 * @param speed
	 *            user clock speed
	 */
	void setUserClockSpeed(CPUClock speed);

	/**
	 * Getter of the default SID model.
	 * 
	 * @return the default SID model
	 */
	ChipModel getDefaultSidModel();

	/**
	 * Setter of the default SID model.
	 * 
	 * @param model
	 *            the default SID model
	 */
	void setDefaultSidModel(ChipModel model);

	/**
	 * Getter of the user SID model.
	 * 
	 * @return the user SID model
	 */
	ChipModel getUserSidModel();

	/**
	 * Setter of the user SID model.
	 * 
	 * @param model
	 *            user SID model
	 */
	void setUserSidModel(ChipModel model);

	/**
	 * Getter of the chip to be used for MOS6581.
	 * 
	 * @return the chip to be used for MOS6581
	 */
	int getHardsid6581();

	/**
	 * Setter of the chip to be used for MOS6581.
	 * 
	 * @param chip
	 *            the chip to be used for MOS6581
	 */
	void setHardsid6581(int chip);

	/**
	 * Getter of the chip to be used for CSG8580.
	 * 
	 * @return the chip to be used for CSG8580
	 */
	int getHardsid8580();

	/**
	 * Setter of the chip to be used for CSG8580.
	 * 
	 * @param chip
	 *            the chip to be used for CSG8580
	 */
	void setHardsid8580(int chip);

	/**
	 * Is SID filter enabled?
	 * 
	 * @return filter enabled
	 */
	boolean isFilter();

	/**
	 * Is stereo SID filter enabled?
	 * 
	 * @return stereo filter enabled
	 */
	boolean isStereoFilter();

	/**
	 * Is 3-SID filter enabled?
	 * 
	 * @return 3-SID filter enabled
	 */
	boolean isThirdSIDFilter();

	/**
	 * Setter of the filter enable.
	 * 
	 * @param enable
	 *            the filter enable
	 */
	void setFilter(boolean enable);

	/**
	 * Setter of the stereo filter enable.
	 * 
	 * @param enable
	 *            the stereo filter enable
	 */
	void setStereoFilter(boolean enable);

	/**
	 * Setter of the 3-SID filter enable.
	 * 
	 * @param enable
	 *            the 3-SID filter enable
	 */
	void setThirdSIDFilter(boolean enable);

	/**
	 * Getter of the filter setting of MOS6581.
	 * 
	 * @return the filter setting of MOS6581
	 */
	String getFilter6581();

	/**
	 * Getter of the stereo filter setting of MOS6581.
	 * 
	 * @return the stereo filter setting of MOS6581
	 */
	String getStereoFilter6581();

	/**
	 * Getter of the 3-SID filter setting of MOS6581.
	 * 
	 * @return the 3-SID filter setting of MOS6581
	 */
	String getThirdSIDFilter6581();

	/**
	 * Setter of the filter setting of MOS6581.
	 * 
	 * @param filterName
	 *            filter setting of MOS6581
	 */
	void setFilter6581(String filterName);

	/**
	 * Setter of the stereo filter setting of MOS6581.
	 * 
	 * @param filterName
	 *            stereo filter setting of MOS6581
	 */
	void setStereoFilter6581(String filterName);

	/**
	 * Setter of the 3-SID filter setting of MOS6581.
	 * 
	 * @param filterName
	 *            3-SID filter setting of MOS6581
	 */
	void setThirdSIDFilter6581(String filterName);

	/**
	 * Getter of the filter setting of CSG8580.
	 * 
	 * @return the filter setting of CSG8580
	 */
	String getFilter8580();

	/**
	 * Getter of the stereo filter setting of CSG8580.
	 * 
	 * @return the stereo filter setting of CSG8580
	 */
	String getStereoFilter8580();

	/**
	 * Getter of the 3-SID filter setting of CSG8580.
	 * 
	 * @return the 3-SID filter setting of CSG8580
	 */
	String getThirdSIDFilter8580();

	/**
	 * Setter of the filter setting of CSG8680.
	 * 
	 * @param filterName
	 *            filter setting of CSG8680
	 */
	void setFilter8580(String filterName);

	/**
	 * Setter of stereo the filter setting of CSG8680.
	 * 
	 * @param filterName
	 *            stereo filter setting of CSG8680
	 */
	void setStereoFilter8580(String filterName);

	/**
	 * Setter of the 3-SID filter setting of CSG8680.
	 * 
	 * @param filterName
	 *            3-SID filter setting of CSG8680
	 */
	void setThirdSIDFilter8580(String filterName);

	/**
	 * Getter of the filter setting of MOS6581 for ReSIDfp.
	 * 
	 * @return the filter setting of MOS6581 for ReSIDfp
	 */
	String getReSIDfpFilter6581();

	/**
	 * Getter of the stereo filter setting of MOS6581 for ReSIDfp.
	 * 
	 * @return the stereo filter setting of MOS6581 for ReSIDfp
	 */
	String getReSIDfpStereoFilter6581();

	/**
	 * Getter of the 3-SID filter setting of MOS6581 for ReSIDfp.
	 * 
	 * @return the 3-SID filter setting of MOS6581 for ReSIDfp
	 */
	String getReSIDfpThirdSIDFilter6581();

	/**
	 * Setter of the filter setting of MOS6581 for ReSIDfp.
	 * 
	 * @param filterName
	 *            filter setting of MOS6581 for ReSIDfp
	 */
	void setReSIDfpFilter6581(String filterName);

	/**
	 * Setter of the stereo filter setting of MOS6581 for ReSIDfp.
	 * 
	 * @param filterName
	 *            stereo filter setting of MOS6581 for ReSIDfp
	 */
	void setReSIDfpStereoFilter6581(String filterName);

	/**
	 * Setter of the 3-SID filter setting of MOS6581 for ReSIDfp.
	 * 
	 * @param filterName
	 *            3-SID filter setting of MOS6581 for ReSIDfp
	 */
	void setReSIDfpThirdSIDFilter6581(String filterName);

	/**
	 * Getter of the filter setting of CSG8580.
	 * 
	 * @return the filter setting of CSG8580
	 */
	String getReSIDfpFilter8580();

	/**
	 * Getter of the stereo filter setting of CSG8580.
	 * 
	 * @return the stereo filter setting of CSG8580
	 */
	String getReSIDfpStereoFilter8580();

	/**
	 * Getter of the 3-SID filter setting of CSG8580.
	 * 
	 * @return the 3-SID filter setting of CSG8580
	 */
	String getReSIDfpThirdSIDFilter8580();

	/**
	 * Setter of the filter setting of CSG8680.
	 * 
	 * @param filterName
	 *            filter setting of CSG8680
	 */
	void setReSIDfpFilter8580(String filterName);

	/**
	 * Setter of the stereo filter setting of CSG8680.
	 * 
	 * @param filterName
	 *            stereo filter setting of CSG8680
	 */
	void setReSIDfpStereoFilter8580(String filterName);

	/**
	 * Setter of the 3-SID filter setting of CSG8680.
	 * 
	 * @param filterName
	 *            3-SID filter setting of CSG8680
	 */
	void setReSIDfpThirdSIDFilter8580(String filterName);

	/**
	 * Getter of the enable SID digi-boost.
	 * 
	 * @return the enable SID digi-boost
	 */
	boolean isDigiBoosted8580();

	/**
	 * setter of the enable SID digi-boost.
	 * 
	 * @param boost
	 *            the enable SID digi-boost
	 */
	void setDigiBoosted8580(boolean boost);

	/**
	 * Getter of the stereo SID base address.
	 * 
	 * @return the stereo SID base address
	 */
	int getDualSidBase();

	int getThirdSIDBase();

	/**
	 * Setter of the stereo SID base address.
	 * 
	 * @param base
	 *            stereo SID base address
	 */
	void setDualSidBase(int base);

	/**
	 * Setter of the 3-SID base address.
	 * 
	 * @param base
	 *            3-SID base address
	 */
	void setThirdSIDBase(int base);

	/**
	 * @return SID chip to read from (fake stereo)
	 */
	int getSidNumToRead();

	/**
	 * Setter of the SID chip to read from (fake stereo).
	 * 
	 * @param sidNumToRead
	 *            SID chip to read from (fake stereo)
	 */
	void setSidNumToRead(int sidNumToRead);

	/**
	 * Getter of the fake stereo mode.
	 * 
	 * @return the fake stereo mode
	 */
	boolean isFakeStereo();

	/**
	 * Setter of the fake stereo mode.
	 * 
	 * @param fakeStereo
	 *            fake stereo mode
	 */
	void setFakeStereo(boolean fakeStereo);

	/**
	 * Getter of the forced playback stereo mode.
	 * 
	 * @return the forced playback stereo mode
	 */
	boolean isForceStereoTune();

	/**
	 * Getter of the forced playback 3-SID mode.
	 * 
	 * @return the forced playback 3-SID mode
	 */
	boolean isForce3SIDTune();

	/**
	 * Setter of the forced playback stereo mode.
	 * 
	 * @param force
	 *            forced playback stereo mode
	 */
	void setForceStereoTune(boolean force);

	/**
	 * Setter of the forced playback 3-SID mode.
	 * 
	 * @param force
	 *            forced playback 3-SID mode
	 */
	void setForce3SIDTune(boolean force);

	/**
	 * Getter of mute voice 1
	 * 
	 * @return mute voice 1
	 */
	boolean isMuteVoice1();

	/**
	 * Setter of mute voice 1
	 * 
	 * @param mute
	 *            mute voice 1
	 */
	void setMuteVoice1(boolean mute);

	/**
	 * Getter of mute voice 2
	 * 
	 * @return mute voice 2
	 */
	boolean isMuteVoice2();

	/**
	 * Setter of mute voice 2
	 * 
	 * @param mute
	 *            mute voice 2
	 */
	void setMuteVoice2(boolean mute);

	/**
	 * Getter of mute voice 3
	 * 
	 * @return mute voice 3
	 */
	boolean isMuteVoice3();

	/**
	 * Setter of mute voice 3
	 * 
	 * @param mute
	 *            mute voice 3
	 */
	void setMuteVoice3(boolean mute);

	/**
	 * Getter of mute voice 4
	 * 
	 * @return mute voice 4
	 */
	boolean isMuteVoice4();

	/**
	 * Setter of mute voice 4
	 * 
	 * @param mute
	 *            mute voice 4
	 */
	void setMuteVoice4(boolean mute);

	/**
	 * Getter of stereo mute voice 1
	 * 
	 * @return stereo mute voice 1
	 */
	boolean isMuteStereoVoice1();

	/**
	 * Setter of stereo mute voice 1
	 * 
	 * @param mute
	 *            stereo mute voice 1
	 */
	void setMuteStereoVoice1(boolean mute);

	/**
	 * Getter of stereo mute voice 2
	 * 
	 * @return stereo mute voice 2
	 */
	boolean isMuteStereoVoice2();

	/**
	 * Setter of stereo mute voice 2
	 * 
	 * @param mute
	 *            stereo mute voice 2
	 */
	void setMuteStereoVoice2(boolean mute);

	/**
	 * Getter of stereo mute voice 3
	 * 
	 * @return stereo mute voice 3
	 */
	boolean isMuteStereoVoice3();

	/**
	 * Setter of stereo mute voice 3
	 * 
	 * @param mute
	 *            stereo mute voice 3
	 */
	void setMuteStereoVoice3(boolean mute);

	/**
	 * Getter of stereo mute voice 4
	 * 
	 * @return stereo mute voice 4
	 */
	boolean isMuteStereoVoice4();

	/**
	 * Setter of stereo mute voice 4
	 * 
	 * @param mute
	 *            stereo mute voice 4
	 */
	void setMuteStereoVoice4(boolean mute);

	/**
	 * Getter of 3-SID mute voice 1
	 * 
	 * @return 3-SID mute voice 1
	 */
	boolean isMuteThirdSIDVoice1();

	/**
	 * Setter of 3-SID mute voice 1
	 * 
	 * @param mute
	 *            3-SID mute voice 1
	 */
	void setMuteThirdSIDVoice1(boolean mute);

	/**
	 * Getter of 3-SID mute voice 2
	 * 
	 * @return 3-SID mute voice 2
	 */
	boolean isMuteThirdSIDVoice2();

	/**
	 * Setter of 3-SID mute voice 2
	 * 
	 * @param mute
	 *            3-SID mute voice 2
	 */
	void setMuteThirdSIDVoice2(boolean mute);

	/**
	 * Getter of 3-SID mute voice 3
	 * 
	 * @return 3-SID mute voice 3
	 */
	boolean isMuteThirdSIDVoice3();

	/**
	 * Setter of 3-SID mute voice 3
	 * 
	 * @param mute
	 *            3-SID mute voice 3
	 */
	void setMuteThirdSIDVoice3(boolean mute);

	/**
	 * Getter of 3-SID mute voice 4
	 * 
	 * @return 3-SID mute voice 4
	 */
	boolean isMuteThirdSIDVoice4();

	/**
	 * Setter of 3-SID mute voice 4
	 * 
	 * @param mute
	 *            3-SID mute voice 4
	 */
	void setMuteThirdSIDVoice4(boolean mute);

	/**
	 * Getter of the the stereo SID model.
	 * 
	 * @return the stereo SID model
	 */
	ChipModel getStereoSidModel();

	/**
	 * Setter of the the stereo SID model.
	 * 
	 * @param model
	 *            the the stereo SID model
	 */
	void setStereoSidModel(ChipModel model);

	/**
	 * Getter of the the 3-SID model.
	 * 
	 * @return the 3-SID model
	 */
	ChipModel getThirdSIDModel();

	/**
	 * Setter of the the 3-SID model.
	 * 
	 * @param model
	 *            the the 3-SID model
	 */
	void setThirdSIDModel(ChipModel model);

	/**
	 * Get filter enable depending of the SID number
	 * 
	 * @param sidNum
	 *            SID number
	 * @return filter enable
	 */
	default boolean isFilterEnable(int sidNum) {
		switch (sidNum) {
		case 0:
			return isFilter();
		case 1:
			return isStereoFilter();
		case 2:
			return isThirdSIDFilter();

		default:
			throw new RuntimeException("Maximum SIDs exceeded: " + sidNum + "!");
		}
	}

	/**
	 * Set filter enable depending of the SID number
	 * 
	 * @param sidNum
	 *            SID number
	 * @param enable
	 *            filter enable
	 */
	default void setFilterEnable(int sidNum, boolean enable) {
		switch (sidNum) {
		case 0:
			setFilter(enable);
			break;
		case 1:
			setStereoFilter(enable);
			break;
		case 2:
			setThirdSIDFilter(enable);
			break;

		default:
			throw new RuntimeException("Maximum SIDs exceeded: " + sidNum + "!");
		}
	}

	/**
	 * Get the current filter name depending of the SID number, emulation type
	 * and chip model.
	 * 
	 * @param sidNum
	 *            SId number
	 * @param emulation
	 *            emulation type
	 * @param chipModel
	 *            SID chip model
	 * @return the current filter name
	 */
	default String getFilterName(int sidNum, Emulation emulation, ChipModel chipModel) {
		switch (chipModel) {
		case MOS6581:
			switch (emulation) {
			case RESID:
				switch (sidNum) {
				case 0:
					return getFilter6581();

				case 1:
					return getStereoFilter6581();

				case 2:
					return getThirdSIDFilter6581();

				default:
					throw new RuntimeException("Maximum SIDs exceeded: " + sidNum + "!");

				}
			case RESIDFP:
				switch (sidNum) {
				case 0:
					return getReSIDfpFilter6581();

				case 1:
					return getReSIDfpStereoFilter6581();

				case 2:
					return getReSIDfpThirdSIDFilter6581();

				default:
					throw new RuntimeException("Maximum SIDs exceeded: " + sidNum + "!");

				}
			default:
				throw new RuntimeException("Unknown emulation type: " + emulation + "!");
			}

		case MOS8580:
			switch (emulation) {
			case RESID:
				switch (sidNum) {
				case 0:
					return getFilter8580();

				case 1:
					return getStereoFilter8580();

				case 2:
					return getThirdSIDFilter8580();

				default:
					throw new RuntimeException("Maximum SIDs exceeded: " + sidNum + "!");

				}
			case RESIDFP:
				switch (sidNum) {
				case 0:
					return getReSIDfpFilter8580();

				case 1:
					return getReSIDfpStereoFilter8580();

				case 2:
					return getReSIDfpThirdSIDFilter8580();

				default:
					throw new RuntimeException("Maximum SIDs exceeded: " + sidNum + "!");

				}
			default:
				throw new RuntimeException("Unknown emulation type: " + emulation + "!");
			}
		default:
			throw new RuntimeException("Unknown chip model: " + chipModel + "!");
		}
	}

	/**
	 * Set the current filter name depending of the SID number, emulation type
	 * and chip model.
	 *
	 * @param sidNum
	 *            SId number
	 * @param emulation
	 *            emulation type
	 * @param chipModel
	 *            SID chip model
	 * @param filterName
	 *            filter name
	 */
	default void setFilterName(int sidNum, Emulation emulation, ChipModel chipModel, String filterName) {
		switch (chipModel) {
		case MOS6581:
			switch (emulation) {
			case RESID:
				switch (sidNum) {
				case 0:
					setFilter6581(filterName);
					break;
				case 1:
					setStereoFilter6581(filterName);
					break;

				case 2:
					setThirdSIDFilter6581(filterName);
					break;

				default:
					throw new RuntimeException("Maximum SIDs exceeded: " + sidNum + "!");

				}
				break;
			case RESIDFP:
				switch (sidNum) {
				case 0:
					setReSIDfpFilter6581(filterName);
					break;

				case 1:
					setReSIDfpStereoFilter6581(filterName);
					break;

				case 2:
					setReSIDfpThirdSIDFilter6581(filterName);
					break;

				default:
					throw new RuntimeException("Maximum SIDs exceeded: " + sidNum + "!");

				}
				break;
			default:
				throw new RuntimeException("Unknown emulation type: " + emulation + "!");
			}
			break;

		case MOS8580:
			switch (emulation) {
			case RESID:
				switch (sidNum) {
				case 0:
					setFilter8580(filterName);
					break;

				case 1:
					setStereoFilter8580(filterName);
					break;

				case 2:
					setThirdSIDFilter8580(filterName);
					break;

				default:
					throw new RuntimeException("Maximum SIDs exceeded: " + sidNum + "!");

				}
				break;
			case RESIDFP:
				switch (sidNum) {
				case 0:
					setReSIDfpFilter8580(filterName);
					break;

				case 1:
					setReSIDfpStereoFilter8580(filterName);
					break;

				case 2:
					setReSIDfpThirdSIDFilter8580(filterName);
					break;

				default:
					throw new RuntimeException("Maximum SIDs exceeded: " + sidNum + "!");

				}
				break;
			default:
				throw new RuntimeException("Unknown emulation type: " + emulation + "!");
			}
			break;
		default:
			throw new RuntimeException("Unknown chip model: " + chipModel + "!");
		}
	}

	default boolean isMuteVoice(int sidNum, int voice) {
		switch (sidNum) {
		case 0:
			switch (voice) {
			case 0:
				return isMuteVoice1();
			case 1:
				return isMuteVoice2();
			case 2:
				return isMuteVoice3();
			case 3:
				return isMuteVoice4();
			default:
				throw new RuntimeException("Unknown voice: " + voice + "!");
			}
		case 1:
			switch (voice) {
			case 0:
				return isMuteStereoVoice1();
			case 1:
				return isMuteStereoVoice2();
			case 2:
				return isMuteStereoVoice3();
			case 3:
				return isMuteStereoVoice4();
			default:
				throw new RuntimeException("Unknown voice: " + voice + "!");
			}
		case 2:
			switch (voice) {
			case 0:
				return isMuteThirdSIDVoice1();
			case 1:
				return isMuteThirdSIDVoice2();
			case 2:
				return isMuteThirdSIDVoice3();
			case 3:
				return isMuteThirdSIDVoice4();
			default:
				throw new RuntimeException("Unknown voice: " + voice + "!");
			}
		default:
			throw new RuntimeException("Maximum SIDs exceeded: " + sidNum + "!");
		}
	}
}