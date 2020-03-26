package libsidplay.config;

public interface IWhatsSidSection {

	/**
	 * Getter of the WhatsSid enable.
	 * 
	 * @return the WhatsSid enable
	 */
	boolean isEnable();
	
	/**
	 * Setter of the WhatsSid enable.
	 * 
	 * @param url the WhatsSid enable
	 */
	void setEnable(boolean enable);
	
	/**
	 * Getter of the WhatsSid url.
	 * 
	 * @return the WhatsSid url
	 */
	String getUrl();

	/**
	 * Setter of the WhatsSid url.
	 * 
	 * @param url the WhatsSid url
	 */
	void setUrl(String url);

	/**
	 * Getter of the WhatsSid username.
	 * 
	 * @return the WhatsSid username
	 */
	String getUsername();

	/**
	 * Setter of the WhatsSid username.
	 * 
	 * @param username the WhatsSid username
	 */
	void setUsername(String username);

	/**
	 * Getter of the WhatsSid password.
	 * 
	 * @return the WhatsSid password
	 */
	String getPassword();

	/**
	 * Setter of the WhatsSid password.
	 * 
	 * @param password the WhatsSid password
	 */
	void setPassword(String password);

	/**
	 * Getter of the capture time.
	 * 
	 * @return the capture time
	 */
	int getCaptureTime();

	/**
	 * Setter of the capture time.
	 * 
	 * @param captureTime the capture time
	 */
	void setCaptureTime(int captureTime);

	/**
	 * Getter of the match start time.
	 * 
	 * @return the match start time
	 */
	int getMatchStartTime();

	/**
	 * Setter of the match start time.
	 * 
	 * @param matchStartTime the match start time
	 */
	void setMatchStartTime(int matchStartTime);
}