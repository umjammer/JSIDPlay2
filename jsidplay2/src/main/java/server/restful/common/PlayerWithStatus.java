package server.restful.common;

import static libsidplay.components.keyboard.KeyTableEntry.SPACE;
import static server.restful.common.IServletSystemProperties.HLS_NOT_YET_PLAYED_TIMEOUT;
import static server.restful.common.IServletSystemProperties.RTMP_EXCEEDS_MAXIMUM_DURATION;
import static server.restful.common.IServletSystemProperties.RTMP_NOT_YET_PLAYED_TIMEOUT;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Event;
import libsidplay.components.keyboard.KeyTableEntry;
import net.java.truevfs.access.TArchiveDetector;
import net.java.truevfs.access.TFile;
import sidplay.Player;
import sidplay.audio.AudioDriver;
import sidplay.audio.ProxyDriver;
import sidplay.audio.SleepDriver;
import sidplay.player.State;
import ui.common.filefilter.DiskFileFilter;

public final class PlayerWithStatus {

	private static final DiskFileFilter DISK_FILE_FILTER = new DiskFileFilter();

	private final Player player;

	private File diskImage;

	private final int pressSpaceInterval;

	private final StatusText statusText;

	private final LocalDateTime created;

	private LocalDateTime validUntil;

	private int playCounter;

	public PlayerWithStatus(Player player, File diskImage, boolean showStatus, int pressSpaceInterval) {
		this.player = player;
		this.diskImage = diskImage;
		this.pressSpaceInterval = pressSpaceInterval;
		statusText = new StatusText(player, showStatus);
		created = LocalDateTime.now();
		validUntil = created.plusSeconds(RTMP_NOT_YET_PLAYED_TIMEOUT);
		addPressSpaceListener();
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

	public void onKeepAlive(long currentTime) {
		LocalDateTime maxDuration = created.plusSeconds(RTMP_EXCEEDS_MAXIMUM_DURATION);
		if (LocalDateTime.now().isBefore(maxDuration)) {
			validUntil = LocalDateTime.now().plusSeconds(HLS_NOT_YET_PLAYED_TIMEOUT);
		} else {
			validUntil = maxDuration;
		}
		getSleepDriver().ifPresent(sleepDriver -> sleepDriver.setCurrentTime(currentTime));
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

	public File insertNextDisk() throws IOException {
		diskImage = determineNextDiskImage(diskImage);
		player.insertDisk(extract(diskImage));
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
				if (key == KeyTableEntry.RESTORE) {
					player.getC64().getKeyboard().restore();
				} else {
					player.getC64().getKeyboard().keyPressed(key);

					player.getC64().getEventScheduler().schedule(new Event("Wait Until Virtual Keyboard Key Released") {
						@Override
						public void event() throws InterruptedException {
							player.getC64().getEventScheduler()
									.scheduleThreadSafeKeyEvent(new Event("Virtual Keyboard Key Released") {
										@Override
										public void event() throws InterruptedException {
											player.getC64().getKeyboard().keyReleased(key);
										}
									});
						}
					}, player.getC64().getClock().getCyclesPerFrame() << 2);
				}
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

				player.getC64().getEventScheduler().schedule(new Event("Wait Until Virtual Keyboard Key Released") {
					@Override
					public void event() throws InterruptedException {
						player.getC64().getEventScheduler()
								.scheduleThreadSafeKeyEvent(new Event("Virtual Joystick Released") {
									@Override
									public void event() throws InterruptedException {
										player.getC64().setJoystick(number, null);
									}
								});
					}
				}, player.getC64().getClock().getCyclesPerFrame() << 2);
			}
		});
	}

	private File determineNextDiskImage(File diskImage) {
		if (diskImage != null) {
			File[] files = diskImage.getParentFile().listFiles(DISK_FILE_FILTER);
			List<File> filesList = Arrays.asList(Optional.ofNullable(files).orElse(new File[0])).stream()
					.filter(File::isFile).collect(Collectors.toList());
			Iterator<File> fileIterator = filesList.stream().sorted().iterator();
			while (fileIterator.hasNext()) {
				File file = fileIterator.next();
				if (file.equals(diskImage) && fileIterator.hasNext()) {
					return fileIterator.next();
				}
			}
			return filesList.stream().sorted().findFirst().orElse(diskImage);
		}
		return diskImage;
	}

	private File extract(File diskImage) throws IOException {
		if (diskImage != null) {
			TFile file = new TFile(diskImage);
			if (file.isEntry()) {
				File tmpDir = new File(player.getConfig().getSidplay2Section().getTmpDir(),
						UUID.randomUUID().toString());
				File tmpFile = new File(tmpDir, file.getName());
				tmpDir.mkdirs();
				TFile.cp_rp(file, tmpFile, TArchiveDetector.ALL);
				return tmpFile;
			}
		}
		return diskImage;
	}

	private void addPressSpaceListener() {
		if (pressSpaceInterval > 0) {
			player.stateProperty().addListener(event -> {
				if (event.getNewValue() == State.START) {

					player.getC64().getEventScheduler().schedule(new Event("Key Pressed") {
						@Override
						public void event() throws InterruptedException {

							// press space every N seconds
							player.getC64().getKeyboard().keyPressed(SPACE);

							player.getC64().getEventScheduler().schedule(new Event("Key Released") {
								@Override
								public void event() throws InterruptedException {
									player.getC64().getKeyboard().keyReleased(SPACE);
								}
							}, player.getC64().getClock().getCyclesPerFrame() << 2);

							player.getC64().getEventScheduler().schedule(this,
									pressSpaceInterval * (long) player.getC64().getClock().getCpuFrequency());
						}

					}, pressSpaceInterval * (long) player.getC64().getClock().getCpuFrequency());
				}
			});
		}
	}

	private void addStatusTextListener() {
		player.stateProperty().addListener(event -> {
			if (event.getNewValue() == State.START) {

				player.getC64().getEventScheduler().schedule(new Event("Update Status Text") {
					@Override
					public void event() throws InterruptedException {
						statusText.update(diskImage);

						player.getC64().getEventScheduler().schedule(this,
								player.getC64().getClock().getCyclesPerFrame());
					}
				}, 0);
			}
		});
	}

	private Optional<SleepDriver> getSleepDriver() {
		AudioDriver audioDriver = player.getAudioDriver();
		if (audioDriver instanceof ProxyDriver) {
			ProxyDriver proxyDriver = (ProxyDriver) audioDriver;
			if (proxyDriver.getDriverOne() instanceof SleepDriver) {
				return Optional.of((SleepDriver) proxyDriver.getDriverOne());
			}
		}
		return Optional.empty();
	}

}
