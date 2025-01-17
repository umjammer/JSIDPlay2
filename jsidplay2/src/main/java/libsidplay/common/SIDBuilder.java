/**
 *                           Sid Builder Classes
 *                           -------------------
 *  begin                : Sat May 6 2001
 *  copyright            : (C) 2001 by Simon White
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
package libsidplay.common;

import libsidplay.sidtune.SidTune;

/**
 * @author Ken Händel
 *
 *         Implement this class to create a new SID emulation for libsidplay2.
 */
public interface SIDBuilder {
	/**
	 * Create a new SID chip emulation.
	 *
	 * @param device old SID chip in use
	 * @param sidNum SID chip number
	 * @param tune   current tune
	 *
	 * @return emulated SID chip
	 */
	SIDEmu lock(SIDEmu device, int sidNum, SidTune tune);

	/**
	 * Destroy SID chip emulation.
	 *
	 * @param device SID chip to destroy
	 */
	void unlock(SIDEmu device);

	default void destroy() {
	}

}
