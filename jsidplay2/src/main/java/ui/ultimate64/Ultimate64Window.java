package ui.ultimate64;

import static libsidplay.Ultimate64.SocketStreamingCommand.SOCKET_CMD_AUDIOSTREAM_OFF;
import static libsidplay.Ultimate64.SocketStreamingCommand.SOCKET_CMD_AUDIOSTREAM_ON;
import static libsidplay.Ultimate64.SocketStreamingCommand.SOCKET_CMD_VICSTREAM_OFF;
import static libsidplay.Ultimate64.SocketStreamingCommand.SOCKET_CMD_VICSTREAM_ON;
import static sidplay.audio.AudioConfig.getDefaultBufferSize;
import static sidplay.audio.JavaSound.getDeviceInfo;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;

import javax.sound.sampled.LineUnavailableException;

import javafx.animation.Animation;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.util.Duration;
import libsidplay.Ultimate64;
import libsidplay.common.CPUClock;
import libsidplay.common.VICChipModel;
import libsidplay.components.mos656x.IPalette;
import libsidplay.components.mos656x.PALEmulation;
import libsidplay.config.IWhatsSidSection;
import libsidplay.sidtune.SidTune;
import libsidutils.fingerprinting.IFingerprintMatcher;
import libsidutils.fingerprinting.rest.beans.MusicInfoWithConfidenceBean;
import sidplay.Player;
import sidplay.audio.AudioConfig;
import sidplay.audio.JavaSound;
import sidplay.fingerprinting.FingerprintJsonClient;
import sidplay.fingerprinting.WhatsSidSupport;
import ui.common.C64Window;
import ui.common.ImageQueue;
import ui.common.Toast;
import ui.common.converter.NumberToStringConverter;
import ui.entities.config.AudioSection;
import ui.entities.config.EmulationSection;
import ui.entities.config.SidPlay2Section;

public class Ultimate64Window extends C64Window implements Ultimate64 {

	private static final int SCREEN_HEIGHT = 272;
	private static final int SCREEN_WIDTH = 384;

	private static final int FRAME_RATE = 48000;
	private static final int CHANNELS = 2;
	private static final int AUDIO_BUFFER_SIZE = 192;

	private StreamingPlayer audioPlayer = new StreamingPlayer() {

		private DatagramSocket serverSocket;
		private JavaSound javaSound = new JavaSound();
		private Thread whatsSidMatcherThread;

		@Override
		protected void open() throws IOException, LineUnavailableException, InterruptedException {
			final AudioSection audioSection = util.getConfig().getAudioSection();
			final EmulationSection emulationSection = util.getConfig().getEmulationSection();
			final IWhatsSidSection whatsSidSection = util.getConfig().getWhatsSidSection();

			String url = whatsSidSection.getUrl();
			String username = whatsSidSection.getUsername();
			String password = whatsSidSection.getPassword();
			int connectionTimeout = whatsSidSection.getConnectionTimeout();

			AudioConfig audioConfig = new AudioConfig(FRAME_RATE, CHANNELS, audioBufferSize.getValue());
			javaSound.open(audioConfig, getDeviceInfo(audioSection));

			whatsSidEnabled = whatsSidSection.isEnable();
			whatsSidSupport = new WhatsSidSupport(FRAME_RATE, whatsSidSection.getCaptureTime(),
					whatsSidSection.getMinimumRelativeConfidence());
			whatsSidSupport.reset();
			fingerPrintMatcher = new FingerprintJsonClient(url, username, password, connectionTimeout);

			serverSocket = new DatagramSocket(emulationSection.getUltimate64StreamingAudioPort());
			serverSocket.setSoTimeout(SOCKET_CONNECT_TIMEOUT);
			startStreaming(emulationSection, SOCKET_CMD_AUDIOSTREAM_ON, emulationSection.getUltimate64StreamingTarget()
					+ ":" + emulationSection.getUltimate64StreamingAudioPort(), 0);
		}

		@Override
		protected void play() throws IOException, InterruptedException {
			IWhatsSidSection whatsSidSection = util.getConfig().getWhatsSidSection();

			byte[] receiveData = new byte[2 + (AUDIO_BUFFER_SIZE << 2)];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
			serverSocket.receive(receivePacket);
//			int sequenceNo = ((receivePacket.getData()[1] & 0xff) << 8) | (receivePacket.getData()[0] & 0xff);
			/* left ch, right ch (16 bits each) */
			ShortBuffer shortBuffer = ByteBuffer.wrap(receiveData, 2, receiveData.length - 2)
					.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
			while (shortBuffer.hasRemaining()) {
				short valL = shortBuffer.get();
				short valR = shortBuffer.get();
				javaSound.buffer().putShort(valL);
				if (!javaSound.buffer().putShort(valR).hasRemaining()) {
					javaSound.write();
					((Buffer) javaSound.buffer()).clear();
				}
				if (whatsSidEnabled) {
					if (whatsSidSupport.output(valL, valR)) {
						matchTune(whatsSidSection);
					}
				}
			}
		}

		private void matchTune(IWhatsSidSection whatsSidSection) {
			if (whatsSidMatcherThread == null || !whatsSidMatcherThread.isAlive()) {
				whatsSidMatcherThread = new Thread(() -> {
					try {
						MusicInfoWithConfidenceBean result = whatsSidSupport.match(fingerPrintMatcher);
						if (result != null) {
							Platform.runLater(() -> {
								System.out.println(result);
								Toast.makeText("whatssid", whatsSidPositioner, result.toString(), 5);
							});
						}
					} catch (Exception e) {
						// server not available? silently ignore!
					}
				});
				whatsSidMatcherThread.setPriority(Thread.MIN_PRIORITY);
				whatsSidMatcherThread.start();
			}
		}

		@Override
		protected void close() {
			EmulationSection emulationSection = util.getConfig().getEmulationSection();

			stopStreaming(emulationSection, SOCKET_CMD_AUDIOSTREAM_OFF);
			javaSound.close();
			if (serverSocket != null) {
				serverSocket.close();
			}
			audioStreaming.setSelected(false);
			whatsSidMatcherThread = null;
		}
	};

