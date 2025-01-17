/**
 *                         Sidplay2 config file reader.
 *                         ----------------------------
 *  begin                : Sun Mar 25 2001
 *  copyright            : (C) 2000 by Simon White
 *  email                : s_a_white@email.com
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 * @author Ken Händel
 *
 */
package sidplay.ini;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.beust.jcommander.ParametersDelegate;

import libsidplay.config.IAudioSection;
import libsidplay.config.IC1541Section;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;
import libsidplay.config.IFilterSection;
import libsidplay.config.IPrinterSection;
import libsidplay.config.ISidPlay2Section;
import libsidplay.config.IWhatsSidSection;
import sidplay.ini.converter.BeanToStringConverter;

/**
 * 
 * Configuration of the console version of JSIDPlay2.
 * 
 * INI configuration file support responsible to load and save all emulator
 * settings.
 *
 * @author Ken Händel
 *
 */
public class IniConfig implements IConfig {
	/** Name of our config file. */
	private static final String FILE_NAME = "jsidplay2.ini";

	private static IniConfig singleInstance;

	/** INI configuration filename or null (use internal configuration) */
	private final File iniPath;

	@ParametersDelegate
	private ISidPlay2Section sidplay2Section;
	@ParametersDelegate
	private IC1541Section c1541Section;
	@ParametersDelegate
	private IPrinterSection printerSection;
	@ParametersDelegate
	private IniConsoleSection consoleSection;
	@ParametersDelegate
	private IAudioSection audioSection;
	@ParametersDelegate
	private IEmulationSection emulationSection;
	@ParametersDelegate
	private IWhatsSidSection whatsSidSection;

	protected IniReader iniReader;

	private void clear() {
		sidplay2Section = new IniSidplay2Section(iniReader);
		c1541Section = new IniC1541Section(iniReader);
		printerSection = new IniPrinterSection(iniReader);
		consoleSection = new IniConsoleSection(iniReader);
		audioSection = new IniAudioSection(iniReader);
		emulationSection = new IniEmulationSection(iniReader);
		whatsSidSection = new IniWhatsSidSection(iniReader);
	}

	@Override
	public final List<? extends IFilterSection> getFilterSection() {
		final List<IFilterSection> filters = new ArrayList<>();
		for (final String heading : iniReader.listSections()) {
			if (!heading.matches("Filter.*")) {
				continue;
			}

			filters.add(new IniFilterSection(iniReader, heading));
		}

		return filters;
	}

	/**
	 * Get default configuration, read from internal jsidplay2.ini file.<BR>
	 * This is a Single instance!
	 *
	 * @return default configuration
	 */
	public static final IniConfig getDefault() {
		if (singleInstance == null) {
			singleInstance = new IniConfig();
		}
		return singleInstance;
	}

	/**
	 * Read internal configuration file.
	 */
	public IniConfig() {
		this(false, null);
	}

	/**
	 * Read configuration file (external or internal, if it does not exist).<BR>
	 *
	 * @param createIfNotExists If external configuration file does not exist,
	 *                          create it
	 */
	public IniConfig(boolean createIfNotExists) {
		this(createIfNotExists, getINIPath());
	}

	private IniConfig(boolean createIfNotExists, File iniPath) {
		this.iniPath = iniPath;
		if (iniPath != null && iniPath.exists()) {
			try (InputStream is = new FileInputStream(iniPath)) {
				iniReader = new IniReader(is);
				clear();
				/* validate loaded configuration */
				if (sidplay2Section.getVersion() == REQUIRED_CONFIG_VERSION) {
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
			if (!iniPath.getParentFile().exists()) iniPath.getParentFile().mkdirs();
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
			File oldConfig = new File(parent, "sidplay2.ini");
			if (oldConfig.exists()) {
				System.err.println(String.format("Warning, %s is NOT used anymore, please use the new name %s!",
						oldConfig, configPlace));
			}
			if (configPlace.exists()) {
				return configPlace;
			}
		}
		return new File(System.getProperty("user.home") + "/.config/jsidplay2", FILE_NAME);
	}

	/**
	 * Create backup of old INI file
	 * 
	 * @param iniFile the INI file to backup
	 */
	private void createINIBackup(final File iniFile) {
		File bakFile = new File(iniFile.getParentFile(), FILE_NAME + ".bak");
		iniFile.renameTo(bakFile);

		int currentVersion = sidplay2Section.getVersion();
		System.out.println("Configuration file version=" + currentVersion + " is older than expected version="
				+ REQUIRED_CONFIG_VERSION + ", a backup has been created: " + bakFile);
	}

	private void readInternal() {
		try (InputStream is = getClass().getClassLoader().getResourceAsStream("sidplay/ini/" + FILE_NAME)) {
			iniReader = new IniReader(is);
			clear();
			/*
			 * Set the current version so that we detect old versions in future.
			 */
			sidplay2Section.setVersion(REQUIRED_CONFIG_VERSION);
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
	public final ISidPlay2Section getSidplay2Section() {
		return sidplay2Section;
	}

	@Override
	public final IC1541Section getC1541Section() {
		return c1541Section;
	}

	@Override
	public final IPrinterSection getPrinterSection() {
		return printerSection;
	}

	public final IniConsoleSection getConsoleSection() {
		return consoleSection;
	}

	@Override
	public final IAudioSection getAudioSection() {
		return audioSection;
	}

	@Override
	public final IEmulationSection getEmulationSection() {
		return emulationSection;
	}

	@Override
	public final IWhatsSidSection getWhatsSidSection() {
		return whatsSidSection;
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}
}
