package client.teavm;

import static libsidplay.common.CPUClock.PAL;
import static libsidplay.components.mos656x.MOS6569.BORDER_HEIGHT;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.sound.sampled.LineUnavailableException;

import org.teavm.interop.Import;

import libsidplay.common.CPUClock;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SIDListener;
import libsidplay.components.mos656x.MOS6567;
import libsidplay.components.mos656x.VIC;
import libsidplay.config.IAudioSection;
import sidplay.audio.AudioConfig;
import sidplay.audio.AudioDriver;
import sidplay.audio.VideoDriver;

public final class JavaScriptAudioDriver implements AudioDriver, VideoDriver, SIDListener {

	private CPUClock cpuClock;
	private EventScheduler context;
	private ByteBuffer sampleBuffer;

	private FloatBuffer resultL, resultR;
	private float[] lookupTable = new float[65536];
	private int n, nthFrame;
	private long sidWiteTime;

	private int[] array;
	private int length;

	public JavaScriptAudioDriver(int nthFrame) {
		this.nthFrame = nthFrame;
	}

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		this.cpuClock = cpuClock;
		this.context = context;
		AudioConfig cfg = new AudioConfig(audioSection);
		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);

		resultL = FloatBuffer.wrap(new float[cfg.getChunkFrames()]);
		resultR = FloatBuffer.wrap(new float[cfg.getChunkFrames()]);
		n = 0;
		sidWiteTime = 0;
		for (int i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++) {
			lookupTable[i + 32768] = (float) (i / 32768.0f);
		}
	}

	@Override
	public void write() throws InterruptedException {
		int position = ((Buffer) sampleBuffer).position();
		((Buffer) sampleBuffer).flip();
		// SHORT to FLOAT samples
		ShortBuffer shortBuffer = sampleBuffer.asShortBuffer();
		while (shortBuffer.hasRemaining()) {
			resultL.put(lookupTable[shortBuffer.get() + 32768]);
			resultR.put(lookupTable[shortBuffer.get() + 32768]);
		}
		((Buffer) sampleBuffer).position(position);
		processSamples(resultL.array(), resultR.array(), resultL.position());
		((Buffer) resultL).clear();
		((Buffer) resultR).clear();
	}

	@Override
	public void accept(VIC vic) {
		if (++n == nthFrame) {
			n = 0;
			if (array == null) {
				array = vic.getPalEmulation().getPixels().array();
				length = VIC.MAX_WIDTH * (cpuClock == PAL ? BORDER_HEIGHT : MOS6567.BORDER_HEIGHT) << 2;
			}
			processPixels(array, length);
		}
	}

	@Override
	public void write(int addr, byte data) {
		final long time = context.getTime(Event.Phase.PHI2);
		if (sidWiteTime == 0) {
			sidWiteTime = time;
		}
		processSidWrite(time, time - sidWiteTime, addr, data & 0xff);
		sidWiteTime = time;
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

	/* This methods maps to a JavaScript methods in a web page. */
	@Import(module = "audiodriver", name = "processSamples")
	private static native void processSamples(float[] resultL, float[] resultR, int length);

	@Import(module = "audiodriver", name = "processPixels")
	private static native void processPixels(int[] pixels, int length);

	@Import(module = "audiodriver", name = "processSidWrite")
	private static native void processSidWrite(long time, long relTime, int addr, int value);

}