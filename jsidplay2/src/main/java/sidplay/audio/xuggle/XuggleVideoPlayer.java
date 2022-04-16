package sidplay.audio.xuggle;

import static com.xuggle.xuggler.IContainer.Type.READ;
import static sidplay.player.State.PAUSE;
import static sidplay.player.State.PLAY;
import static sidplay.player.State.QUIT;

import java.awt.image.BufferedImage;
import java.io.IOException;

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

public abstract class XuggleVideoPlayer extends XuggleVideoBase implements Runnable {

	protected ObjectProperty<State> stateProperty = new ObjectProperty<>(State.class.getSimpleName(), QUIT);

	private IContainer container;
	private IStreamCoder videoCoder, audioCoder;
	private int videoStreamId, audioStreamId;
	private IConverter converter;

	public VideoInfo open(String filename) throws IOException {
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

		return new VideoInfo(audioCoder, videoCoder);
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
		System.out.println("Number of streams: " + container.getNumStreams());
		System.out.println("Start time: " + container.getStartTime());
		System.out.println("Duration (ms): " + container.getDuration());
		System.out.println("File Size (bytes): " + container.getFileSize());
		System.out.println("Bit Rate: " + (long) container.getBitRate());

		if (videoStreamId != -1) {
			IStream videoStream = container.getStream(videoStreamId);
			System.out.println("*** Start of Video Stream Info ***");
			System.out.printf("type: %s; ", videoCoder.getCodecType());
			System.out.printf("codec: %s; ", videoCoder.getCodecID());
			System.out.printf("duration: %s; ", videoStream.getDuration());
			System.out.printf("timebase: %d/%d; ", videoStream.getTimeBase().getNumerator(),
					videoStream.getTimeBase().getDenominator());
			System.out.printf("coder timebase: %d/%d; ", videoCoder.getTimeBase().getNumerator(),
					videoCoder.getTimeBase().getDenominator());
			System.out.println();
		}

		if (audioStreamId != -1) {
			IStream audioStream = container.getStream(audioStreamId);
			System.out.println("*** Start of Audio Stream Info ***");
			System.out.printf("type: %s; ", audioCoder.getCodecType());
			System.out.printf("codec: %s; ", audioCoder.getCodecID());
			System.out.printf("sample rate: %d; ", audioCoder.getSampleRate());
			System.out.printf("channels: %d; ", audioCoder.getChannels());
			System.out.printf("duration: %s; ", audioStream.getDuration());
			System.out.printf("timebase: %d/%d; ", audioStream.getTimeBase().getNumerator(),
					audioStream.getTimeBase().getDenominator());
			System.out.printf("coder timebase: %d/%d; ", audioCoder.getTimeBase().getNumerator(),
					audioCoder.getTimeBase().getDenominator());
			System.out.println();
		}
	}

	protected abstract void write(byte[] samples) throws InterruptedException;

	protected abstract void write(BufferedImage image);

}
