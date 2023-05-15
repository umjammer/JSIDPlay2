package sidplay.audio;

import static java.lang.Short.BYTES;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static libsidplay.config.IAudioSystemProperties.MAX_TIME_GAP;

import java.io.IOException;
import java.nio.ByteBuffer;

import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.CPUClock;
import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;
import libsidplay.config.IAudioSection;

/**
 * No sound output at all, but sleeps to slow down video production.
 * 
 * @author ken
 *
 */
public class SleepDriver implements AudioDriver {

	private CPUClock cpuClock;
	private EventScheduler context;

	/**
	 * Real-time since recording started
	 */
	private long startTime, time;
	/**
	 * C64 emulation time (emulation runs much faster than real-time)
	 */
	private long startC64Time, c64Time;
	/**
	 * Current time of a video player client
	 */
	private volatile long currentTime;

	private ByteBuffer sampleBuffer;

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		this.cpuClock = cpuClock;
		this.context = context;
		AudioConfig cfg = new AudioConfig(audioSection);

		startTime = 0;
		time = 0;
		startC64Time = 0;
		c64Time = 0;
		currentTime = 0;
		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * BYTES * cfg.getChannels()).order(LITTLE_ENDIAN);
	}

	@Override
	public void write() throws InterruptedException {
		if (startTime == 0) {
			startTime = System.currentTimeMillis();
			startC64Time = context.getTime(Phase.PHI2);
		}
		time = Math.max(System.currentTimeMillis() - startTime, currentTime + /* from rolling a dice */4000);
		c64Time = (long) ((context.getTime(Phase.PHI2) - startC64Time) * 1000 / cpuClock.getCpuFrequency());

		long sleepTime = c64Time - time;
		if (sleepTime > MAX_TIME_GAP) {
			try {
				long sleep = 1000;
//				System.err.println("c64Time: " + (c64Time / 60000) + ":" + (c64Time % 60000));
//				System.err.println("time: " + (time / 60000) + ":" + (time % 60000));
//				System.err.println("Sleep: " + sleep);

				// slow down video production, that a client-side fastForward after a key press
				// jumps not too far
				Thread.sleep(sleep);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void setCurrentTime(Long currentTime) {
		this.currentTime = currentTime;
	}

	@Override
	public void close() {
	}

	@Override
	public ByteBuffer buffer() {
		return sampleBuffer;
	}

	@Override
	public boolean isRecording() {
		return false;
	}

}
