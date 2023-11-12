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
	 * Video streaming time gap in ms between emulation time and current client
	 * viewer time in ms (default: 10s). SleepDriver will sleep, if time gap exceeds
	 * that value
	 * 
	 * @jsidplay2.systemProperty jsidplay2.sleep_driver.max_time_gap
	 */
	int MAX_TIME_GAP = Integer.valueOf(System.getProperty("jsidplay2.sleep_driver.max_time_gap", "10000"));

	/**
	 * Video streaming time to slow down video production in ms (default: 250ms).
	 * Sleep time of the SleepDriver, if current client viewer is far from video
	 * (max time gap reached)
	 * 
	 * @jsidplay2.systemProperty jsidplay2.sleep_driver.sleep_time
	 */
	long SLEEP_DRIVER_SLEEP_TIME = Long.valueOf(System.getProperty("jsidplay2.sleep_driver.sleep_time", "250"));
}
