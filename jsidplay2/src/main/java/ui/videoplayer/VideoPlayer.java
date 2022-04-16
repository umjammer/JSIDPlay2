package ui.videoplayer;

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javafx.animation.Animation;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TextField;
import javafx.scene.control.TitledPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.stage.DirectoryChooser;
import javafx.util.Duration;
import sidplay.Player;
import sidplay.audio.AudioConfig;
import sidplay.audio.JavaSound;
import sidplay.audio.xuggle.XuggleVideoPlayer;
import sidplay.player.State;
import ui.common.C64VBox;
import ui.common.C64Window;
import ui.common.ImageQueue;
import ui.common.UIPart;
import ui.entities.config.AudioSection;
import ui.entities.config.SidPlay2Section;

public class VideoPlayer extends C64VBox implements UIPart {

	public static final String ID = "VIDEOPLAYER";

	private class StateChangeListener implements PropertyChangeListener {
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			if ((event.getNewValue() == State.END || event.getNewValue() == State.QUIT)
					&& util.getPlayer().getAudioDriver().isRecording()) {
				Path sourcePath = Paths.get(util.getPlayer().getRecordingFilename());
				filenameField.setText(sourcePath.toFile().getAbsolutePath());
			}
		}
	}

	@FXML
	private TextField filenameField;

	@FXML
	private ToggleButton recordingPauseContinue;

	@FXML
	private TitledPane monitor;

	@FXML
	private Canvas screen;

	private ImageQueue<Image> imageQueue;

	private PauseTransition pauseTransition;
	private SequentialTransition sequentialTransition;

	private JavaSound javaSound;

	private Thread thread;

	private XuggleVideoPlayer playerRunnable;

	private StateChangeListener propertyChangeListener;

	public VideoPlayer() {
		super();
	}

	public VideoPlayer(C64Window window, Player player) {
		super(window, player);
	}

	@FXML
	@Override
	protected void initialize() {
		pauseTransition = new PauseTransition();
		sequentialTransition = new SequentialTransition(pauseTransition);
		pauseTransition.setOnFinished(evt -> {
			Image image = imageQueue.pull();
			if (image != null) {
				// memory leak prevention!?
				// https://github.com/kasemir/org.csstudio.display.builder/issues/174
				screen.getGraphicsContext2D().clearRect(0, 0, screen.getWidth(), screen.getHeight());
				screen.getGraphicsContext2D().drawImage(image, 0, 0);
			}
		});
		sequentialTransition.setCycleCount(Animation.INDEFINITE);

		imageQueue = new ImageQueue<>();

		Path sourcePath = Paths.get(util.getPlayer().getRecordingFilename());
		filenameField.setText(sourcePath.toFile().getAbsolutePath());
		propertyChangeListener = new StateChangeListener();
		util.getPlayer().stateProperty().addListener(propertyChangeListener);

		javaSound = new JavaSound();
		playerRunnable = new XuggleVideoPlayer() {

			@Override
			public void run() {
				try {
					SidPlay2Section sidplay2Section = util.getConfig().getSidplay2Section();
					AudioSection audioSection = util.getConfig().getAudioSection();

					VideoInfo videoInfo = super.open(filenameField.getText());

					Platform.runLater(() -> {
						screen.getGraphicsContext2D().clearRect(0, 0, videoInfo.getWidth(), videoInfo.getHeight());
						screen.setWidth(videoInfo.getWidth());
						screen.setHeight(videoInfo.getHeight());

						double scale = sidplay2Section.getVideoScaling();
						screen.setScaleX(scale);
						screen.setScaleY(scale);
						pauseTransition.setDuration(Duration.millis(1000. / videoInfo.getFrameRate()));
						sequentialTransition.playFromStart();
					});

					javaSound.open(new AudioConfig(videoInfo.getSampleRate(), videoInfo.getChannels(),
							audioSection.getAudioBufferSize()), JavaSound.getDeviceInfo(audioSection));

					super.run();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					super.close();
					sequentialTransition.stop();
					imageQueue.clear();
					javaSound.close();
					screen.getGraphicsContext2D().clearRect(0, 0, screen.getWidth(), screen.getHeight());
					recordingPauseContinue.setSelected(false);
					thread = null;
				}
			}

			@Override
			protected void write(BufferedImage image) {
				imageQueue.push(SwingFXUtils.toFXImage(image, null));
			}

			@Override
			protected void write(byte[] samples) throws InterruptedException {
				for (byte b : samples) {
					if (!javaSound.buffer().put(b).hasRemaining()) {
						javaSound.write();
						((Buffer) javaSound.buffer()).clear();
					}
				}
			}

			@Override
			public void pauseContinue() {
				super.pauseContinue();
				if (javaSound != null) {
					javaSound.pause();
				}
			}
		};
	}

	@Override
	public void doClose() {
		util.getPlayer().stateProperty().removeListener(propertyChangeListener);
		stop();
		imageQueue.dispose();

	}

	@FXML
	private void pauseContinue() {
		if (thread != null) {
			playerRunnable.pauseContinue();
		} else {
			thread = new Thread(playerRunnable);
			thread.start();
		}
	}

	@FXML
	public void stop() {
		try {
			if (thread != null) {
				playerRunnable.terminate();
				thread.join();
			}
			recordingPauseContinue.setSelected(false);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@FXML
	public void export() {
		try {
			SidPlay2Section sidplay2Section = util.getConfig().getSidplay2Section();
			final DirectoryChooser fileDialog = new DirectoryChooser();
			fileDialog.setTitle(util.getBundle().getString("SAVE_RECORDING"));
			fileDialog.setInitialDirectory(sidplay2Section.getLastDirectory());
			File directory = fileDialog.showDialog(getScene().getWindow());
			if (directory != null) {
				sidplay2Section.setLastDirectory(directory);

				Path sourcePath = Paths.get(util.getPlayer().getRecordingFilename());
				Path targetPath = new File(directory, sourcePath.toFile().getName()).toPath();
				Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
				sourcePath.toFile().deleteOnExit();
				System.out.println("Recording Saved to: " + targetPath);
			}
		} catch (IOException e) {
			openErrorDialog(e.getMessage());
		}
	}

	private void openErrorDialog(String msg) {
		Alert alert = new Alert(AlertType.ERROR, msg);
		alert.setTitle(util.getBundle().getString("ALERT_TITLE"));
		alert.showAndWait();
	}

}
