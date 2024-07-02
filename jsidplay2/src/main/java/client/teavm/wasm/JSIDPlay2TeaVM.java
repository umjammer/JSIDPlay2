package client.teavm.wasm;

import static java.util.Optional.ofNullable;

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

	@Export(name = "js2open")
	public static void js2open(byte[] sidContents, String sidContentsNameFromJS, int song, int nthFrame,
			boolean addSidListener, byte[] cartContents, String cartContentsNameFromJS, String commandFromJS)
			throws IOException, SidTuneError, LineUnavailableException, InterruptedException {
		// JavaScript string cannot be used directly for some reason, therefore:
		String sidContentsName = ofNullable(sidContentsNameFromJS).map(String::valueOf).orElse(null);
		String cartContentsName = ofNullable(cartContentsNameFromJS).map(String::valueOf).orElse(null);
		String command = ofNullable(commandFromJS).map(String::valueOf).orElse(null);

		jsidplay2.open(sidContents, sidContentsName, song, nthFrame, addSidListener, cartContents, cartContentsName,
				command);
	}

	@Export(name = "js2typeInCommand")
	public static void js2typeInCommand(final String multiLineCommandFromJS) {
		// JavaScript string cannot be used directly for some reason, therefore:
		String multiLineCommand = ofNullable(multiLineCommandFromJS).map(String::valueOf).orElse(null);

		jsidplay2.typeInCommand(multiLineCommand);
	}

	@Export(name = "js2clock")
	public static void js2clock() throws InterruptedException {
		jsidplay2.clock();
	}

	@Export(name = "js2setDefaultPlayLength")
	public static void js2setDefaultPlayLength(double timeInS) {
		jsidplay2.setDefaultPlayLength(timeInS);
	}

	@Export(name = "js2insertDisk")
	public static void js2insertDisk(byte[] diskContents, String diskContentsNameFromJS) {
		// JavaScript string cannot be used directly for some reason, therefore:
		String diskContentsName = ofNullable(diskContentsNameFromJS).map(String::valueOf).orElse(null);

		jsidplay2.insertDisk(diskContents, diskContentsName);
	}

	@Export(name = "js2ejectDisk")
	public static void js2ejectDisk() {
		jsidplay2.ejectDisk();
	}

	@Export(name = "js2insertTape")
	public static void js2insertTape(byte[] tapeContents, String tapeContentsNameFromJS) {
		// JavaScript string cannot be used directly for some reason, therefore:
		String tapeContentsName = ofNullable(tapeContentsNameFromJS).map(String::valueOf).orElse(null);

		jsidplay2.insertTape(tapeContents, tapeContentsName);
	}

	@Export(name = "js2ejectTape")
	public static void js2ejectTape() {
		jsidplay2.ejectTape();
	}

	@Export(name = "js2pressPlayOnTape")
	public static void js2pressPlayOnTape() {
		jsidplay2.pressPlayOnTape();
	}

	@Export(name = "js2insertREUfile")
	public static void js2insertREUfile(byte[] cartContents, String cartContentsNameFromJS) {
		// JavaScript string cannot be used directly for some reason, therefore:
		String cartContentsName = ofNullable(cartContentsNameFromJS).map(String::valueOf).orElse(null);

		jsidplay2.insertREUfile(cartContents, cartContentsName);
	}

	@Export(name = "js2insertREU")
	public static void js2insertREU(int sizeKb) {
		jsidplay2.insertREU(sizeKb);
	}

	@Export(name = "js2typeKey")
	public static void js2typeKey(String keyCodeFromJS) {
		// JavaScript string cannot be used directly for some reason, therefore:
		String keyCode = ofNullable(keyCodeFromJS).map(String::valueOf).orElse(null);

		jsidplay2.typeKey(KeyTableEntry.valueOf(keyCode));
	}

	@Export(name = "js2pressKey")
	public static void js2pressKey(String keyCodeFromJS) {
		// JavaScript string cannot be used directly for some reason, therefore:
		String keyCode = ofNullable(keyCodeFromJS).map(String::valueOf).orElse(null);

		jsidplay2.pressKey(KeyTableEntry.valueOf(keyCode));
	}

	@Export(name = "js2releaseKey")
	public static void js2releaseKey(String keyCodeFromJS) {
		// JavaScript string cannot be used directly for some reason, therefore:
		String keyCode = ofNullable(keyCodeFromJS).map(String::valueOf).orElse(null);

		jsidplay2.releaseKey(KeyTableEntry.valueOf(keyCode));
	}

	@Export(name = "js2joystick")
	public static void js2joystick(int number, int value) {
		jsidplay2.joystick(number, value);
	}

	@Export(name = "js2volumeLevels")
	public static void js2volumeLevels(float mainVolume, float secondVolume, float thirdVolume, float mainBalance,
			float secondBalance, float thirdBalance, int mainDelay, int secondDelay, int thirdDelay) {
		jsidplay2.volumeLevels(mainVolume, secondVolume, thirdVolume, mainBalance, secondBalance, thirdBalance,
				mainDelay, secondDelay, thirdDelay);
	}

	@Export(name = "js2stereo")
	public static void js2stereo(String stereoModeFromJS, int dualSidBase, int thirdSIDBase, boolean fakeStereo,
			String sidToReadFromJS) {
		// JavaScript string cannot be used directly for some reason, therefore:
		String stereoMode = ofNullable(stereoModeFromJS).map(String::valueOf).orElse(null);
		String sidToRead = ofNullable(sidToReadFromJS).map(String::valueOf).orElse(null);

		jsidplay2.stereo(StereoMode.valueOf(stereoMode), dualSidBase, thirdSIDBase, fakeStereo,
				SidReads.valueOf(sidToRead));
	}

	@Export(name = "js2defaultEmulation")
	public static void js2defaultEmulation(String emulationFromJS) {
		// JavaScript string cannot be used directly for some reason, therefore:
		String emulation = ofNullable(emulationFromJS).map(String::valueOf).orElse(null);

		jsidplay2.defaultEmulation(Emulation.valueOf(emulation));
	}

	@Export(name = "js2defaultChipModel")
	public static void js2defaultChipModel(String chipModelFromJS) {
		// JavaScript string cannot be used directly for some reason, therefore:
		String chipModel = ofNullable(chipModelFromJS).map(String::valueOf).orElse(null);

		jsidplay2.defaultChipModel(ChipModel.valueOf(chipModel));
	}

	@Export(name = "js2filterName")
	public static void js2filterName(String emulationFromJS, String chipModelFromJS, int sidNum, String filterNameFromJS) {
		// JavaScript string cannot be used directly for some reason, therefore:
		String emulation = ofNullable(emulationFromJS).map(String::valueOf).orElse(null);
		String chipModel = ofNullable(chipModelFromJS).map(String::valueOf).orElse(null);
		String filterName = ofNullable(filterNameFromJS).map(String::valueOf).orElse(null);

		jsidplay2.filterName(Emulation.valueOf(emulation), ChipModel.valueOf(chipModel), sidNum, filterName);
	}

	@Export(name = "js2mute")
	public static void js2mute(int sidNum, int voice, boolean value) {
		jsidplay2.mute(sidNum, voice, value);
	}

	@Export(name = "js2fastForward")
	public static void js2fastForward() {
		jsidplay2.fastForward();
	}

	@Export(name = "js2normalSpeed")
	public static void js2normalSpeed() {
		jsidplay2.normalSpeed();
	}

	@Export(name = "js2freezeCartridge")
	public static void js2freezeCartridge() {
		jsidplay2.freezeCartridge();
	}

	@Export(name = "js2delaySidBlaster")
	public static void js2delaySidBlaster(int cycles) {
		jsidplay2.delaySidBlaster(cycles);
	}

}