package client.teavm.config;

import libsidplay.config.IWhatsSidSection;

public final class JavaScriptWhatsSidSection implements IWhatsSidSection {
	@Override
	public void setUsername(String username) {
	}

	@Override
	public void setUrl(String url) {
	}

	@Override
	public void setPassword(String password) {
	}

	@Override
	public void setMinimumRelativeConfidence(float minimumRelativeConfidence) {
	}

	@Override
	public void setMatchStartTime(int matchStartTime) {
	}

	@Override
	public void setMatchRetryTime(int matchRetryTime) {
	}

	@Override
	public void setEnable(boolean enable) {
	}

	@Override
	public void setDetectChipModel(boolean detectChipModel) {
	}

	@Override
	public void setConnectionTimeout(int connectionTimeout) {
	}

	@Override
	public void setCaptureTime(int captureTime) {
	}

	@Override
	public boolean isEnable() {
		return false;
	}

	@Override
	public boolean isDetectChipModel() {
		return false;
	}

	@Override
	public String getUsername() {
		return null;
	}

	@Override
	public String getUrl() {
		return null;
	}

	@Override
	public String getPassword() {
		return null;
	}

	@Override
	public float getMinimumRelativeConfidence() {
		return 4.5f;
	}

	@Override
	public int getMatchStartTime() {
		return 15;
	}

	@Override
	public int getMatchRetryTime() {
		return 15;
	}

	@Override
	public int getConnectionTimeout() {
		return 5000;
	}

	@Override
	public int getCaptureTime() {
		return 15;
	}
}