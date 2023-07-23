package sidplay.audio.xuggle;

import static com.xuggle.xuggler.IAudioSamples.Format.FMT_S16;
import static com.xuggle.xuggler.IContainer.Type.WRITE;
import static com.xuggle.xuggler.IPixelFormat.Type.YUV420P;
import static com.xuggle.xuggler.IStreamCoder.Flags.FLAG_QSCALE;
import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static java.lang.Short.BYTES;
import static java.nio.ByteOrder.LITTLE_ENDIAN;
import static libsidplay.common.CPUClock.PAL;
import static libsidplay.components.mos656x.MOS6569.BORDER_HEIGHT;
import static libsidplay.components.mos656x.VIC.MAX_HEIGHT;
import static libsidplay.components.mos656x.VIC.MAX_WIDTH;
import static libsidutils.C64FontUtils.TRUE_TYPE_FONT_BIG;
import static libsidutils.C64FontUtils.petsciiToFont;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.sound.sampled.LineUnavailableException;

import com.xuggle.xuggler.Configuration;
import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec.ID;
import com.xuggle.xuggler.IConfigurable;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IContainerFormat;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IRational;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

import libsidplay.common.CPUClock;
import libsidplay.common.Event.Phase;
import libsidplay.common.EventScheduler;
import libsidplay.common.SamplingRate;
import libsidplay.components.mos656x.MOS6567;
import libsidplay.components.mos656x.VIC;
import libsidplay.config.IAudioSection;
import sidplay.audio.AudioConfig;
import sidplay.audio.AudioDriver;
import sidplay.audio.VideoDriver;
import sidplay.audio.exceptions.IniConfigException;

/**
 * Create video.<BR>
 * 
 * <pre>
 * Video possibilities (file, RTMP and HLS):
 * /home/ken/.jsidplay2/jsidplay2.flv
 * rtmp://localhost/live/test
 * http://localhost:90/hls/test.m3u8	(provided by NGINX RTMP as HLS)
 * </pre>
 * 
 * <B>The Kush Gauge:</B> To find a decent bitrate simply multiply the target
 * pixel count by the frame rate; then multiply the result by a factor of 1, 2
 * or 4, depending on the amount of motion in the video; and then multiply that
 * result by 0.07 to get the bit rate in bps. <br>
 * 
 * <pre>
 * Video Coder Bit Rate:
 * PAL low motion:		192 * 312 * 50,1246 * 1 * 0,07	=   210.186
 * PAL high motion:		192 * 312 * 50,1246 * 4 * 0,07	=   840.746
 * NTSC low motion:		192 * 312 * 59,83 * 1 * 0,07	=   250.884
 * NTSC high motion:	192 * 312 * 59,83 * 4 * 0,07	= 1.003.536
 * </pre>
 * 
 * @author ken
 *
 */
public abstract class XuggleVideoDriver extends XuggleBase implements AudioDriver, VideoDriver {

	private static final int STATUS_TEXT_Y = 10;

	private EventScheduler context;

	private IContainer container;
	private IStreamCoder videoCoder, audioCoder;
	private IConverter converter;
	private BufferedImage vicImage;

	private IntBuffer pictureBuffer;
	private int[] statusTextPixels;
	private int statusTextOffset, statusTextX, statusTextOverflow;
	private long frameNo, framesPerKeyFrames, firstAudioTimeStamp, firstVideoTimeStamp;
	private double ticksPerMicrosecond;
	private int audioDelayInMs;

	private ByteBuffer sampleBuffer;

