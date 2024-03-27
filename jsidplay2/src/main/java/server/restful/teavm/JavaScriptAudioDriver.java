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

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		AudioConfig cfg = new AudioConfig(audioSection);
		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public void write() throws InterruptedException {
		int shortPosition = sampleBuffer.position() >> 1;
		short[] dest = new short[shortPosition];
		((Buffer) sampleBuffer).flip();
		sampleBuffer.asShortBuffer().get(dest);
		int channelDataLength = dest.length >> 1;
		float[] resultL = new float[channelDataLength];
		float[] resultR = new float[channelDataLength];
		for (int i = 0; i < channelDataLength; i++) {
			resultL[i] = dest[i << 1] / 32768.0f;
			resultR[i] = dest[(i << 1) + 1] / 32768.0f;
		}
		processSamples(resultL, resultR, channelDataLength);
		((Buffer) sampleBuffer).position(shortPosition << 1);
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