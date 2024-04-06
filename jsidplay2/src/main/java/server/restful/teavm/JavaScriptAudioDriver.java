package server.restful.teavm;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.LineUnavailableException;

import org.teavm.interop.Import;

import libsidplay.common.CPUClock;
import libsidplay.common.EventScheduler;
import libsidplay.config.IAudioSection;
import sidplay.audio.AudioConfig;
import sidplay.audio.AudioDriver;

public final class JavaScriptAudioDriver implements AudioDriver {
	protected ByteBuffer sampleBuffer;

	float[] resultL;
	float[] resultR;

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
		int shortPosition = sampleBuffer.position() >> 1;
		short[] dest = new short[shortPosition];
		((Buffer) sampleBuffer).flip();
		sampleBuffer.asShortBuffer().get(dest);
		((Buffer) sampleBuffer).position(shortPosition << 1);

		int channelDataLength = dest.length >> 1;
		int i = 0;
		for (i = 0; i < channelDataLength; i++) {
			resultL[i] = dest[i << 1] / 32768.0f;
			resultR[i] = dest[(i << 1) + 1] / 32768.0f;
		}
		for (; i < resultL.length; i++) {
			resultL[i] = 0f;
			resultR[i] = 0f;
		}
		processSamples(resultL, resultR, channelDataLength);
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