	@Override
	public void open(IAudioSection audioSection, String recordingFilename, CPUClock cpuClock, EventScheduler context)
			throws IOException, LineUnavailableException, InterruptedException {
		this.context = context;
		AudioConfig cfg = new AudioConfig(audioSection);
		String url = getUrl(audioSection, recordingFilename);
		if (url == null) {
			throw new FileNotFoundException("url is missing, please set option --vcStreamingUrl");
		}

		if (!getSupportedSamplingRates().contains(audioSection.getSamplingRate())) {
			throw new IniConfigException("Sampling rate is not supported by encoder, switch to default",
					() -> audioSection.setSamplingRate(getDefaultSamplingRate()));
		}
		container = IContainer.make();
		IContainerFormat containerFormat = IContainerFormat.make();
		containerFormat.setOutputFormat(getOutputFormatName(), url, null);
		container.setInputBufferLength(0);
		throwExceptionOnError(container.open(url, WRITE, containerFormat), "Could not open: '" + url + "'");

		videoCoder = createVideoCoder(audioSection, cpuClock);
		throwExceptionOnError(videoCoder.open(null, null));

		audioCoder = createAudioCoder(audioSection, cfg);
		throwExceptionOnError(audioCoder.open(null, null));

		container.writeHeader();

		vicImage = new BufferedImage(MAX_WIDTH, MAX_HEIGHT, TYPE_INT_ARGB);
		pictureBuffer = IntBuffer.wrap(((DataBufferInt) vicImage.getRaster().getDataBuffer()).getData());
		converter = ConverterFactory.createConverter(vicImage, YUV420P);

		setStatusText("Recorded by JSIDPlay2!");

		frameNo = 0;
		firstAudioTimeStamp = 0;
		firstVideoTimeStamp = 0;
		statusTextOffset = MAX_WIDTH * ((cpuClock == PAL ? BORDER_HEIGHT : MOS6567.BORDER_HEIGHT) + STATUS_TEXT_Y);
		statusTextX = 0;
		statusTextOverflow = 0;
		audioDelayInMs = audioSection.getVideoCoderAudioDelay();
		ticksPerMicrosecond = cpuClock.getCpuFrequency() / 1000000;
		framesPerKeyFrames = (int) cpuClock.getScreenRefresh();

		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * BYTES * cfg.getChannels()).order(LITTLE_ENDIAN);
	}

	@Override
	public void write() throws InterruptedException {
		long timeStamp = getAudioTimeStamp();

		int numSamples = sampleBuffer.position() >> 2;
		IAudioSamples audioSamples = IAudioSamples.make(numSamples, audioCoder.getChannels(), FMT_S16);
		audioSamples.getData().put(sampleBuffer.array(), 0, 0, sampleBuffer.position());
		audioSamples.setComplete(true, numSamples, audioCoder.getSampleRate(), audioCoder.getChannels(), FMT_S16,
				timeStamp);

		int samplesConsumed = 0;
		IPacket packet = IPacket.make();
		while (samplesConsumed < audioSamples.getNumSamples()) {
			samplesConsumed += throwExceptionOnError(audioCoder.encodeAudio(packet, audioSamples, samplesConsumed));
			if (packet.isComplete()) {
				container.writePacket(packet);
			}
		}
		audioSamples.delete();
		packet.delete();
	}

	@Override
	public void accept(VIC vic) {
		long timeStamp = getVideoTimeStamp();

		pictureBuffer.put(vic.getPixels().array(), 0, statusTextOffset).put(statusTextPixels)
				.put(vic.getPixels().array(), pictureBuffer.position(), pictureBuffer.remaining());
		((Buffer) pictureBuffer).clear();

		IVideoPicture videoPicture = converter.toPicture(vicImage, timeStamp);
		videoPicture.setKeyFrame((frameNo++ % framesPerKeyFrames) == 0);

		IPacket packet = IPacket.make();
		throwExceptionOnError(videoCoder.encodeVideo(packet, videoPicture, 0));
		if (packet.isComplete()) {
			container.writePacket(packet);
		}
		videoPicture.delete();
		packet.delete();
	}

	@Override
	public void close() {
		if (container != null) {
			if (container.isOpened()) {
				container.writeTrailer();
			}
			if (audioCoder != null) {
				if (audioCoder.isOpen()) {
					audioCoder.close();
				}
				audioCoder = null;
			}
			if (videoCoder != null) {
				if (videoCoder.isOpen()) {
					videoCoder.close();
				}
				videoCoder = null;
			}
			if (container.isOpened()) {
				container.close();
			}
			container = null;
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

	public void setStatusText(String text) {
		String fontText = petsciiToFont(text.toUpperCase(Locale.ENGLISH), TRUE_TYPE_FONT_BIG);

		BufferedImage statusImage = new BufferedImage(MAX_WIDTH, c64Font.getSize(), TYPE_INT_ARGB);
		Graphics2D graphics = statusImage.createGraphics();
		graphics.setFont(c64Font);
		graphics.drawString(fontText, -statusTextX, graphics.getFontMetrics(c64Font).getAscent());
		graphics.dispose();

		statusTextOverflow = Math.max(0, c64Font.getSize() * text.length() - statusTextX - MAX_WIDTH);
		statusTextPixels = ((DataBufferInt) statusImage.getRaster().getDataBuffer()).getData();
	}

	public int getStatusTextOverflow() {
		return statusTextOverflow;
	}

	public int getStatusTextX() {
		return statusTextX;
	}

	public void setStatusTextX(int statusTextX) {
		this.statusTextX = statusTextX;
	}

	private IStreamCoder createVideoCoder(IAudioSection audioSection, CPUClock cpuClock) {
		IStreamCoder videoCoder = container.addNewStream(getVideoCodec()).getStreamCoder();
		videoCoder.setNumPicturesInGroupOfPictures(audioSection.getVideoCoderNumPicturesInGroupOfPictures());
		videoCoder.setBitRate(audioSection.getVideoCoderBitRate());
		videoCoder.setBitRateTolerance(audioSection.getVideoCoderBitRateTolerance());
		videoCoder.setTimeBase(IRational.make(1 / cpuClock.getScreenRefresh()));
		videoCoder.setPixelType(YUV420P);
		videoCoder.setHeight(MAX_HEIGHT);
		videoCoder.setWidth(MAX_WIDTH);
		videoCoder.setFlag(FLAG_QSCALE, true);
		videoCoder.setGlobalQuality(audioSection.getVideoCoderGlobalQuality());
		configurePreset(videoCoder, audioSection.getVideoCoderPreset().getPresetName());
		return videoCoder;
	}

	private void configurePreset(IConfigurable configurable, String presetName) {
		try (InputStream is = XuggleVideoDriver.class.getResourceAsStream(presetName)) {
			Properties props = new Properties();
			props.load(is);
			Configuration.configure(props, configurable);
		} catch (IOException | NullPointerException e) {
			throw new RuntimeException("You need preset " + presetName + " in your classpath.");
		}
	}

	private IStreamCoder createAudioCoder(IAudioSection audioSection, AudioConfig cfg) {
		IStreamCoder audioCoder = container.addNewStream(getAudioCodec()).getStreamCoder();
		audioCoder.setChannels(cfg.getChannels());
		audioCoder.setSampleFormat(FMT_S16);
		audioCoder.setBitRate(audioSection.getAudioCoderBitRate());
		audioCoder.setBitRateTolerance(audioSection.getAudioCoderBitRateTolerance());
		audioCoder.setSampleRate(cfg.getFrameRate());
		return audioCoder;
	}

	private long getAudioTimeStamp() {
		long now = context.getTime(Phase.PHI2);
		if (firstAudioTimeStamp == 0) {
			firstAudioTimeStamp = now;
		}
		return (long) ((now - firstAudioTimeStamp) / ticksPerMicrosecond) + (audioDelayInMs * 1000);
	}

	private long getVideoTimeStamp() {
		long now = context.getTime(Phase.PHI2);
		if (firstVideoTimeStamp == 0) {
			firstVideoTimeStamp = now;
		}
		return (long) ((now - firstVideoTimeStamp) / ticksPerMicrosecond);
	}

	protected abstract String getOutputFormatName();

	protected abstract List<SamplingRate> getSupportedSamplingRates();

	protected abstract SamplingRate getDefaultSamplingRate();

	protected abstract ID getVideoCodec();

	protected abstract ID getAudioCodec();

	protected abstract String getUrl(IAudioSection audioSection, String recordingFilename);
}
