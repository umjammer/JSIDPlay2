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
package server.netsiddev.ini;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import libsidplay.config.IFilterSection;
import sidplay.ini.IniFilterSection;
import sidplay.ini.IniReader;

/**
 * INI configuration file support responsible to load and save all emulator
 * settings.
 *
 * @author Ken Händel
 *
 */
public class JSIDDeviceConfig {
	/**
	 * Bump this each time you want to invalidate jsiddevice.ini files on disk
	 */
	protected static final int REQUIRED_CONFIG_VERSION = 1;

	/** Name of our config file. */
	private static final String FILE_NAME = "jsiddevice.ini";

	/** INI configuration filename or null (use internal configuration) */
	private final File iniPath;

	/** Device name prefix of JSIDDEVICE 1.0 */
	private static final String JSIDDEVICE1_NAME_PREFIX = "JSidDevice";

	private IniJSIDDeviceSection jsiddeviceSection;

	private IniJSIDDeviceAudioSection audioSection;

	private IniJSIDDeviceWhatsSidSection whatsSidSection;

	protected IniReader iniReader;

	private String[] filterList;

	/** ReSid device names are sorted by filter strength and SID model */
	public class compareReSidFilterNames implements Comparator<String> {
		private static final String LIGHT8580 = "Light8580";
		private static final String LIGHT = "Light";
		private static final String DARK = "Dark";

		@Override
		public int compare(String o1, String o2) {
			if (o1.startsWith(LIGHT) && !o2.startsWith(LIGHT)) {
				return -1;
			} else if (o2.startsWith(LIGHT) && !o1.startsWith(LIGHT)) {
				return 1;
			} else if (o2.startsWith(LIGHT) && o1.startsWith(LIGHT)) {
				if (o1.startsWith(LIGHT8580) && !o2.startsWith(LIGHT8580)) {
					return 1;
				} else if (o2.startsWith(LIGHT8580) && !o1.startsWith(LIGHT8580)) {
					return -1;
				}
				return -o1.compareTo(o2);
			}

			if (o1.startsWith(DARK) && !o2.startsWith(DARK)) {
				return 1;
			} else if (o2.startsWith(DARK) && !o1.startsWith(DARK)) {
				return -1;
			}
			return o1.compareTo(o2);
		}
	}

	private void clear() {
		jsiddeviceSection = new IniJSIDDeviceSection(iniReader);
		audioSection = new IniJSIDDeviceAudioSection(iniReader);
		whatsSidSection = new IniJSIDDeviceWhatsSidSection(iniReader);

		final List<String> filters = new ArrayList<>();
		final List<String> filtersResidfp = new ArrayList<>();
		final List<String> filtersResid = new ArrayList<>();

		for (final String heading : iniReader.listSections()) {
			if (!heading.matches("Filter.*")) {
				continue;
			}

			String filterName = heading.substring(6);

			if (filterName.startsWith(JSIDDEVICE1_NAME_PREFIX)) {
				filters.add(filterName);
			} else {
				IFilterSection iniFilter = getFilter(filterName);
				if (iniFilter.isReSIDfpFilter6581() || iniFilter.isReSIDfpFilter8580()) {
					filtersResidfp.add(filterName);
				} else {
					filtersResid.add(filterName);
				}
			}
		}

		Collections.sort(filters);
		Collections.sort(filtersResidfp);
		Collections.sort(filtersResid, new compareReSidFilterNames());

		filters.addAll(filtersResid);
		filters.addAll(filtersResidfp);

		filterList = filters.toArray(new String[] {});
	}

	public IFilterSection getFilter(final String filterName) {
		return new IniFilterSection(iniReader, "Filter" + filterName);
	}

	public String[] getFilterList() {
		return filterList;
	}

	public JSIDDeviceConfig(boolean createIfNotExists) {
		iniPath = getINIPath();
		read();

		if (!iniPath.exists() && createIfNotExists) {
			write();
		}
	}

	private void read() {
		if (iniPath.exists()) {
			try (InputStream is = new FileInputStream(iniPath)) {
				iniReader = new IniReader(is);
				clear();
				/* validate loaded configuration */
				if (jsiddeviceSection.getVersion() == REQUIRED_CONFIG_VERSION) {
					return;
				}
				createINIBackup(iniPath);
			} catch (final Exception e) {
				System.err.println("INI reading failed: " + e.getMessage());
			}

			System.out.println(
					"INI file old/broken (version=" + jsiddeviceSection.getVersion() + "). Using default settings.");
		}
		readInternal();
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
	private File getINIPath() {
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
		iniFile.renameTo(new File(iniFile.getParent(), FILE_NAME + ".bak"));
	}

	private void readInternal() {
		try (InputStream is = getClass().getClassLoader().getResourceAsStream("server/netsiddev/ini/" + FILE_NAME)) {
			iniReader = new IniReader(is);
			clear();
			/*
			 * Set the current version so that we detect old versions in future.
			 */
			jsiddeviceSection.setVersion(REQUIRED_CONFIG_VERSION);
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void write() {
		try {
			if (!iniReader.isDirty()) {
				return;
			}
			iniReader.save(iniPath.getAbsolutePath());
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

	public IniJSIDDeviceSection jsiddevice() {
		return jsiddeviceSection;
	}

	public final IniJSIDDeviceAudioSection audio() {
		return audioSection;
	}

	public final IniJSIDDeviceWhatsSidSection whatsSidSection() {
		return whatsSidSection;
	}
}
