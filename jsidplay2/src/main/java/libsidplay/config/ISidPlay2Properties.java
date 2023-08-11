package libsidplay.config;

/**
 * Some system properties to fine tune JSIDPlay2 without touching the
 * configuration.
 * 
 * @author ken
 *
 */
public interface ISidPlay2Properties {

	/**
	 * Prevent unlimited songlength in record mode. Maximum recording time in s.
	 */
	int MAX_SONG_LENGTH = Integer.valueOf(System.getProperty("jsidplay2.timer.max_song_length", "180"));

}
