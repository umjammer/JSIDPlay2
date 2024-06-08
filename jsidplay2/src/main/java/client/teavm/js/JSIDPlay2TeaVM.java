package client.teavm.js;

import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;

import org.teavm.jso.JSExport;

import client.teavm.common.ExportedApi;
import client.teavm.common.IExportedApi;
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
	public static void typeKey(String keyCode) {
		jsidplay2.typeKey(keyCode);
	}

	@JSExport
	public static void pressKey(String keyCode) {
		jsidplay2.pressKey(keyCode);
	}

	@JSExport
	public static void releaseKey(String keyCode) {
		jsidplay2.releaseKey(keyCode);
	}

	@JSExport
	public static void joystick(int number, int value) {
		jsidplay2.joystick(number, value);
	}

	@JSExport
	public static void defaultEmulation(String emulation) {
		jsidplay2.defaultEmulation(emulation);
	}

	@JSExport
	public static void defaultChipModel(String chipModel) {
		jsidplay2.defaultChipModel(chipModel);
	}

	@JSExport
	public static void filterName(String emulation, String chipModel, int sidNum, String filterName) {
		jsidplay2.filterName(emulation, chipModel, sidNum, filterName);
	}

	@JSExport
	public static void delaySidBlaster(int cycles) {
		jsidplay2.delaySidBlaster(cycles);
	}

}