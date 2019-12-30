package sidplay.audio;

import static org.monte.media.AudioFormatKeys.ChannelsKey;
import static org.monte.media.AudioFormatKeys.ENCODING_PCM_SIGNED;
import static org.monte.media.AudioFormatKeys.SampleRateKey;
import static org.monte.media.AudioFormatKeys.SampleSizeInBitsKey;
import static org.monte.media.FormatKeys.EncodingKey;
import static org.monte.media.FormatKeys.FrameRateKey;
import static org.monte.media.FormatKeys.MediaTypeKey;
import static org.monte.media.VideoFormatKeys.DepthKey;
import static org.monte.media.VideoFormatKeys.ENCODING_AVI_MJPG;
import static org.monte.media.VideoFormatKeys.HeightKey;
import static org.monte.media.VideoFormatKeys.QualityKey;
import static org.monte.media.VideoFormatKeys.WidthKey;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.LineUnavailableException;

import org.monte.media.Format;
import org.monte.media.FormatKeys.MediaType;
import org.monte.media.avi.AVIWriter;
import org.monte.media.math.Rational;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

import libsidplay.common.CPUClock;
import libsidplay.components.mos656x.VIC;

@Parameters(resourceBundle = "sidplay.audio.AVIDriver")
public class AVIDriver implements VideoDriver {

	private AVIWriter aviWriter;

	private ByteBuffer sampleBuffer;
	private BufferedImage videoImage;

	/**
	 * AVI: compression quality (1=best, 0.97=default, 0=worst)
	 */
	@Parameter(names = { "--aviVideoQuality" }, descriptionKey = "AVI_VIDEO_QUALITY")
	private float aviVideoQuality = 0.97f;

	@Override
	public void open(AudioConfig cfg, String recordingFilename, CPUClock cpuClock)
			throws IOException, LineUnavailableException {
		System.out.println("Recording, file=" + recordingFilename);
		aviWriter = new AVIWriter(new File(recordingFilename));

		aviWriter.addTrack(new Format(EncodingKey, ENCODING_AVI_MJPG, DepthKey, 24, QualityKey, 1f, MediaTypeKey,
				MediaType.VIDEO, FrameRateKey, new Rational((long) cpuClock.getScreenRefresh(), 1), WidthKey,
				VIC.MAX_WIDTH, HeightKey, VIC.MAX_HEIGHT));

		aviWriter.addTrack(new Format(SampleRateKey, new Rational(cfg.getFrameRate(), 1), ChannelsKey,
				cfg.getChannels(), SampleSizeInBitsKey, Short.SIZE, EncodingKey, ENCODING_PCM_SIGNED));

		videoImage = new BufferedImage(VIC.MAX_WIDTH, VIC.MAX_HEIGHT, BufferedImage.TYPE_INT_RGB);
		aviWriter.setPalette(0, videoImage.getColorModel());
		aviWriter.setCompressionQuality(0, aviVideoQuality);

		sampleBuffer = ByteBuffer.allocate(cfg.getChunkFrames() * Short.BYTES * cfg.getChannels())
				.order(ByteOrder.LITTLE_ENDIAN);
	}

	@Override
	public void accept(VIC vic, int[] bgraData) {
		try {
			videoImage.setRGB(0, 0, VIC.MAX_WIDTH, VIC.MAX_HEIGHT, bgraData, 0, VIC.MAX_WIDTH);
			aviWriter.write(0, videoImage, 1);
		} catch (IOException e) {
			throw new RuntimeException("Error writing AVI video stream", e);
		}
	}

	@Override
	public void write() throws InterruptedException {
		try {
			if (aviWriter.isDataLimitReached()) {
				throw new IOException("AVI file size limit reached!");
			}
			aviWriter.writeSamples(1, sampleBuffer.position() >> 2/* / (Short.BYTES * cfg.getChannels()) */,
					sampleBuffer.array(), 0, sampleBuffer.position(), true);
		} catch (IOException e) {
			throw new RuntimeException("Error writing AVI video stream", e);
		}
	}

	@Override
	public void close() {
		try {
			if (aviWriter != null) {
				aviWriter.close();
			}
		} catch (IOException e) {
			throw new RuntimeException("Error closing AVI", e);
		}
	}

	@Override
	public boolean isRecording() {
		return true;
	}

	@Override
	public String getExtension() {
		return ".avi";
	}
	
	@Override
	public ByteBuffer buffer() {
		return sampleBuffer;
	}

}
