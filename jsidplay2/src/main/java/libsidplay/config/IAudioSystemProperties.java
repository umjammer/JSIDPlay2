package libsidplay.config;

/**
 * Some system properties to fine tune JSIDPlay2 without touching the
 * configuration.
 * 
 * @author ken
 *
 */
public interface IAudioSystemProperties {

	/**
	 * Video streaming: Time gap between emulation time and real time of the
	 * SleepDriver in ms.
	 */
	int MAX_TIME_GAP = Integer.valueOf(System.getProperty("jsidplay2.sleep_driver.max_time_gap", "10000"));

	/**
	 * Video streaming: To slow down video production, if client viewer is far
	 * behind. Sleep time of the SleepDriver in ms.
	 */
	long SLEEP_DRIVER_SLEEP_TIME = Long.valueOf(System.getProperty("jsidplay2.sleep_driver.sleep_time", "1000"));
}
