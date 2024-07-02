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
	public static void js2open(byte[] sidContents, String sidContentsName, int song, int nthFrame, boolean addSidListener,
			byte[] cartContents, String cartContentsName, String command)
			throws IOException, SidTuneError, LineUnavailableException, InterruptedException {
		jsidplay2.open(sidContents, sidContentsName, song, nthFrame, addSidListener, cartContents, cartContentsName,
				command);
	}

	@JSExport
	public static void js2typeInCommand(final String multiLineCommand) {
		jsidplay2.typeInCommand(multiLineCommand);
	}

	@JSExport
	public static void js2clock() throws InterruptedException {
		jsidplay2.clock();
	}

	@JSExport
	public static void js2setDefaultPlayLength(double timeInS) {
		jsidplay2.setDefaultPlayLength(timeInS);
	}
	
	@JSExport
	public static void js2insertDisk(byte[] diskContents, String diskContentsName) {
		jsidplay2.insertDisk(diskContents, diskContentsName);
	}

	@JSExport
	public static void js2ejectDisk() {
		jsidplay2.ejectDisk();
	}

	@JSExport
	public static void js2insertTape(byte[] tapeContents, String tapeContentsName) {
		jsidplay2.insertTape(tapeContents, tapeContentsName);
	}

	@JSExport
	public static void js2ejectTape() {
		jsidplay2.ejectTape();
	}

	@JSExport
	public static void js2pressPlayOnTape() {
		jsidplay2.pressPlayOnTape();
	}

	@JSExport
	public static void js2insertREUfile(byte[] cartContents, String cartContentsName) {
		jsidplay2.insertREUfile(cartContents, cartContentsName);
	}

	@JSExport
	public static void js2insertREU(int sizeKb) {
		jsidplay2.insertREU(sizeKb);
	}

	@JSExport
	public static void js2typeKey(String keyCode) {
		jsidplay2.typeKey(KeyTableEntry.valueOf(keyCode));
	}

	@JSExport
	public static void js2pressKey(String keyCode) {
		jsidplay2.pressKey(KeyTableEntry.valueOf(keyCode));
	}

	@JSExport
	public static void js2releaseKey(String keyCode) {
		jsidplay2.releaseKey(KeyTableEntry.valueOf(keyCode));
	}

	@JSExport
	public static void js2joystick(int number, int value) {
		jsidplay2.joystick(number, value);
	}

	@JSExport
	public static void js2volumeLevels(float mainVolume, float secondVolume, float thirdVolume, float mainBalance,
			float secondBalance, float thirdBalance, int mainDelay, int secondDelay, int thirdDelay) {
		jsidplay2.volumeLevels(mainVolume, secondVolume, thirdVolume, mainBalance, secondBalance, thirdBalance,
				mainDelay, secondDelay, thirdDelay);
	}

	@JSExport
	public static void js2stereo(String stereoMode, int dualSidBase, int thirdSIDBase, boolean fakeStereo,
			String sidToRead) {
		jsidplay2.stereo(StereoMode.valueOf(stereoMode), dualSidBase, thirdSIDBase, fakeStereo,
				SidReads.valueOf(sidToRead));
	}

	@JSExport
	public static void js2defaultEmulation(String emulation) {
		jsidplay2.defaultEmulation(Emulation.valueOf(emulation));
	}

	@JSExport
	public static void js2defaultChipModel(String chipModel) {
		jsidplay2.defaultChipModel(ChipModel.valueOf(chipModel));
	}

	@JSExport
	public static void js2filterName(String emulation, String chipModel, int sidNum, String filterName) {
		jsidplay2.filterName(Emulation.valueOf(emulation), ChipModel.valueOf(chipModel), sidNum, filterName);
	}

	@JSExport
	public static void js2mute(int sidNum, int voice, boolean value) {
		jsidplay2.mute(sidNum, voice, value);
	}

	@JSExport
	public static void js2fastForward() {
		jsidplay2.fastForward();
	}

	@JSExport
	public static void js2normalSpeed() {
		jsidplay2.normalSpeed();
	}

	@JSExport
	public static void js2freezeCartridge() {
		jsidplay2.freezeCartridge();
	}

	@JSExport
	public static void js2delaySidBlaster(int cycles) {
		jsidplay2.delaySidBlaster(cycles);
	}

}