package sidplay.audio;

import static java.lang.Short.BYTES;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static libsidplay.config.IAudioSystemProperties.MAX_TIME_GAP;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

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

	private static final Logger SLEEP_DRIVER = Logger.getLogger(SleepDriver.class.getName());

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
	private volatile Long clientTime;

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
		clientTime = null;
		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * BYTES * cfg.getChannels()).order(LITTLE_ENDIAN);
	}

	@Override
	public void write() throws InterruptedException {
		if (startTime == 0) {
			startTime = System.currentTimeMillis();
			startC64Time = context.getTime(Phase.PHI2);
		}
		time = clientTime != null ? clientTime : System.currentTimeMillis() - startTime;
		time += /* from rolling a dice */ 4000;
		c64Time = (long) ((context.getTime(Phase.PHI2) - startC64Time) * 1000 / cpuClock.getCpuFrequency());

		long gap = c64Time - time;
		if (gap > MAX_TIME_GAP) {
			// slow down video production, that a client-side fastForward after a key press
			// jumps not too far, but not long enough to block a fast forward
			final long sleepTime = Math.min(gap - MAX_TIME_GAP, 1000);

			if (SLEEP_DRIVER.isLoggable(Level.FINE)) {
				SLEEP_DRIVER.fine(String.format("recordingTime=%s, clientTime=%s, c64Time=%s, time=%s, sleepTime=%s",
						millisToDate(System.currentTimeMillis() - startTime), millisToDate(clientTime),
						millisToDate(c64Time), millisToDate(time), millisToDate(sleepTime)));
			}
			Thread.sleep(sleepTime);
		}
	}

	public void setClientTime(Long clientTime) {
		this.clientTime = clientTime;
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

	private String millisToDate(Long millis) {
		if (millis == null) {
			return "00:00:00.000";
		}
		return (new SimpleDateFormat("mm:ss:SSS")).format(new Date(millis));
	}

}
