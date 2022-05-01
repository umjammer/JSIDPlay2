package server.restful.common;

import static server.restful.common.IServletSystemProperties.RTMP_EXCEEDS_MAXIMUM_DURATION;
import static server.restful.common.IServletSystemProperties.RTMP_NOT_YET_PLAYED_TIMEOUT;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Event;
import libsidplay.components.keyboard.KeyTableEntry;
import libsidutils.status.Status;
import net.java.truevfs.access.TArchiveDetector;
import net.java.truevfs.access.TFile;
import sidplay.Player;
import sidplay.audio.AudioDriver;
import sidplay.audio.ProxyDriver;
import sidplay.audio.xuggle.XuggleVideoDriver;
import sidplay.player.State;
import ui.common.filefilter.DiskFileFilter;

public final class PlayerWithStatus {

	private static final DiskFileFilter DISK_FILE_FILTER = new DiskFileFilter();

	private final Player player;

	private final Status status;

	private File diskImage;

	private final LocalDateTime created;

	private LocalDateTime validUntil;

	private int playCounter, statusScrollCounter;

	public PlayerWithStatus(Player player, File diskImage, ResourceBundle resourceBundle) {
		this.player = player;
		this.diskImage = diskImage;
		status = new Status(player, resourceBundle);
		created = LocalDateTime.now();
		validUntil = created.plusSeconds(RTMP_NOT_YET_PLAYED_TIMEOUT);
		addStatusTextListener();
	}

	public void onPlay() {
		++playCounter;
		validUntil = created.plusSeconds(RTMP_EXCEEDS_MAXIMUM_DURATION);
	}

	public void onPlayDone() {
		if (--playCounter == 0) {
			validUntil = LocalDateTime.now().plusSeconds(RTMP_NOT_YET_PLAYED_TIMEOUT);
		}
	}

	public boolean isInvalid() {
		return validUntil.isBefore(LocalDateTime.now());
	}

	public LocalDateTime getValidUntil() {
		return validUntil;
	}

	public void quitPlayer() {
		player.quit();
	}

