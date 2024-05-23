package client.teavm.common;

import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;

import org.teavm.jso.JSObject;

import libsidplay.sidtune.SidTuneError;

public interface IExportedApi extends JSObject {

	void open(byte[] sidContents, String sidContentsName, int song, int nthFrame, boolean addSidListener,
			byte[] cartContents, String cartContentsName)
			throws IOException, SidTuneError, LineUnavailableException, InterruptedException;

	void typeInCommand(String nameFromJS);

	void clock() throws InterruptedException;

	void insertDisk(byte[] diskContents, String diskContentsName);

	void ejectDisk();

	void insertTape(byte[] tapeContents, String tapeContentsName);

	void ejectTape();

	void pressPlayOnTape();

	void typeKey(String keyCode);

	void delaySidBlaster(int cycles);
}