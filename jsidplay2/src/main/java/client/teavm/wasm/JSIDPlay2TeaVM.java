package client.teavm.wasm;

import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;

import org.teavm.interop.Export;

import client.teavm.common.ExportedApi;
import client.teavm.common.IExportedApi;
import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.SidReads;
import libsidplay.common.StereoMode;
import libsidplay.components.keyboard.KeyTableEntry;
import libsidplay.sidtune.SidTuneError;

/**
 * Main class of the TeaVM version of JSIDPlay2 to generate web assembly code.
 */
public class JSIDPlay2TeaVM {

	private static IExportedApi jsidplay2;

	public static void main(String[] args) {
		jsidplay2 = new ExportedApi(new ImportedApi(args));
	}

	//
	// Exports to JavaScript
	//

	@Export(name = "open")
	public static void open(byte[] sidContents, String sidContentsNameFromJS, int song, int nthFrame,
		boolean addSidListener, byte[] cartContents, String cartContentsNameFromJS, String commandFromJS)
		throws IOException, SidTuneError, LineUnavailableException, InterruptedException {
		// JavaScript string cannot be used directly for some reason, therefore:
		String sidContentsName = sidContentsNameFromJS != null ? "" + sidContentsNameFromJS : null;
		String cartContentsName = cartContentsNameFromJS != null ? "" + cartContentsNameFromJS : null;
		String command = commandFromJS != null ? "" + commandFromJS : null;

		jsidplay2.open(sidContents, sidContentsName, song, nthFrame, addSidListener, cartContents, cartContentsName,
				command);
	}

	@Export(name = "typeInCommand")
	public static void typeInCommand(final String multiLineCommandFromJS) {
		// JavaScript string cannot be used directly for some reason, therefore:
		String multiLineCommand = multiLineCommandFromJS != null ? "" + multiLineCommandFromJS : null;

		jsidplay2.typeInCommand(multiLineCommand);
	}

	@Export(name = "clock")
	public static void clock() throws InterruptedException {
		jsidplay2.clock();
	}

	@Export(name = "insertDisk")
	public static void insertDisk(byte[] diskContents, String diskContentsNameFromJS) {
		// JavaScript string cannot be used directly for some reason, therefore:
		String diskContentsName = diskContentsNameFromJS != null ? "" + diskContentsNameFromJS : null;

		jsidplay2.insertDisk(diskContents, diskContentsName);
	}

	@Export(name = "ejectDisk")
	public static void ejectDisk() {
		jsidplay2.ejectDisk();
	}

	@Export(name = "insertTape")
	public static void insertTape(byte[] tapeContents, String tapeContentsNameFromJS) {
		// JavaScript string cannot be used directly for some reason, therefore:
		String tapeContentsName = tapeContentsNameFromJS != null ? "" + tapeContentsNameFromJS : null;

		jsidplay2.insertTape(tapeContents, tapeContentsName);
	}

	@Export(name = "ejectTape")
	public static void ejectTape() {
		jsidplay2.ejectTape();
	}

	@Export(name = "pressPlayOnTape")
	public static void pressPlayOnTape() {
		jsidplay2.pressPlayOnTape();
	}

	@Export(name = "typeKey")
	public static void typeKey(String keyCodeFromJS) {
		// JavaScript string cannot be used directly for some reason, therefore:
		String keyCode = keyCodeFromJS != null ? "" + keyCodeFromJS : null;

		jsidplay2.typeKey(KeyTableEntry.valueOf(keyCode));
	}

	@Export(name = "pressKey")
	public static void pressKey(String keyCodeFromJS) {
		// JavaScript string cannot be used directly for some reason, therefore:
		String keyCode = keyCodeFromJS != null ? "" + keyCodeFromJS : null;

		jsidplay2.pressKey(KeyTableEntry.valueOf(keyCode));
	}

	@Export(name = "releaseKey")
	public static void releaseKey(String keyCodeFromJS) {
		// JavaScript string cannot be used directly for some reason, therefore:
		String keyCode = keyCodeFromJS != null ? "" + keyCodeFromJS : null;

		jsidplay2.releaseKey(KeyTableEntry.valueOf(keyCode));
	}

	@Export(name = "joystick")
	public static void joystick(int number, int value) {
		jsidplay2.joystick(number, value);
	}

	@Export(name = "volumeLevels")
	public static void volumeLevels(float mainVolume, float secondVolume, float thirdVolume, float mainBalance,
		float secondBalance, float thirdBalance, int mainDelay, int secondDelay, int thirdDelay) {
		jsidplay2.volumeLevels(mainVolume, secondVolume, thirdVolume, mainBalance, secondBalance, thirdBalance,
				mainDelay, secondDelay, thirdDelay);
	}

	@Export(name = "stereo")
	public static void stereo(String stereoModeFromJS, int dualSidBase, int thirdSIDBase, boolean fakeStereo,
		String sidToReadFromJS) {
		// JavaScript string cannot be used directly for some reason, therefore:
		String stereoMode = stereoModeFromJS != null ? "" + stereoModeFromJS : null;
		String sidToRead = sidToReadFromJS != null ? "" + sidToReadFromJS : null;

		jsidplay2.stereo(StereoMode.valueOf(stereoMode), dualSidBase, thirdSIDBase, fakeStereo,
				SidReads.valueOf(sidToRead));
	}

	@Export(name = "defaultEmulation")
	public static void defaultEmulation(String emulationFromJS) {
		// JavaScript string cannot be used directly for some reason, therefore:
		String emulation = emulationFromJS != null ? "" + emulationFromJS : null;

		jsidplay2.defaultEmulation(Emulation.valueOf(emulation));
	}

	@Export(name = "defaultChipModel")
	public static void defaultChipModel(String chipModelFromJS) {
		// JavaScript string cannot be used directly for some reason, therefore:
		String chipModel = chipModelFromJS != null ? "" + chipModelFromJS : null;

		jsidplay2.defaultChipModel(ChipModel.valueOf(chipModel));
	}

	@Export(name = "filterName")
	public static void filterName(String emulationFromJS, String chipModelFromJS, int sidNum, String filterNameFromJS) {
		// JavaScript string cannot be used directly for some reason, therefore:
		String emulation = emulationFromJS != null ? "" + emulationFromJS : null;
		String chipModel = chipModelFromJS != null ? "" + chipModelFromJS : null;
		String filterName = filterNameFromJS != null ? "" + filterNameFromJS : null;

		jsidplay2.filterName(Emulation.valueOf(emulation), ChipModel.valueOf(chipModel), sidNum, filterName);
	}

	@Export(name = "mute")
	public static void mute(int sidNum, int voice, boolean value) {
		jsidplay2.mute(sidNum, voice, value);
	}

	@Export(name = "delaySidBlaster")
	public static void delaySidBlaster(int cycles) {
		jsidplay2.delaySidBlaster(cycles);
	}

}