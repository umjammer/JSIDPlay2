package client.teavm;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.sound.sampled.LineUnavailableException;

import org.teavm.interop.Import;

import libsidplay.common.CPUClock;
import libsidplay.common.EventScheduler;
import libsidplay.config.IAudioSection;
import sidplay.audio.AudioConfig;
import sidplay.audio.AudioDriver;

public final class JavaScriptAudioDriver implements AudioDriver {

	protected ByteBuffer sampleBuffer;

	private float[] resultL;
	private float[] resultR;

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		AudioConfig cfg = new AudioConfig(audioSection);
		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);

		resultL = new float[cfg.getChunkFrames()];
		resultR = new float[cfg.getChunkFrames()];
	}

	@Override
	public void write() throws InterruptedException {
		int frameCount = sampleBuffer.position() >> 2;
		((Buffer) sampleBuffer).flip();
		ShortBuffer shortBuffer = sampleBuffer.asShortBuffer();
		for (int i = 0; i < frameCount; i++) {
			resultL[i] = shortBuffer.get() / 32768.0f;
			resultR[i] = shortBuffer.get() / 32768.0f;
		}
		((Buffer) sampleBuffer).position(frameCount << 2);
		processSamples(resultL, resultR, frameCount);
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
		return true;
	}

	/* This method maps to a JavaScript method in a web page. */
	@Import(module = "env", name = "processSamples")
	private static native void processSamples(float[] resultL, float[] resultR, int length);

}