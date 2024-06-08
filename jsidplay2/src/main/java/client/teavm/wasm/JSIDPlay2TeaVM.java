package client.teavm.wasm;

import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;

import org.teavm.interop.Export;

import client.teavm.common.ExportedApi;
import client.teavm.common.IExportedApi;
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
	public static void open(byte[] sidContents, String sidContentsName, int song, int nthFrame, boolean addSidListener,
			byte[] cartContents, String cartContentsName, String command)
			throws IOException, SidTuneError, LineUnavailableException, InterruptedException {
		jsidplay2.open(sidContents, sidContentsName, song, nthFrame, addSidListener, cartContents, cartContentsName,
				command);
	}

	@Export(name = "typeInCommand")
	public static void typeInCommand(final String nameFromJS) {
		jsidplay2.typeInCommand(nameFromJS);
	}

	@Export(name = "clock")
	public static void clock() throws InterruptedException {
		jsidplay2.clock();
	}

	@Export(name = "insertDisk")
	public static void insertDisk(byte[] diskContents, String diskContentsName) {
		jsidplay2.insertDisk(diskContents, diskContentsName);
	}

	@Export(name = "ejectDisk")
	public static void ejectDisk() {
		jsidplay2.ejectDisk();
	}

	@Export(name = "insertTape")
	public static void insertTape(byte[] tapeContents, String tapeContentsName) {
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
	public static void typeKey(String keyCode) {
		jsidplay2.typeKey(keyCode);
	}

	@Export(name = "pressKey")
	public static void pressKey(String keyCode) {
		jsidplay2.pressKey(keyCode);
	}

	@Export(name = "releaseKey")
	public static void releaseKey(String keyCode) {
		jsidplay2.releaseKey(keyCode);
	}

	@Export(name = "joystick")
	public static void joystick(int number, int value) {
		jsidplay2.joystick(number, value);
	}

	@Export(name = "defaultEmulation")
	public static void defaultEmulation(String emulation) {
		jsidplay2.defaultEmulation(emulation);
	}

	@Export(name = "defaultChipModel")
	public static void defaultChipModel(String chipModel) {
		jsidplay2.defaultChipModel(chipModel);
	}

	@Export(name = "filterName")
	public static void filterName(String emulation, String chipModel, int sidNum, String filterName) {
		jsidplay2.filterName(emulation, chipModel, sidNum, filterName);
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
