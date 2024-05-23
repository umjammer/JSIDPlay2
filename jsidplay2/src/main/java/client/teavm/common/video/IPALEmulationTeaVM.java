package client.teavm.common.video;

import libsidplay.components.mos656x.IPALEmulation;

public interface IPALEmulationTeaVM extends IPALEmulation {

	int getNthFrame();

	void setNthFrame(int nthFrame);

}
