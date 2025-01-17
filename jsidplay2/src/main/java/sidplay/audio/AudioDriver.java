/**
 *                                  description
 *                                  -----------
 *  begin                : Sat Jul 8 2000
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
package sidplay.audio;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Optional;

import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.CPUClock;
import libsidplay.common.EventScheduler;
import libsidplay.config.IAudioSection;

public interface AudioDriver {

	/**
	 * Open audio interface.
	 *
	 * @param audioSection      audio configuration
	 * @param recordingFilename name for a recording
	 * @param cpuClock          CPU clock
	 * @param context           event context
	 * @throws IOException
	 * @throws LineUnavailableException
	 */
	void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException;

	/**
	 * Write the complete contents of ByteBuffer to audio device.
	 *
	 * @throws InterruptedException
	 */
	void write() throws InterruptedException;

	/**
	 * Temporarily cease audio production, for instance if user paused the
	 * application. Some backends such as DirectSound end up looping the audio
	 * unless explicitly told to pause.
	 *
	 * Audio will be resumed automatically on next write().
	 */
	default void pause() {
	}

	/**
	 * Free the audio device. (Counterpart of open().)
	 */
	void close();

	/**
	 * Return the bytebuffer intended to hold the audio data.
	 *
	 * The audio data is in interleaved format and has as many channels as given by
	 * the result of open(). Use putShort() to write 16-bit values. Don't call
	 * write() until you have filled the entire buffer with audio.
	 *
	 * @return The buffer to write audio to.
	 */
	ByteBuffer buffer();

	/**
	 * @return is this audio driver recording tunes?
	 */
	boolean isRecording();

	/**
	 * @return file extension for recordings
	 */
	default String getExtension() {
		return null;
	}

	/**
	 * @return concrete audio driver, if proxied
	 */
	default <T extends AudioDriver> Optional<T> lookup(Class<T> clz) {
		if (clz.isInstance(this)) {
			return Optional.of(clz.cast(this));
		}
		return Optional.empty();
	}

}
