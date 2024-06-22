package client.teavm.js;

import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;

import org.teavm.jso.JSExport;

import client.teavm.common.ExportedApi;
import client.teavm.common.IExportedApi;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.SidReads;
import libsidplay.common.StereoMode;
import libsidplay.components.keyboard.KeyTableEntry;
import libsidplay.sidtune.SidTuneError;

/**
 * Main class of the TeaVM version of JSIDPlay2 to generate JavaScript code.
 */
public class JSIDPlay2TeaVM {

	private static IExportedApi jsidplay2;

	public static void main(String[] args) {
		jsidplay2 = new ExportedApi(new ImportedApi(args));
	}

	//
	// Exports to JavaScript
	//

	@JSExport
	public static void open(byte[] sidContents, String sidContentsName, int song, int nthFrame, boolean addSidListener,
			byte[] cartContents, String cartContentsName, String command)
			throws IOException, SidTuneError, LineUnavailableException, InterruptedException {
		jsidplay2.open(sidContents, sidContentsName, song, nthFrame, addSidListener, cartContents, cartContentsName,
				command);
	}

	@JSExport
	public static void typeInCommand(final String nameFromJS) {
		jsidplay2.typeInCommand(nameFromJS);
	}

	@JSExport
	public static void clock() throws InterruptedException {
		jsidplay2.clock();
	}

	@JSExport
	public static void insertDisk(byte[] diskContents, String diskContentsName) {
		jsidplay2.insertDisk(diskContents, diskContentsName);
	}

	@JSExport
	public static void ejectDisk() {
		jsidplay2.ejectDisk();
	}

	@JSExport
	public static void insertTape(byte[] tapeContents, String tapeContentsName) {
		jsidplay2.insertTape(tapeContents, tapeContentsName);
	}

	@JSExport
	public static void ejectTape() {
		jsidplay2.ejectTape();
	}

	@JSExport
	public static void pressPlayOnTape() {
		jsidplay2.pressPlayOnTape();
	}

	@JSExport
	public static void insertREUfile(byte[] cartContents, String cartContentsName) {
		jsidplay2.insertREUfile(cartContents, cartContentsName);
	}

	@JSExport
	public static void insertREU(int sizeKb) {
		jsidplay2.insertREU(sizeKb);
	}

	@JSExport
	public static void typeKey(String keyCode) {
		jsidplay2.typeKey(KeyTableEntry.valueOf(keyCode));
	}

	@JSExport
	public static void pressKey(String keyCode) {
		jsidplay2.pressKey(KeyTableEntry.valueOf(keyCode));
	}

	@JSExport
	public static void releaseKey(String keyCode) {
		jsidplay2.releaseKey(KeyTableEntry.valueOf(keyCode));
	}

	@JSExport
	public static void joystick(int number, int value) {
		jsidplay2.joystick(number, value);
	}

	@JSExport
	public static void volumeLevels(float mainVolume, float secondVolume, float thirdVolume, float mainBalance,
			float secondBalance, float thirdBalance, int mainDelay, int secondDelay, int thirdDelay) {
		jsidplay2.volumeLevels(mainVolume, secondVolume, thirdVolume, mainBalance, secondBalance, thirdBalance,
				mainDelay, secondDelay, thirdDelay);
	}

	@JSExport
	public static void stereo(String stereoMode, int dualSidBase, int thirdSIDBase, boolean fakeStereo,
			String sidToRead) {
		jsidplay2.stereo(StereoMode.valueOf(stereoMode), dualSidBase, thirdSIDBase, fakeStereo,
				SidReads.valueOf(sidToRead));
	}

	@JSExport
	public static void defaultEmulation(String emulation) {
		jsidplay2.defaultEmulation(Emulation.valueOf(emulation));
	}

	@JSExport
	public static void defaultChipModel(String chipModel) {
		jsidplay2.defaultChipModel(ChipModel.valueOf(chipModel));
	}

	@JSExport
	public static void filterName(String emulation, String chipModel, int sidNum, String filterName) {
		jsidplay2.filterName(Emulation.valueOf(emulation), ChipModel.valueOf(chipModel), sidNum, filterName);
	}

	@JSExport
	public static void mute(int sidNum, int voice, boolean value) {
		jsidplay2.mute(sidNum, voice, value);
	}

	@JSExport
	public static void fastForward() {
		jsidplay2.fastForward();
	}

	@JSExport
	public static void normalSpeed() {
		jsidplay2.normalSpeed();
	}

	@JSExport
	public static void freezeCartridge() {
		jsidplay2.freezeCartridge();
	}

	@JSExport
	public static void delaySidBlaster(int cycles) {
		jsidplay2.delaySidBlaster(cycles);
	}

}