	private StreamingPlayer videoPlayer = new StreamingPlayer() {
		private DatagramSocket serverSocket;
		private WritableImage image;
		private boolean frameStart;

		@Override
		protected void open() throws IOException, LineUnavailableException {
			SidPlay2Section sidplay2Section = util.getConfig().getSidplay2Section();
			EmulationSection emulationSection = util.getConfig().getEmulationSection();

			palEmulation = new PALEmulation(VICChipModel.MOS6567R8);
			palEmulation.setPalEmulationEnable(enablePalEmulation.isSelected());
			IPalette palette = palEmulation.getPalette();
			palette.setBrightness(sidplay2Section.getBrightness());
			palette.setContrast(sidplay2Section.getContrast());
			palette.setGamma(sidplay2Section.getGamma());
			palette.setSaturation(sidplay2Section.getSaturation());
			palette.setPhaseShift(sidplay2Section.getPhaseShift());
			palette.setOffset(sidplay2Section.getOffset());
			palette.setTint(sidplay2Section.getTint());
			palette.setLuminanceC(sidplay2Section.getBlur());
			palette.setDotCreep(sidplay2Section.getBleed());
			palEmulation.updatePalette();

			serverSocket = new DatagramSocket(emulationSection.getUltimate64StreamingVideoPort());
			serverSocket.setSoTimeout(SOCKET_CONNECT_TIMEOUT);
			image = new WritableImage(SCREEN_WIDTH, SCREEN_HEIGHT);
			startStreaming(emulationSection, SOCKET_CMD_VICSTREAM_ON, emulationSection.getUltimate64StreamingTarget()
					+ ":" + emulationSection.getUltimate64StreamingVideoPort(), 0);
			frameStart = false;
		}

		@Override
		protected void play() throws IOException, InterruptedException {
			byte[] receiveData = new byte[780];
			DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);

			serverSocket.receive(receivePacket);
//			int sequenceNo = ((receivePacket.getData()[1] & 0xff) << 8) | (receivePacket.getData()[0] & 0xff);
//			int frameNo = ((receivePacket.getData()[3] & 0xff) << 8) | (receivePacket.getData()[2] & 0xff);
			int lineNo = (receivePacket.getData()[5] & 0xff) << 8 | receivePacket.getData()[4] & 0xff;
			boolean isLastPacketOfFrame = (lineNo & 1 << 15) != 0;
			lineNo &= lineNo & ~(1 << 15);
			int pixelsPerLine = (receivePacket.getData()[7] & 0xff) << 8 | receivePacket.getData()[6] & 0xff;
			int linesPerPacket = receivePacket.getData()[8] & 0xff;
			int bitsPerPixel = receivePacket.getData()[9] & 0xff;
			int encodingType = (receivePacket.getData()[11] & 0xff) << 8 | receivePacket.getData()[10] & 0xff;
			byte[] pixelData = new byte[linesPerPacket * pixelsPerLine * bitsPerPixel / 8];
			System.arraycopy(receivePacket.getData(), 12, pixelData, 0, pixelData.length);
			assert pixelsPerLine == SCREEN_WIDTH;
			assert linesPerPacket == 4;
			assert bitsPerPixel == 4;
			assert encodingType == 0; // or later 1 for RLE?

			if (frameStart) {
				palEmulation.reset();
				int graphicsDataBuffer = 0;
				int pixelDataOffset = 0;
				for (int y = 0; y < linesPerPacket; y++) {
					int rasterY = lineNo + y;
					palEmulation.determineCurrentPalette(rasterY, rasterY == 0);

					for (int x = 0; x < pixelsPerLine; x++) {
						graphicsDataBuffer <<= 4;
						graphicsDataBuffer |= pixelData[pixelDataOffset + x >> 1] >> ((x & 1) << 2) & 0xf;
						if ((x + 1 & 0x7) == 0) {
							palEmulation.drawPixels(graphicsDataBuffer);
						}
					}
					pixelDataOffset += pixelsPerLine;
				}
				image.getPixelWriter().setPixels(0, lineNo, pixelsPerLine, linesPerPacket,
						PixelFormat.getByteBgraPreInstance(), palEmulation.getPixels().array(), 0, pixelsPerLine << 2);

				if (isLastPacketOfFrame) {
					imageQueue.push(copyImage());
				}
			}
			if (isLastPacketOfFrame) {
				frameStart = true;
			}
		}

