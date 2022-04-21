package sidplay.audio.xuggle;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import com.xuggle.xuggler.IContainer;
import com.xuggle.xuggler.IError;
import com.xuggle.xuggler.IPixelFormat;
import com.xuggle.xuggler.IStreamCoder;
import com.xuggle.xuggler.video.ArgbConverter;
import com.xuggle.xuggler.video.ConverterFactory;

import libsidutils.C64Font;

public class XuggleVideoBase implements C64Font {

	public static class VideoInfo {

		private long duration;

		private int sampleRate, channels;

		private int width, height;
		private double frameRate;

		public VideoInfo(IContainer container, IStreamCoder audioCoder, IStreamCoder videoCoder) {
			duration = container.getDuration();
			if (audioCoder != null) {
				this.sampleRate = audioCoder.getSampleRate();
				this.channels = audioCoder.getChannels();
			}
			if (videoCoder != null) {
				this.width = videoCoder.getWidth();
				this.height = videoCoder.getHeight();
				this.frameRate = videoCoder.getFrameRate().getValue();
			}
		}

		public long getDuration() {
			return duration;
		}

		public int getSampleRate() {
			return sampleRate;
		}

		public int getChannels() {
			return channels;
		}

		public int getWidth() {
			return width;
		}

		public int getHeight() {
			return height;
		}

		public double getFrameRate() {
			return frameRate;
		}
	}

	private static final int FONT_SIZE = 8;

	protected static final Font c64Font;

	static {
		try {
			InputStream fontStream = XuggleVideoDriver.class.getResourceAsStream(FONT_NAME);
			if (fontStream == null) {
				throw new IOException("Font not found: " + FONT_NAME);
			}
			GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
			c64Font = Font.createFont(Font.TRUETYPE_FONT, fontStream).deriveFont((float) FONT_SIZE);
			graphicsEnvironment.registerFont(c64Font);

			ConverterFactory.registerConverter(new ConverterFactory.Type(ConverterFactory.XUGGLER_ARGB_32,
					ArgbConverter.class, IPixelFormat.Type.ARGB, BufferedImage.TYPE_INT_ARGB));
		} catch (IOException | FontFormatException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	public void throwExceptionOnError(int rc, String message) throws IOException {
		if (rc < 0) {
			System.err.println(message);
		}
		throwExceptionOnError(rc);
	}

	public void throwExceptionOnError(int rc) throws IOException {
		if (rc < 0) {
			throw new IOException(IError.make(rc).getDescription());
		}
	}

}
