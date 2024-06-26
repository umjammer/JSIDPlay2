package client.teavm.common.audio;

import static libsidplay.common.CPUClock.PAL;
import static libsidplay.components.mos656x.MOS6569.BORDER_HEIGHT;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.sound.sampled.LineUnavailableException;

import client.teavm.common.IImportedApi;
import client.teavm.common.video.PALEmulationTeaVM;
import libsidplay.common.CPUClock;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.Mixer;
import libsidplay.common.SIDBuilder;
import libsidplay.common.SIDListener;
import libsidplay.components.mos656x.MOS6567;
import libsidplay.components.mos656x.VIC;
import libsidplay.config.IAudioSection;
import sidplay.audio.AudioConfig;
import sidplay.audio.AudioDriver;
import sidplay.audio.VideoDriver;

/**
 * Audio driver to be used in the JavaScript and web assembly version builds.
 * Browser needs float array for each channel with sound samples with a value
 * range of -1..1. And pixel data is required as a byte array containing color
 * data four bytes each pixel RGBA. Additionally the possibility to sniff for
 * SID writes helps to make USB hardware working in the browser and for debug
 * purposes.
 * 
 * <B>Note:</B> A lookup table is used for sample data conversion (short to
 * float) for performance reasons
 */
public final class AudioDriverTeaVM implements AudioDriver, VideoDriver, SIDListener {

	private final IImportedApi importedApi;
	private final SIDBuilder sidBuilder;
	private final int nthFrame;
	private final byte[] pixelsArray;
	private final float[] lookupTable;

	private EventScheduler context;
	private ByteBuffer sampleBuffer;
	private ShortBuffer shortBuffer;
	private FloatBuffer resultL, resultR;
	private int n, pixelsLength;
	private long sidWriteTime;
	private int fastForwardVICFrames;

	public AudioDriverTeaVM(IImportedApi importedApi, SIDBuilder sidBuilder, PALEmulationTeaVM palEmulation) {
		this.importedApi = importedApi;
		this.sidBuilder = sidBuilder;
		nthFrame = palEmulation != null ? palEmulation.getNthFrame() : 0;
		fastForwardVICFrames = 0;
		pixelsArray = palEmulation != null ? palEmulation.getPixels().array() : null;
		lookupTable = new float[65536];
		for (int i = Short.MIN_VALUE; i <= Short.MAX_VALUE; i++) {
			lookupTable[i + 32768] = (float) (i / 32768.0f);
		}
	}

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		this.context = context;
		AudioConfig cfg = new AudioConfig(audioSection);

		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);
		shortBuffer = sampleBuffer.asShortBuffer();
		resultL = FloatBuffer.wrap(new float[cfg.getChunkFrames()]);
		resultR = FloatBuffer.wrap(new float[cfg.getChunkFrames()]);

		n = 0;
		sidWriteTime = 0L;
		pixelsLength = VIC.MAX_WIDTH * (cpuClock == PAL ? BORDER_HEIGHT : MOS6567.BORDER_HEIGHT) << 2;
	}

	@Override
	public void write() throws InterruptedException {
		int position = sampleBuffer.position();
		((Buffer) shortBuffer).limit(position >> 1);
		while (shortBuffer.hasRemaining()) {
			resultL.put(lookupTable[shortBuffer.get() + 32768]);
			resultR.put(lookupTable[shortBuffer.get() + 32768]);
		}
		importedApi.processSamples(resultL.array(), resultR.array(), resultL.position());
		((Buffer) resultL).clear();
		((Buffer) resultR).clear();
		((Buffer) shortBuffer).rewind();
	}

	public void writeRemaining() throws InterruptedException {
		int position = sampleBuffer.position();
		if (position < sampleBuffer.capacity()) {
			resultL = FloatBuffer.wrap(new float[position >> 2]);
			resultR = FloatBuffer.wrap(new float[position >> 2]);
		}
		write();
	}

	@Override
	public void accept(VIC vic) {
		int fastForwardBitMask = ((Mixer) sidBuilder).getFastForwardBitMask();
		if ((fastForwardVICFrames++ & fastForwardBitMask) == fastForwardBitMask) {
			if (++n == nthFrame) {
				n = 0;
				importedApi.processPixels(pixelsArray, pixelsLength);
			}
		}
	}

	@Override
	public void write(int addr, byte data) {
		final long time = context.getTime(Event.Phase.PHI2);
		if (sidWriteTime == 0) {
			sidWriteTime = time;
		}
		importedApi.processSidWrite(time, (int) (time - sidWriteTime), addr, data & 0xff);
		sidWriteTime = time;
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

}