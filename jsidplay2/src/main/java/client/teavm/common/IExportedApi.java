package client.teavm.common;

import org.teavm.jso.JSObject;

/**
 * Exports to JavaScript.
 */
public interface IExportedApi extends JSObject {

	void open(byte[] sidContents, String sidContentsName, int song, int nthFrame, boolean addSidListener,
			byte[] cartContents, String cartContentsName);

	void clock() throws InterruptedException;

	void insertDisk(byte[] diskContents, String diskContentsName);

	void ejectDisk();

	void insertTape(byte[] tapeContents, String tapeContentsName);

	void ejectTape();

	void pressPlayOnTape();

	void typeInCommand(String command);
	
	void typeKey(String keyCode);

	void delaySidBlaster(int cycles);
}