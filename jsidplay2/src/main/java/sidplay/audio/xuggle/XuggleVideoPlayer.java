package sidplay.audio.xuggle;

import static com.xuggle.xuggler.IContainer.Type.READ;
import static java.lang.String.format;
import static sidplay.player.State.PAUSE;
import static sidplay.player.State.PLAY;
import static sidplay.player.State.QUIT;

import java.awt.image.BufferedImage;
import java.util.logging.Logger;

import com.xuggle.xuggler.IAudioSamples;
import com.xuggle.xuggler.ICodec;
import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IPacket;
import com.xuggle.xuggler.IStream;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.IVideoPicture;
import com.xuggle.xuggler.video.ConverterFactory;
import com.xuggle.xuggler.video.IConverter;

import sidplay.player.ObjectProperty;
import sidplay.player.State;

public abstract class XuggleVideoPlayer extends XuggleBase implements Runnable {

	private static final Logger LOGGER = Logger.getLogger(XuggleVideoPlayer.class.getName());

	protected ObjectProperty<State> stateProperty = new ObjectProperty<>(State.class.getSimpleName(), QUIT);

	private IContainer container;
	private IStreamCoder videoCoder, audioCoder;
	private int videoStreamId, audioStreamId;
	private IConverter converter;

	public VideoInfo open(String filename) {
		stateProperty.set(State.OPEN);
		container = IContainer.make();
		throwExceptionOnError(container.open(filename, READ, null), "Could not open: '" + filename + "'");

		videoStreamId = -1;
		audioStreamId = -1;
		for (int streamIdx = 0; streamIdx < container.getNumStreams(); streamIdx++) {
			IStreamCoder coder = container.getStream(streamIdx).getStreamCoder();

			if (videoStreamId == -1 && coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO) {
				videoStreamId = streamIdx;
				videoCoder = coder;
			} else if (audioStreamId == -1 && coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO) {
				audioStreamId = streamIdx;
				audioCoder = coder;
			}
		}
		if (videoStreamId == -1 && audioStreamId == -1) {
			throw new RuntimeException("could not find audio or video stream in container: " + filename);
		}

		if (videoCoder != null) {
			throwExceptionOnError(videoCoder.open(null, null));
		}
		if (audioCoder != null) {
			throwExceptionOnError(audioCoder.open(null, null));
		}

		dump();

		stateProperty.set(State.START);

		return new VideoInfo(container, audioCoder, videoCoder);
	}

	@Override
	public void run() {
		stateProperty.set(State.PLAY);
		try {
			IPacket packet = IPacket.make();
			while (container.readNextPacket(packet) >= 0
					&& (stateProperty.get() == PLAY || stateProperty.get() == PAUSE)) {
				while (stateProperty.get() == PAUSE) {
					Thread.yield();
				}

				if (videoStreamId != -1 && packet.getStreamIndex() == videoStreamId) {
					decodeVideoPacket(packet);

				} else if (audioStreamId != -1 && packet.getStreamIndex() == audioStreamId) {
					decodeAudioPacket(packet);

				}
			}
			packet.delete();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (stateProperty.get() == PLAY) {
			stateProperty.set(State.END);
		}
	}

	private void decodeAudioPacket(IPacket packet) throws InterruptedException {
		IAudioSamples samples = IAudioSamples.make(1024, audioCoder.getChannels());

		int offset = 0;
		while (offset < packet.getSize()) {
			int bytesDecoded = audioCoder.decodeAudio(samples, packet, offset);
			if (bytesDecoded < 0) {
				break;
			}
			offset += bytesDecoded;
			if (samples.isComplete()) {
				write(samples.getData().getByteArray(0, samples.getSize()));
			}
		}
	}

	private void decodeVideoPacket(IPacket packet) {
		IVideoPicture picture = IVideoPicture.make(videoCoder.getPixelType(), videoCoder.getWidth(),
				videoCoder.getHeight());

		int bytesDecoded = videoCoder.decodeVideo(picture, packet, 0);
		if (bytesDecoded < 0) {
			return;
		}
		if (picture.isComplete()) {
			if (converter == null) {
				converter = ConverterFactory.createConverter(ConverterFactory.XUGGLER_BGR_24, picture);
			}
			write(converter.toImage(picture));
		}
	}

	public void pauseContinue() {
		if (stateProperty.get() == PLAY) {
			stateProperty.set(PAUSE);
		} else {
			stateProperty.set(PLAY);
		}
	}

	public void terminate() {
		stateProperty.set(QUIT);
	}

	public void close() {
		converter = null;
		if (videoCoder != null) {
			videoCoder.close();
			videoCoder = null;
		}
		if (audioCoder != null) {
			audioCoder.close();
			audioCoder = null;
		}
		if (container != null) {
			container.close();
			container = null;
		}
	}

	private void dump() {
		LOGGER.info(format("*** Start of Video Info ***"));
		StringBuilder containerInfo = new StringBuilder();
		containerInfo.append(format("Number of streams: %d; ", container.getNumStreams()));
		containerInfo.append(format("Start time: %d; ", container.getStartTime()));
		containerInfo.append(format("Duration: %dus; ", container.getDuration()));
		containerInfo.append(format("File Size: %d bytes; ", container.getFileSize()));
		containerInfo.append(format("Bit Rate: %d bytes/sec; ", container.getBitRate()));
		LOGGER.info(containerInfo.toString());

		if (videoStreamId != -1) {
			IStream videoStream = container.getStream(videoStreamId);
			StringBuilder videoInfo = new StringBuilder();
			videoInfo.append(format("type: %s; ", videoCoder.getCodecType()));
			videoInfo.append(format("codec: %s; ", videoCoder.getCodecID()));
			videoInfo.append(format("duration: %sus; ", videoStream.getDuration()));
			videoInfo.append(format("timebase: %d/%d; ", videoStream.getTimeBase().getNumerator(),
					videoStream.getTimeBase().getDenominator()));
			videoInfo.append(format("video timebase: %d/%d; ", videoCoder.getTimeBase().getNumerator(),
					videoCoder.getTimeBase().getDenominator()));
			videoInfo.append(format("frame rate: %f; ", videoStream.getFrameRate().getDouble()));
			videoInfo.append(format("frames: %f; ",
					(videoStream.getFrameRate().getDouble() * container.getDuration() / 1000000.)));
			LOGGER.info(videoInfo.toString());
		}

		if (audioStreamId != -1) {
			IStream audioStream = container.getStream(audioStreamId);
			StringBuilder audioInfo = new StringBuilder();
			audioInfo.append(format("type: %s; ", audioCoder.getCodecType()));
			audioInfo.append(format("codec: %s; ", audioCoder.getCodecID()));
			audioInfo.append(format("duration: %sus; ", audioStream.getDuration()));
			audioInfo.append(format("audio timebase: %d/%d; ", audioStream.getTimeBase().getNumerator(),
					audioStream.getTimeBase().getDenominator()));
			audioInfo.append(format("coder timebase: %d/%d; ", audioCoder.getTimeBase().getNumerator(),
					audioCoder.getTimeBase().getDenominator()));
			audioInfo.append(format("sample rate: %d; ", audioCoder.getSampleRate()));
			audioInfo
					.append(format("samples: %f; ", (audioCoder.getSampleRate() * container.getDuration() / 1000000.)));
			audioInfo.append(format("channels: %d; ", audioCoder.getChannels()));
			LOGGER.info(audioInfo.toString());
		}
	}

	protected abstract void write(byte[] samples) throws InterruptedException;

	protected abstract void write(BufferedImage image);

}
