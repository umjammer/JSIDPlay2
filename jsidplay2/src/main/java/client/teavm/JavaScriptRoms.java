package client.teavm;

import static org.teavm.metaprogramming.Metaprogramming.emit;
import static org.teavm.metaprogramming.Metaprogramming.exit;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import org.teavm.metaprogramming.CompileTime;
import org.teavm.metaprogramming.Meta;
import org.teavm.metaprogramming.Value;

import sidplay.AllRoms;

@CompileTime
public class JavaScriptRoms {
	public static final String CHAR_ROM = "CHAR_ROM";
	public static final String BASIC_ROM = "BASIC_ROM";
	public static final String KERNAL_ROM = "KERNAL_ROM";
	public static final String PSID_DRIVER_ROM = "PSID_DRIVER_ROM";

	private static final String PSID_DRIVER_BIN = "/libsidplay/sidtune/psiddriver.bin";

	@Meta
	public static native Map<String, String> getJavaScriptRoms(boolean b);

	private static void getJavaScriptRoms(Value<Boolean> b) {
		try (DataInputStream is = new DataInputStream(JavaScriptRoms.class.getResourceAsStream(PSID_DRIVER_BIN))) {
			URL url = JavaScriptRoms.class.getResource(PSID_DRIVER_BIN);
			byte[] psidDriverBin = new byte[url.openConnection().getContentLength()];
			is.readFully(psidDriverBin);

			String charRom = new String(Base64.getEncoder().encode(AllRoms.CHAR));
			String basicRom = new String(Base64.getEncoder().encode(AllRoms.BASIC));
			String kernalRom = new String(Base64.getEncoder().encode(AllRoms.KERNAL));
			String psidDriver = new String(Base64.getEncoder().encode(psidDriverBin));

			Value<Map<String, String>> result = emit(() -> new HashMap<>());

			Value<String> basicRomValue = emit(() -> basicRom);
			Value<String> kernalRomValue = emit(() -> kernalRom);
			Value<String> charRomValue = emit(() -> charRom);
			Value<String> psidDriverValue = emit(() -> psidDriver);

			emit(() -> result.get().put(CHAR_ROM, charRomValue.get()));
			emit(() -> result.get().put(BASIC_ROM, basicRomValue.get()));
			emit(() -> result.get().put(KERNAL_ROM, kernalRomValue.get()));
			emit(() -> result.get().put(PSID_DRIVER_ROM, psidDriverValue.get()));

			exit(() -> result.get());

		} catch (IOException e) {
			throw new RuntimeException("Load failed for resource: " + PSID_DRIVER_BIN);
		}
	}
}