		private WritableImage copyImage() {
			PixelReader pixelReader = image.getPixelReader();
			int width = (int) image.getWidth();
			int height = (int) image.getHeight();

			WritableImage writableImage = new WritableImage(width, height);
			PixelWriter pixelWriter = writableImage.getPixelWriter();

			for (int y = 0; y < height; y++) {
				for (int x = 0; x < width; x++) {
					pixelWriter.setColor(x, y, pixelReader.getColor(x, y));
				}
			}
			return writableImage;
		}

		@Override
		protected void close() {
			EmulationSection emulationSection = util.getConfig().getEmulationSection();

			imageQueue.clear();
			stopStreaming(emulationSection, SOCKET_CMD_VICSTREAM_OFF);
			if (serverSocket != null) {
				serverSocket.close();
			}
			videoStreaming.setSelected(false);
		}

	};

	@FXML
	private ImageView screen;

	@FXML
	private ToggleButton audioStreaming, videoStreaming;

	@FXML
	private ComboBox<Integer> audioBufferSize;

	@FXML
	private CheckBox enablePalEmulation;

	@FXML
	protected Label whatsSidPositioner;

	@FXML
	private Slider scaling;

	@FXML
	private Label scalingValue;

	private boolean whatsSidEnabled;
	private WhatsSidSupport whatsSidSupport;
	private IFingerprintMatcher fingerPrintMatcher;

	private PALEmulation palEmulation;

	private ImageQueue<Image> imageQueue;

	private PauseTransition pauseTransition;
	private SequentialTransition sequentialTransition;

	public Ultimate64Window() {
		super();
	}

	public Ultimate64Window(Player player) {
		super(player);
	}

	@FXML
	@Override
	protected void initialize() {
		SidPlay2Section sidplay2Section = util.getConfig().getSidplay2Section();
		EmulationSection emulationSection = util.getConfig().getEmulationSection();

		scaling.setLabelFormatter(new NumberToStringConverter<>(2));
		scaling.valueProperty().bindBidirectional(sidplay2Section.videoScalingProperty());
		scalingValue.textProperty().bindBidirectional(sidplay2Section.videoScalingProperty(),
				new NumberToStringConverter<>(2));
		scaling.valueProperty()
				.addListener((observable, oldValue, newValue) -> updateScaling(sidplay2Section.getVideoScaling()));
		updateScaling(sidplay2Section.getVideoScaling());

		// TODO configure values
		audioBufferSize.setValue(getDefaultBufferSize());
		enablePalEmulation.setSelected(true);

		pauseTransition = new PauseTransition();
		sequentialTransition = new SequentialTransition(pauseTransition);
		pauseTransition.setOnFinished(evt -> {
			Image image = imageQueue.pull();
			if (image != null) {
				screen.setImage(image);
			}
		});
		sequentialTransition.setCycleCount(Animation.INDEFINITE);

		imageQueue = new ImageQueue<>();

		SidTune tune = util.getPlayer().getTune();
		setupVideoScreen(CPUClock.getCPUClock(emulationSection, tune));

		sequentialTransition.playFromStart();
	}

	private void updateScaling(double scale) {
		screen.setScaleX(scale);
		screen.setScaleY(scale);
	}

	@FXML
	private void enableDisableAudioStreaming() {
		if (audioStreaming.isSelected()) {
			audioPlayer.start();
		} else {
			audioPlayer.stop();
		}
	}

	@FXML
	private void enableDisableVideoStreaming() {
		if (videoStreaming.isSelected()) {
			videoPlayer.start();
		} else {
			videoPlayer.stop();
		}
	}

	@FXML
	private void setAudioBufferSize() {
		if (audioStreaming.isSelected()) {
			audioPlayer.stop();
			audioPlayer.start();
			audioStreaming.setSelected(true);
		}
	}

	@FXML
	private void setEnablePalEmulation() {
		palEmulation.setPalEmulationEnable(enablePalEmulation.isSelected());
	}

	/**
	 * Connect VIC output with screen.
	 */
	private void setupVideoScreen(final CPUClock cpuClock) {
		pauseTransition.setDuration(Duration.millis(1000. / cpuClock.getScreenRefresh()));

		screen.setFitWidth(SCREEN_WIDTH);
		screen.setFitHeight(SCREEN_HEIGHT);
	}

	@Override
	public void doClose() {
		audioPlayer.stop();
		videoPlayer.stop();
	}
}
