package client.teavm.common;

import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.SidReads;
import libsidplay.common.StereoMode;
import libsidplay.components.keyboard.KeyTableEntry;
import libsidplay.sidtune.SidTuneError;

/**
 * Exports to JavaScript.
 */
public interface IExportedApi {

	void open(byte[] sidContents, String sidContentsName, int song, int nthFrame, boolean addSidListener,
		byte[] cartContents, String cartContentsName, String command)
		throws IOException, SidTuneError, LineUnavailableException, InterruptedException;

	void typeInCommand(String multiLineCommand);

	void clock() throws InterruptedException;

	void insertDisk(byte[] diskContents, String diskContentsName);

	void ejectDisk();

	void insertTape(byte[] tapeContents, String tapeContentsName);

	void ejectTape();

	void pressPlayOnTape();

	void typeKey(KeyTableEntry keyTableEntry);

	void pressKey(KeyTableEntry keyTableEntry);

	void releaseKey(KeyTableEntry keyTableEntry);

	void delaySidBlaster(int cycles);

	void joystick(int number, int value);

	void volumeLevels(float mainVolume, float secondVolume, float thirdVolume, float mainBalance, float secondBalance,
		float thirdBalance, int mainDelay, int secondDelay, int thirdDelay);

	void stereo(StereoMode stereoMode, int dualSidBase, int thirdSIDBase, boolean fakeStereo, SidReads sidToRead);

	void defaultEmulation(Emulation emulation);

	void defaultChipModel(ChipModel chipModel);

	void filterName(Emulation emulation, ChipModel chipModel, int sidNum, String filterName);

	void mute(int sidNum, int voice, boolean value);

}
