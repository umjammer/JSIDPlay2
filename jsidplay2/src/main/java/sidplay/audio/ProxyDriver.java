package sidplay.audio;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.Optional;

import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.CPUClock;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDListener;
import libsidplay.components.mos6510.IMOS6510Extension;
import libsidplay.components.mos656x.VIC;
import libsidplay.config.IAudioSection;

/**
 * Proxy driver to use two different sound or video drivers at the same time.
 *
 * <B>Note:</B> Both driver's sample buffer must be equal in size.
 *
 * @author Ken Händel
 *
 */
public class ProxyDriver implements AudioDriver, VideoDriver, SIDListener, IMOS6510Extension {
	private final AudioDriver driverOne;
	private final AudioDriver driverTwo;

	/**
	 * Create a proxy driver
	 *
	 * @param driver1 sound driver, that buffer gets filled
	 * @param driver2 sound driver, that gets the copied sample buffer
	 */
	public ProxyDriver(final AudioDriver driver1, final AudioDriver driver2) {
		driverOne = driver1;
		driverTwo = driver2;
	}

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		driverOne.open(audioSection, recordingFilename, cpuClock, context);
		driverTwo.open(audioSection, recordingFilename, cpuClock, context);
	}

	@Override
	public void pause() {
		driverOne.pause();
		driverTwo.pause();
	}

	@Override
	public void write() throws InterruptedException {
		driverOne.write();
		ByteBuffer driverTwoBuffer = driverTwo.buffer();
		if (driverTwoBuffer != null) {
			// Driver two's buffer gets the contents of driver one's buffer
			ByteBuffer readOnlyCopy = driverOne.buffer().asReadOnlyBuffer();
			((Buffer) readOnlyCopy).flip();
			((Buffer) driverTwoBuffer).clear();
			driverTwoBuffer.put(readOnlyCopy);
			driverTwo.write();
		}
	}

	@Override
	public void accept(VIC vic) {
		if (driverOne instanceof VideoDriver) {
			((VideoDriver) driverOne).accept(vic);
		}
		if (driverTwo instanceof VideoDriver) {
			((VideoDriver) driverTwo).accept(vic);
		}
	}

	@Override
	public void write(int addr, byte data) {
		if (driverOne instanceof SIDListener) {
			((SIDListener) driverOne).write(addr, data);
		}
		if (driverTwo instanceof SIDListener) {
			((SIDListener) driverTwo).write(addr, data);
		}
	}

	@Override
	public void jmpJsr() {
		if (driverOne instanceof IMOS6510Extension) {
			((IMOS6510Extension) driverOne).jmpJsr();
		}
		if (driverTwo instanceof IMOS6510Extension) {
			((IMOS6510Extension) driverTwo).jmpJsr();
		}
	}

	@Override
	public void close() {
		driverOne.close();
		driverTwo.close();
	}

	/**
	 * Driver one's buffer gets filled, while driver two gets a copy in method
	 * write()
	 */
	@Override
	public ByteBuffer buffer() {
		return driverOne.buffer();
	}

	@Override
	public boolean isRecording() {
		return driverOne.isRecording() || driverTwo.isRecording();
	}

	@Override
	public String getExtension() {
		return driverOne.getExtension() != null ? driverOne.getExtension() : driverTwo.getExtension();
	}

	@Override
	public <T extends AudioDriver> Optional<T> lookup(Class<T> clz) {
		if (clz.isInstance(driverOne)) {
			return Optional.of(clz.cast(driverOne));
		} else if (clz.isInstance(driverTwo)) {
			return Optional.of(clz.cast(driverTwo));
		}
		return Optional.empty();
	}

	public AudioDriver getDriverOne() {
		return driverOne;
	}

	public AudioDriver getDriverTwo() {
		return driverTwo;
	}

}
