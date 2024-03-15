package server.restful.wasm;

import org.teavm.interop.Export;
import org.teavm.interop.Import;

import libsidplay.common.CPUClock;

/**
 * Just a sandbox class to add Java functions to the c64jukebox.
 */
public class ComplexLogic {

	/* This method is invoked by the browser. */
	@Export(name = "toJava")
	public static void toJava(int cycles) {
		long delay = (long) (cycles / CPUClock.PAL.getCpuFrequency() * 1000000000L);
		long startTime = System.nanoTime();
		while (System.nanoTime() - startTime < delay)
			;
//		fromJava(magicNumber);
	}

	/* This method maps to a JavaScript method in a web page. */
	@Import(module = "env", name = "fromJava")
	private static native void fromJava(int message);

	public static void main(String[] args) {
	}
}
