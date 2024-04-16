package client.teavm;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.sound.sampled.LineUnavailableException;

import org.teavm.interop.Import;

import libsidplay.common.CPUClock;
import libsidplay.common.EventScheduler;
import libsidplay.components.mos656x.VIC;
import libsidplay.config.IAudioSection;
import sidplay.audio.AudioConfig;
import sidplay.audio.AudioDriver;
import sidplay.audio.VideoDriver;

public final class JavaScriptAudioDriver implements AudioDriver, VideoDriver {

	protected ByteBuffer sampleBuffer;

	private FloatBuffer resultL, resultR;
	private ByteBuffer pixels;
	private int n, nthFrame;

	public JavaScriptAudioDriver(int nthFrame) {
		this.nthFrame = nthFrame;
	}

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		AudioConfig cfg = new AudioConfig(audioSection);
		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);

		resultL = FloatBuffer.wrap(new float[cfg.getChunkFrames()]);
		resultR = FloatBuffer.wrap(new float[cfg.getChunkFrames()]);
		pixels = ByteBuffer.wrap(new byte[(VIC.MAX_WIDTH * VIC.MAX_HEIGHT) << 2]);
	}

	@Override
	public void write() throws InterruptedException {
		int frameCount = sampleBuffer.position() >> 2;
		((Buffer) sampleBuffer).flip();
		// SHORT to FLOAT samples
		ShortBuffer shortBuffer = sampleBuffer.asShortBuffer();
		for (int i = 0; i < frameCount; i++) {
			resultL.put(shortBuffer.get() / 32768.0f);
			resultR.put(shortBuffer.get() / 32768.0f);
		}
		((Buffer) sampleBuffer).position(frameCount << 2);
		processSamples(resultL.array(), resultR.array(), frameCount);
		((Buffer) resultL).clear();
		((Buffer) resultR).clear();
	}

	@Override
	public void accept(VIC vic) {
		if (++n == nthFrame) {
			n = 0;
			// ARGB to RGBA
			for (int argb : vic.getPixels().array()) {
				pixels.put((byte) ((argb >> 16) & 0xff));
				pixels.put((byte) ((argb >> 8) & 0xff));
				pixels.put((byte) (argb & 0xff));
				pixels.put((byte) ((argb >> 24) & 0xff));
			}
			processPixels(pixels.array());
			((Buffer) pixels).clear();
		}
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

	@Import(module = "env", name = "processPixels")
	private static native void processPixels(byte[] pixels);

}