	public File insertNextDisk() {
		try {
			setNextDiskImage();
			player.insertDisk(extract());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return diskImage;
	}

	public void setDefaultSidModel6581() {
		player.getConfig().getEmulationSection().setDefaultSidModel(ChipModel.MOS6581);
		player.updateSIDChipConfiguration();
	}

	public void setDefaultSidModel8580() {
		player.getConfig().getEmulationSection().setDefaultSidModel(ChipModel.MOS8580);
		player.updateSIDChipConfiguration();
	}

	public void setDefaultEmulationReSid() {
		player.getConfig().getEmulationSection().setDefaultEmulation(Emulation.RESID);
		player.updateSIDChipConfiguration();
	}

	public void setDefaultEmulationReSidFp() {
		player.getConfig().getEmulationSection().setDefaultEmulation(Emulation.RESIDFP);
		player.updateSIDChipConfiguration();
	}

	public void typeKey(KeyTableEntry key) {
		player.getC64().getEventScheduler().scheduleThreadSafeKeyEvent(new Event("Virtual Keyboard Key Pressed") {
			@Override
			public void event() throws InterruptedException {
				player.getC64().getKeyboard().keyPressed(key);

				player.getC64().getEventScheduler()
						.scheduleThreadSafeKeyEvent(new Event("Virtual Keyboard Key Released") {
							@Override
							public void event() throws InterruptedException {
								player.getC64().getKeyboard().keyReleased(key);
							}
						});
			}
		});
	}

	public void pressKey(KeyTableEntry key) {
		player.getC64().getEventScheduler().scheduleThreadSafeKeyEvent(new Event("Virtual Keyboard Key Pressed") {
			@Override
			public void event() throws InterruptedException {
				player.getC64().getKeyboard().keyPressed(key);
			}
		});
	}

	public void releaseKey(KeyTableEntry key) {
		player.getC64().getEventScheduler().scheduleThreadSafeKeyEvent(new Event("Virtual Keyboard Key Released") {
			@Override
			public void event() throws InterruptedException {
				player.getC64().getKeyboard().keyReleased(key);
			}
		});
	}

	public void joystick(int number, int value) {
		player.getC64().getEventScheduler().scheduleThreadSafeKeyEvent(new Event("Virtual Joystick Pressed") {
			@Override
			public void event() throws InterruptedException {
				player.getC64().setJoystick(number, () -> (byte) (0xff ^ value));

				player.getC64().getEventScheduler().scheduleThreadSafeKeyEvent(new Event("Virtual Joystick Released") {
					@Override
					public void event() throws InterruptedException {
						player.getC64().setJoystick(number, null);
					}
				});
			}
		});
	}

	private void setNextDiskImage() {
		if (diskImage != null) {
			File[] files = diskImage.getParentFile().listFiles(DISK_FILE_FILTER);
			List<File> filesList = Arrays.asList(Optional.ofNullable(files).orElse(new File[0]));
			Iterator<File> fileIterator = filesList.stream().sorted().iterator();
			while (fileIterator.hasNext()) {
				File siblingFile = fileIterator.next();
				if (siblingFile.equals(diskImage) && fileIterator.hasNext()) {
					diskImage = fileIterator.next();
					return;
				}
			}
			diskImage = filesList.stream().sorted().findFirst().orElse(diskImage);
		}
	}

	private File extract() throws IOException {
		if (diskImage != null) {
			TFile file = new TFile(diskImage);
			if (file.isEntry()) {
				File tmpDir = player.getConfig().getSidplay2Section().getTmpDir();
				File tmpFile = new File(tmpDir, file.getName());
				tmpFile.deleteOnExit();
				TFile.cp_rp(file, tmpFile, TArchiveDetector.ALL);
				return tmpFile;
			}
		}
		return diskImage;
	}

	private void addStatusTextListener() {
		player.stateProperty().addListener(event -> {
			if (event.getNewValue() == State.START) {

				player.getC64().getEventScheduler().schedule(new Event("Update Status Text") {
					@Override
					public void event() throws InterruptedException {

						getXuggleVideoDriver().ifPresent(xuggleVideoDriver -> {
							xuggleVideoDriver.setStatusText(createStatusText());

							int statusTextOffset = xuggleVideoDriver.getStatusTextOffset();
							int statusTextOverflow = xuggleVideoDriver.getStatusTextOverflow();

							// scroll forward after some time
							if (statusScrollCounter++ > 10) {
								if (statusTextOverflow > 0) {
									xuggleVideoDriver.setStatusTextOffset(statusTextOffset + 8);
								}
							}
							// reset scroll status if scroll has finished
							if (statusTextOverflow == 0) {
								statusScrollCounter = 0;
								xuggleVideoDriver.setStatusTextOffset(0);
							}
						});
						player.getC64().getEventScheduler().schedule(this,
								(long) (player.getC64().getClock().getCpuFrequency()));
					}
				}, 0);
			}
		});
	}

	private Optional<XuggleVideoDriver> getXuggleVideoDriver() {
		AudioDriver audioDriver = player.getAudioDriver();
		if (audioDriver instanceof ProxyDriver) {
			ProxyDriver proxyDriver = (ProxyDriver) audioDriver;
			if (proxyDriver.getDriverTwo() instanceof XuggleVideoDriver) {
				return Optional.of((XuggleVideoDriver) proxyDriver.getDriverTwo());
			}
		}
		return Optional.empty();
	}

	private String createStatusText() {
		StringBuilder result = new StringBuilder();

		String determinePSID64 = status.determinePSID64();
		String determineCartridge = status.determineCartridge();

		result.append(status.determineTime(false));
		result.append(", ");
		result.append(status.determineVideoNorm());
		result.append(", ");
		result.append(status.determineChipModels());
		result.append(", ");
		result.append(status.determineEmulations());
		result.append(determinePSID64.isEmpty() ? "" : ", " + determinePSID64);
		result.append(determineCartridge.isEmpty() ? "" : ", " + determineCartridge);
		result.append(", ");
		result.append(status.determineTapeActivity(false));
		result.append(status.determineDiskActivity(false));
		result.append(diskImage.getName());

		return result.toString();
	}

}
