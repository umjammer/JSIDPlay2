package sidplay.audio;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Optional;

import javax.sound.sampled.LineUnavailableException;

import libsidplay.common.CPUClock;
import libsidplay.common.EventScheduler;
import libsidplay.common.SamplingRate;
import libsidplay.config.IAudioSection;
import lowlevel.LameDecoder;
import sidplay.audio.exceptions.IniConfigException;
import sidplay.audio.exceptions.SongEndException;

/**
 * Sound driver to compare emulation with MP3 recording (listen to emulation and
 * MP3 recording in parallel).
 *
 * @author Ken Händel
 *
 */
public class CmpToMP3FileDriver extends JavaSound {

	/**
	 * Jump3r decoder.
	 */
	protected LameDecoder jump3r;
	private int factor;
	private ByteBuffer decodedMP3Buffer;
	private ByteBuffer mp3Buffer;

	private IAudioSection audioSection;

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		AudioConfig cfg = new AudioConfig(audioSection);
		this.audioSection = audioSection;
		File mp3 = audioSection.getMp3();
		if (mp3 == null) {
			throw new FileNotFoundException("mp3 file is missing");
		}
		if (!mp3.exists()) {
			throw new FileNotFoundException(mp3.getAbsolutePath() + " (No such file or directory)");
		}
		jump3r = new LameDecoder(mp3.getAbsolutePath());
		int sampleRate = jump3r.getSampleRate();
		int channels = jump3r.getChannels();
		int frameSize = jump3r.getFrameSize();
		Optional<SamplingRate> samplingRateFound = Arrays.asList(SamplingRate.values()).stream()
				.filter(samplingRate -> samplingRate.getFrequency() == sampleRate).findFirst();
		if (!samplingRateFound.isPresent()) {
			throw new IOException("Unsupported sample rate: " + sampleRate + " in " + mp3);
		}
		if (sampleRate != audioSection.getSamplingRate().getFrequency()) {
			throw new IniConfigException("Sampling rate does not match " + sampleRate + ", switch to it",
					() -> audioSection.setSamplingRate(samplingRateFound.get()));
		}
		decodedMP3Buffer = ByteBuffer.wrap(new byte[frameSize * Short.BYTES * channels]).order(ByteOrder.nativeOrder());

		factor = Math.max(1, cfg.getBufferFrames() / frameSize);
		mp3Buffer = ByteBuffer.allocateDirect(factor * frameSize * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.nativeOrder());

		super.open(cfg, getDeviceInfo(audioSection));
		if (buffer().capacity() < mp3Buffer.capacity()) {
			// Prevent BufferOverflowException
			cfg.setBufferFrames(mp3Buffer.capacity());
			cfg.setAudioBufferSize(mp3Buffer.capacity());
			super.open(cfg, getDeviceInfo(audioSection));
		}
	}

	@Override
	public void write() throws InterruptedException {
		((Buffer) mp3Buffer).clear();
		boolean decoded = true;
		for (int i = 0; i < factor; i++) {
			((Buffer) decodedMP3Buffer).clear();
			decoded &= jump3r.decode(decodedMP3Buffer);
			if (!decoded) {
				break;
			}
			if (jump3r.getChannels() == 1) {
				monoToStereo(decodedMP3Buffer, mp3Buffer);
			} else {
				mp3Buffer.put(decodedMP3Buffer);
			}
		}
		if (audioSection.isPlayOriginal()) {
			((Buffer) buffer()).clear();
			((Buffer) mp3Buffer).flip();
			buffer().put(mp3Buffer);
		}
		super.write();
		if (!decoded) {
			throw new SongEndException();
		}
	}

	@Override
	public void close() {
		super.close();
		if (jump3r != null) {
			jump3r.close();
		}
	}

	private void monoToStereo(ByteBuffer monoMP3Buffer, ByteBuffer stereoBuffer) {
		ShortBuffer monoBuffer = monoMP3Buffer.asShortBuffer();
		for (int i = 0; i < monoBuffer.limit(); i++) {
			short monoSample = monoBuffer.get();
			stereoBuffer.putShort(monoSample);
			stereoBuffer.putShort(monoSample);
		}
	}
}
