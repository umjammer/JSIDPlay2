package sidplay.player;

import java.io.IOException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class DebugUtil {

	private static final String LOG_CONFIG_RESOURCE = "/sidplay/logconfig.properties";

	public static void init() {
		try {
			// turn off HSQL logging re-configuration
			System.setProperty("hsqldb.reconfig_logging", "false");
			// configure JSIDPlay2 logging (java util logging)
			LogManager.getLogManager().readConfiguration(DebugUtil.class.getResourceAsStream(LOG_CONFIG_RESOURCE));
		} catch (final IOException | NullPointerException e) {
			Logger.getAnonymousLogger().severe("Could not load " + LOG_CONFIG_RESOURCE + ": " + e.getMessage());
		}
	}
}
