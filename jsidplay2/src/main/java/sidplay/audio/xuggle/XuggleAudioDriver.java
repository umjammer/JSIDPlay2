package sidplay.audio.xuggle;

import static com.xuggle.mediatool.ToolFactory.makeWriter;
import static com.xuggle.xuggler.io.XugglerIO.generateUniqueName;
import static com.xuggle.xuggler.io.XugglerIO.map;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.sound.sampled.LineUnavailableException;

import com.xuggle.mediatool.IMediaWriter;
import com.xuggle.xuggler.ICodec.ID;
import com.xuggle.xuggler.IContainerFormat;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.io.InputOutputStreamHandler;

import libsidplay.common.CPUClock;
import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;
import libsidplay.common.SamplingRate;
import libsidplay.config.IAudioSection;
import sidplay.audio.AudioConfig;
import sidplay.audio.AudioDriver;
import sidplay.audio.exceptions.IniConfigException;

public abstract class XuggleAudioDriver extends XuggleBase implements AudioDriver {

	private class AbortingOutputStreamHandler extends InputOutputStreamHandler {
		public AbortingOutputStreamHandler(OutputStream out) {
			super(null, out, true);
		}

		public int write(byte[] buf, int size) {
			try {
				if (getOpenStream() == null || !(getOpenStream() instanceof OutputStream)) {
					return -1;
				}
				OutputStream stream = (OutputStream) getOpenStream();
				stream.write(buf, 0, size);
				return size;
			} catch (IOException e) {
				// signal to abort recording immediately next call of write
				aborted = true;
				return -1;
			}
		};
	}

	protected OutputStream out;

	private EventScheduler context;

	private IMediaWriter writer;
	private long firstTimeStamp;
	private double ticksPerMicrosecond;
	boolean aborted;

	protected ByteBuffer sampleBuffer;

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
		throws IOException, LineUnavailableException, InterruptedException {
		this.context = context;
		AudioConfig cfg = new AudioConfig(audioSection);
		out = getOut(recordingFilename);

		if (!getSupportedSamplingRates().contains(audioSection.getSamplingRate())) {
			throw new IniConfigException("Sampling rate is not supported by encoder, switch to default",
					() -> audioSection.setSamplingRate(getDefaultSamplingRate()));
		}

		writer = makeWriter(map(generateUniqueName(out), new AbortingOutputStreamHandler(out)));

		IContainerFormat containerFormat = IContainerFormat.make();
		containerFormat.setOutputFormat(getOutputFormatName(), null, null);

		writer.getContainer().setFormat(containerFormat);
		throwExceptionOnError(writer.addAudioStream(0, 0, getAudioCodec(), cfg.getChannels(), cfg.getFrameRate()));

		configureStreamCoder(writer.getContainer().getStream(0).getStreamCoder(), audioSection);

		aborted = false;
		firstTimeStamp = 0;
		ticksPerMicrosecond = cpuClock.getCpuFrequency() / 1000000;

		sampleBuffer = ByteBuffer.allocateDirect(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public void write() throws InterruptedException {
		if (aborted) {
			throw new RuntimeException("Error writing MP3 audio stream");
		}
		long timeStamp = getTimeStamp();

		short[] shortArray = new short[sampleBuffer.position() >> 1];
		((Buffer) sampleBuffer).flip();
		sampleBuffer.asShortBuffer().get(shortArray);

		writer.encodeAudio(0, shortArray, timeStamp, TimeUnit.MICROSECONDS);
	}

	@Override
	public void close() {
		try {
			if (writer != null && writer.isOpen()) {
				writer.close();
			}
		} finally {
			writer = null;
		}
	}

	@Override
	public ByteBuffer buffer() {
		return sampleBuffer;
	}

	@Override
	public boolean isRecording() {
		return true;
	}

	private long getTimeStamp() {
		long now = context.getTime(Phase.PHI2);
		if (firstTimeStamp == 0) {
			firstTimeStamp = now;
		}
		return (long) ((now - firstTimeStamp) / ticksPerMicrosecond);
	}

	protected void configureStreamCoder(IStreamCoder streamCoder, IAudioSection audioSection) {
	}

	protected abstract List<SamplingRate> getSupportedSamplingRates();

	protected abstract String getOutputFormatName();

	protected abstract SamplingRate getDefaultSamplingRate();

	protected abstract ID getAudioCodec();

	protected abstract OutputStream getOut(String recordingFilename) throws IOException;

}
