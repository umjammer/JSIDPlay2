package sidplay.audio;

import static javax.sound.sampled.AudioSystem.getMixerInfo;
import static javax.sound.sampled.AudioSystem.getSourceDataLine;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.FloatControl.Type;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Mixer.Info;
import javax.sound.sampled.SourceDataLine;

import libsidplay.common.CPUClock;
import libsidplay.common.EventScheduler;
import libsidplay.config.IAudioSection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JavaSound implements AudioDriver {

	private static final Logger logger = LoggerFactory.getLogger(JavaSound.class.getName());

	private AudioConfig cfg;
	private AudioFormat audioFormat;
	private SourceDataLine dataLine;
	private ByteBuffer sampleBuffer;

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		open(new AudioConfig(audioSection), getDeviceInfo(audioSection));
	}

	/**
	 * The audio parameters may be manipulated by open().
	 *
	 * @param cfg  audio configuration
	 * @param info mixer info
	 * @throws LineUnavailableException
	 */
	public void open(final AudioConfig cfg, final Info info) throws LineUnavailableException {
		this.cfg = cfg;
		boolean signed = true;
		boolean bigEndian = false;
		audioFormat = new AudioFormat(cfg.getFrameRate(), Short.SIZE, cfg.getChannels(), signed, bigEndian);
		setAudioDevice(info);
	}

	public void setAudioDevice(final Info info) throws LineUnavailableException {
		try {
			// first close previous dataLine when it is already present
			close();
			dataLine = getSourceDataLine(audioFormat, info);
			dataLine.open(dataLine.getFormat(), cfg.getBufferFrames() * Short.BYTES * cfg.getChannels());

			dataLine.start();

			FloatControl gainControl = (FloatControl) dataLine.getControl(Type.MASTER_GAIN);
logger.debug("MainVolume: {}", cfg.getMainVolume());
			gainControl.setValue(cfg.getMainVolume());

			// The actual buffer size for the open line may differ from the
			// requested buffer size, therefore
			cfg.setBufferFrames(dataLine.getBufferSize() / Short.BYTES / cfg.getChannels());
		} catch (IllegalArgumentException e) {
			// mixer not supported? No sound! Hardware based SIDBuilders can still be used.
			System.err.println(e.getMessage());
		}
		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public void write() throws InterruptedException {
		if (dataLine == null) {
			return;
		}
		// in pause mode next call of write continues
		if (!dataLine.isActive()) {
			dataLine.start();
		}
		dataLine.write(sampleBuffer.array(), 0, sampleBuffer.position());
	}

	/**
	 * Estimate the length of audio data before we run out
	 *
	 * @return playback time in ms
	 */
	public int getRemainingPlayTime() {
		if (dataLine == null) {
			return 0;
		}
		int bytesPerFrame = dataLine.getFormat().getChannels() * Short.BYTES;
		int framesPlayed = dataLine.available() / bytesPerFrame;
		int framesTotal = dataLine.getBufferSize() / bytesPerFrame;
		int framesNotYetPlayed = framesTotal - framesPlayed;
		return framesNotYetPlayed * 1000 / framesTotal;
	}

	@Override
	public void pause() {
		if (dataLine != null && dataLine.isActive()) {
			dataLine.stop();
		}
	}

	public void flush() {
		if (dataLine != null && dataLine.isActive()) {
			dataLine.flush();
		}
	}

	@Override
	public void close() {
		if (dataLine == null) {
			return;
		}
		if (dataLine.isActive()) {
			if (dataLine.isRunning()) {
				dataLine.drain();
			}
			dataLine.stop();
			dataLine.flush();
		}
		if (dataLine.isOpen()) {
			dataLine.close();
		}
	}

	@Override
	public ByteBuffer buffer() {
		return sampleBuffer;
	}

	@Override
	public boolean isRecording() {
		return false;
	}

	public static final Info getDeviceInfo(IAudioSection audioSection) {
		List<Info> devices = getDeviceInfos();
		return IntStream.range(0, devices.size()).filter(i -> i == audioSection.getDevice()).mapToObj(devices::get)
				.findFirst().orElse((Info) null);
	}

	public static final List<Info> getDeviceInfos() {
		return Arrays.asList(getMixerInfo()).stream().map(AudioSystem::getMixer)
				.filter(mixer -> mixer.isLineSupported(new Line.Info(SourceDataLine.class))).map(Mixer::getMixerInfo)
				.collect(Collectors.toList());
	}

}
