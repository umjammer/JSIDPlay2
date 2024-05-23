package client.teavm.common.video;

import libsidplay.components.mos656x.IPALEmulation;

/**
 * To save performance we calculate and render only every Nth frame.
 */
public interface IPALEmulationTeaVM extends IPALEmulation {

	int getNthFrame();

	void setNthFrame(int nthFrame);

}
