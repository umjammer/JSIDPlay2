package client.teavm.js;

import org.teavm.jso.JSBody;

import client.teavm.common.ExportedApi;
import client.teavm.common.IExportedApi;

/**
 * Main class of the TeaVM version of JSIDPlay2 to generate JavaScript code.
 */
public class JSIDPlay2TeaVM {

	public static void main(String[] args) {
		exportAPI(new ExportedApi(new ImportedApi(args)));
	}

	//
	// Exports to JavaScript
	//

	@JSBody(params = "jsidplay2", script = "main.api = jsidplay2;")
	private static native void exportAPI(IExportedApi jsidplay2);

}