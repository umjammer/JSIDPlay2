package client.teavm.js;

import org.teavm.jso.JSBody;

import client.teavm.common.ExportedApi;
import client.teavm.common.IExportedApi;
import client.teavm.js.audio.JavaScriptAudioDriver;
import client.teavm.js.config.JavaScriptConfigResolver;

/**
 * TeaVM version of JSIDPlay2 to generate JavaScript code.
 */
public class JSIDPlay2TeaVM {

	public static void main(String[] args) {
		exportAPI(new ExportedApi(new JavaScriptConfigResolver(args), new JavaScriptAudioDriver()));
	}

	//
	// Exports to JavaScript
	//

	@JSBody(params = "jsidplay2", script = "main.api = jsidplay2;")
	private static native void exportAPI(IExportedApi jsidplay2);

}