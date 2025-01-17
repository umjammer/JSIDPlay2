/**
 * @author Ken Händel
 *
 */
package libsidutils.fingerprinting.ini;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import sidplay.ini.IniReader;
import sidplay.ini.converter.BeanToStringConverter;

/**
 * INI configuration file support responsible to load and save all emulator
 * settings.
 *
 * @author Ken Händel
 *
 */
public class IniFingerprintConfig implements IFingerprintConfig {
	/** Name of our config file. */
	private static final String FILE_NAME = "fingerprint.ini";

	private static IniFingerprintConfig singleInstance;

	/** INI configuration filename or null (use internal configuration) */
	private final File iniPath;

	private IFingerprintSection fingerPrintSection;

	protected IniReader iniReader;

	private void clear() {
		fingerPrintSection = new IniFingerprintSection(iniReader);
	}

	/**
	 * Get default configuration, read from internal fingerprint.ini file.<BR>
	 * This is a Single instance!
	 *
	 * @return default configuration
	 */
	public static IniFingerprintConfig getDefault() {
		if (singleInstance == null) {
			singleInstance = new IniFingerprintConfig();
		}
		return singleInstance;
	}

	/**
	 * Read internal configuration file.
	 */
	public IniFingerprintConfig() {
		this(false, null);
	}

	/**
	 * Read configuration file (external or internal, if it does not exist).<BR>
	 *
	 * @param createIfNotExists If external configuration file does not exist,
	 *                          create it
	 */
	public IniFingerprintConfig(boolean createIfNotExists) {
		this(createIfNotExists, getINIPath());
	}

	private IniFingerprintConfig(boolean createIfNotExists, File iniPath) {
		this.iniPath = iniPath;
		if (iniPath != null && iniPath.exists()) {
			try (InputStream is = new FileInputStream(iniPath)) {
				iniReader = new IniReader(is);
				clear();
				/* validate loaded configuration */
				if (fingerPrintSection.getVersion() == REQUIRED_CONFIG_VERSION) {
					if (createIfNotExists) {
						System.out.println("Use INI file: " + iniPath);
					}
					return;
				}
			} catch (final Exception e) {
				throw new RuntimeException(e);
			}
			createINIBackup(iniPath);
			System.out.println("Use internal INI file: " + FILE_NAME);
		}

		readInternal();
		if (iniPath != null && !iniPath.exists() && createIfNotExists) {
			write();
		}
	}

	/**
	 * Determine INI filename.
	 * <OL>
	 * <LI>If INI file exists in the user directory, then use it, else
	 * <LI>use INI file in the current working directory
	 * </OL>
	 *
	 * @return the absolute path name of the INI file to use
	 */
	public static File getINIPath() {
		for (final String parent : new String[] { System.getProperty("user.dir"), System.getProperty("user.home"), }) {
			File configPlace = new File(parent, FILE_NAME);
			if (configPlace.exists()) {
				return configPlace;
			}
		}
		return new File(System.getProperty("user.home"), FILE_NAME);
	}

	/**
	 * Create backup of old INI file
	 *
	 * @param iniFile the INI file to backup
	 */
	private void createINIBackup(final File iniFile) {
		File bakFile = new File(iniFile.getParentFile(), FILE_NAME + ".bak");
		iniFile.renameTo(bakFile);

		int currentVersion = fingerPrintSection.getVersion();
		System.out.println("Configuration file version=" + currentVersion + " is older than expected version="
				+ REQUIRED_CONFIG_VERSION + ", a backup has been created: " + bakFile);
	}

	private void readInternal() {
		try (InputStream is = getClass().getClassLoader()
				.getResourceAsStream("libsidutils/fingerprinting/ini/" + FILE_NAME)) {
			iniReader = new IniReader(is);
			clear();
			/*
			 * Set the current version so that we detect old versions in future.
			 */
			fingerPrintSection.setVersion(REQUIRED_CONFIG_VERSION);
		} catch (final IOException e) {
			e.printStackTrace();
			return;
		}
	}

	public final void write() {
		if (!iniReader.isDirty()) {
			return;
		}

		System.out.println("Save INI file: " + iniPath);
		try {
			iniReader.save(iniPath.getAbsolutePath());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public final IFingerprintSection getFingerPrintSection() {
		return fingerPrintSection;
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}

}
