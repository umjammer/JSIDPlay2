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
import libsidplay.components.mos656x.VIC;
import libsidplay.config.IAudioSection;
import sidplay.audio.AudioConfig;
import sidplay.audio.AudioDriver;
import sidplay.audio.VideoDriver;

public final class JavaScriptAudioDriver implements AudioDriver, VideoDriver {

	protected ByteBuffer sampleBuffer;

	private float[] resultL, resultR;
	private byte[] pixels;
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

		resultL = new float[cfg.getChunkFrames()];
		resultR = new float[cfg.getChunkFrames()];
		pixels = new byte[(VIC.MAX_WIDTH * VIC.MAX_HEIGHT) << 2];
	}

	@Override
	public void write() throws InterruptedException {
		int frameCount = sampleBuffer.position() >> 2;
		((Buffer) sampleBuffer).flip();
		// SHORT to FLOAT samples
		ShortBuffer shortBuffer = sampleBuffer.asShortBuffer();
		for (int i = 0; i < frameCount; i++) {
			resultL[i] = shortBuffer.get() / 32768.0f;
			resultR[i] = shortBuffer.get() / 32768.0f;
		}
		((Buffer) sampleBuffer).position(frameCount << 2);
		processSamples(resultL, resultR, frameCount);
	}

	@Override
	public void accept(VIC vic) {
		if (++n == nthFrame) {
			n = 0;
			// ARGB to RGBA
			int[] array = vic.getPixels().array();
			for (int i = 0; i < array.length; i++) {
				int argb = array[i];
				pixels[i << 2] = (byte) ((argb >> 16) & 0xff);
				pixels[(i << 2) + 1] = (byte) ((argb >> 8) & 0xff);
				pixels[(i << 2) + 2] = (byte) (argb & 0xff);
				pixels[(i << 2) + 3] = (byte) ((argb >> 24) & 0xff);
			}
			processPixels(pixels);
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