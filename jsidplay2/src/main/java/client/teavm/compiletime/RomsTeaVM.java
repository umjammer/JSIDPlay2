package client.teavm.compiletime;

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
public class RomsTeaVM {
	public static final String CHAR_ROM = "CHAR_ROM";
	public static final String BASIC_ROM = "BASIC_ROM";
	public static final String KERNAL_ROM = "KERNAL_ROM";
	public static final String C1541_ROM = "C1541_ROM";
	public static final String PSID_DRIVER_ROM = "PSID_DRIVER_ROM";
	public static final String JIFFYDOS_C64_ROM = "JIFFYDOS_C64_ROM";
	public static final String JIFFYDOS_C1541_ROM = "JIFFYDOS_C1541_ROM";

	private static final String PSID_DRIVER_BIN = "/libsidplay/sidtune/psiddriver.bin";

	@Meta
	public static native Map<String, String> getJavaScriptRoms(boolean b);

	private static void getJavaScriptRoms(Value<Boolean> b) {
		try (DataInputStream is = new DataInputStream(RomsTeaVM.class.getResourceAsStream(PSID_DRIVER_BIN))) {
			URL url = RomsTeaVM.class.getResource(PSID_DRIVER_BIN);
			byte[] psidDriverBin = new byte[url.openConnection().getContentLength()];
			is.readFully(psidDriverBin);

			String charRom = new String(Base64.getEncoder().encode(AllRoms.CHAR));
			String basicRom = new String(Base64.getEncoder().encode(AllRoms.BASIC));
			String kernalRom = new String(Base64.getEncoder().encode(AllRoms.KERNAL));
			String c1541Rom = new String(Base64.getEncoder().encode(AllRoms.C1541));
			String psidDriver = new String(Base64.getEncoder().encode(psidDriverBin));
			String jiffyDosC64Rom = new String(Base64.getEncoder().encode(AllRoms.JIFFYDOS_C64));
			String jiffyDosC1541Rom = new String(Base64.getEncoder().encode(AllRoms.JIFFYDOS_C1541));

			Value<Map<String, String>> result = emit(() -> new HashMap<>());

			Value<String> basicRomValue = emit(() -> basicRom);
			Value<String> kernalRomValue = emit(() -> kernalRom);
			Value<String> charRomValue = emit(() -> charRom);
			Value<String> c1541RomValue = emit(() -> c1541Rom);
			Value<String> psidDriverValue = emit(() -> psidDriver);
			Value<String> jiffyDosC64RomValue = emit(() -> jiffyDosC64Rom);
			Value<String> jiffyDosC1541RomValue = emit(() -> jiffyDosC1541Rom);

			emit(() -> result.get().put(CHAR_ROM, charRomValue.get()));
			emit(() -> result.get().put(BASIC_ROM, basicRomValue.get()));
			emit(() -> result.get().put(KERNAL_ROM, kernalRomValue.get()));
			emit(() -> result.get().put(C1541_ROM, c1541RomValue.get()));
			emit(() -> result.get().put(PSID_DRIVER_ROM, psidDriverValue.get()));
			emit(() -> result.get().put(JIFFYDOS_C64_ROM, jiffyDosC64RomValue.get()));
			emit(() -> result.get().put(JIFFYDOS_C1541_ROM, jiffyDosC1541RomValue.get()));

			exit(() -> result.get());

		} catch (IOException e) {
			throw new RuntimeException("Load failed for resource: " + PSID_DRIVER_BIN);
		}
	}
}
