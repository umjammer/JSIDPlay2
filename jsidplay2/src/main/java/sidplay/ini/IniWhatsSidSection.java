package sidplay.ini;

import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_CAPTURE_TIME;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_CONNECTION_TIMEOUT;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_DETECT_CHIP_MODEL;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_ENABLE;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_MATCH_RETRY_TIME;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_MATCH_START_TIME;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_MINIMUM_RELATIVE_CONFIDENCE;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_PASSWORD;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_URL;
import static sidplay.ini.IniDefaults.DEFAULT_WHATSSID_USERNAME;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import libsidplay.config.IWhatsSidSection;
import sidplay.ini.converter.BeanToStringConverter;

/**
 * WhatsSID section of the INI file.
 *
 * @author Ken Händel
 *
 */
@Parameters(resourceBundle = "sidplay.ini.IniWhatsSidSection")
public class IniWhatsSidSection extends IniSection implements IWhatsSidSection {

	private static final String SECTION_ID = "WhatsSID";

	protected IniWhatsSidSection(IniReader iniReader) {
		super(iniReader);
	}

	@Override
	public final boolean isEnable() {
		return iniReader.getPropertyBool(SECTION_ID, "Enable", DEFAULT_WHATSSID_ENABLE);
	}

	@Override
	@Parameter(names = { "--whatsSIDEnable" }, descriptionKey = "WHATSSID_ENABLE", arity = 1, order = 2000)
	public final void setEnable(boolean enable) {
		iniReader.setProperty(SECTION_ID, "Enable", enable);
	}

	@Override
	public final String getUrl() {
		return iniReader.getPropertyString(SECTION_ID, "Url", DEFAULT_WHATSSID_URL);
	}

	@Override
	@Parameter(names = { "--whatsSIDUrl" }, descriptionKey = "WHATSSID_URL", order = 2001)
	public final void setUrl(String url) {
		iniReader.setProperty(SECTION_ID, "Url", url);
	}

	@Override
	public final String getUsername() {
		return iniReader.getPropertyString(SECTION_ID, "Username", DEFAULT_WHATSSID_USERNAME);
	}

	@Override
	@Parameter(names = { "--whatsSIDUsername" }, descriptionKey = "WHATSSID_USERNAME", order = 2002)
	public final void setUsername(String username) {
		iniReader.setProperty(SECTION_ID, "Username", username);
	}

	@Override
	public final String getPassword() {
		return iniReader.getPropertyString(SECTION_ID, "Password", DEFAULT_WHATSSID_PASSWORD);
	}

	@Override
	@Parameter(names = { "--whatsSIDPassword" }, descriptionKey = "WHATSSID_PASSWORD", password = true, order = 2003)
	public final void setPassword(String password) {
		iniReader.setProperty(SECTION_ID, "Password", password);
	}

	@Override
	public final int getConnectionTimeout() {
		return iniReader.getPropertyInt(SECTION_ID, "Connection Timeout", DEFAULT_WHATSSID_CONNECTION_TIMEOUT);
	}

	@Override
	@Parameter(names = { "--whatsSIDConnectionTimeout" }, descriptionKey = "WHATSSID_CONNECTION_TIMEOUT", order = 2004)
	public final void setConnectionTimeout(int connectionTimeout) {
		iniReader.setProperty(SECTION_ID, "Connection Timeout", connectionTimeout);
	}

	@Override
	public final int getCaptureTime() {
		return iniReader.getPropertyInt(SECTION_ID, "Capture Time", DEFAULT_WHATSSID_CAPTURE_TIME);
	}

	@Override
	@Parameter(names = { "--whatsSIDCaptureTime" }, descriptionKey = "WHATSSID_CAPTURE_TIME", order = 2005)
	public final void setCaptureTime(int captureTime) {
		iniReader.setProperty(SECTION_ID, "Capture Time", captureTime);
	}

	@Override
	public final int getMatchStartTime() {
		return iniReader.getPropertyInt(SECTION_ID, "Match Start Time", DEFAULT_WHATSSID_MATCH_START_TIME);
	}

	@Override
	@Parameter(names = { "--whatsSIDMatchStartTime" }, descriptionKey = "WHATSSID_MATCH_START_TIME", order = 2006)
	public final void setMatchStartTime(int matchStartTime) {
		iniReader.setProperty(SECTION_ID, "Match Start Time", matchStartTime);
	}

	@Override
	public final int getMatchRetryTime() {
		return iniReader.getPropertyInt(SECTION_ID, "Match Retry Time", DEFAULT_WHATSSID_MATCH_RETRY_TIME);
	}

	@Override
	@Parameter(names = { "--whatsSIDMatchRetryTime" }, descriptionKey = "WHATSSID_MATCH_RETRY_TIME", order = 2007)
	public final void setMatchRetryTime(int matchRetryTime) {
		iniReader.setProperty(SECTION_ID, "Match Retry Time", matchRetryTime);
	}

	@Override
	public final float getMinimumRelativeConfidence() {
		return iniReader.getPropertyFloat(SECTION_ID, "Minimum Relative Confidence",
				DEFAULT_WHATSSID_MINIMUM_RELATIVE_CONFIDENCE);
	}

	@Override
	@Parameter(names = {
			"--whatsSIDMinimumRelativeConfidence" }, descriptionKey = "WHATSSID_MINIMUM_RELATIVE_CONFIDENCE", order = 2008)
	public final void setMinimumRelativeConfidence(float minimumRelativeConfidence) {
		iniReader.setProperty(SECTION_ID, "Minimum Relative Confidence", minimumRelativeConfidence);
	}

	@Override
	public final boolean isDetectChipModel() {
		return iniReader.getPropertyBool(SECTION_ID, "Detect ChipModel", DEFAULT_WHATSSID_DETECT_CHIP_MODEL);
	}

	@Override
	@Parameter(names = {
			"--whatsSIDDetectChipModel" }, descriptionKey = "WHATSSID_DETECT_CHIP_MODEL", arity = 1, order = 2009)
	public final void setDetectChipModel(boolean detectChipModel) {
		iniReader.setProperty(SECTION_ID, "Detect ChipModel", detectChipModel);
	}

	@Override
	public final String toString() {
		return BeanToStringConverter.toString(this);
	}

}
