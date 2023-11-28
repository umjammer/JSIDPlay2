package server.restful.common.rtmp;

import static libsidplay.components.keyboard.KeyTableEntry.SPACE;
import static server.restful.common.IServletSystemProperties.HLS_NOT_YET_PLAYED_TIMEOUT;
import static server.restful.common.IServletSystemProperties.RTMP_EXCEEDS_MAXIMUM_DURATION;
import static server.restful.common.IServletSystemProperties.RTMP_NOT_YET_PLAYED_TIMEOUT;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import libsidplay.common.ChipModel;
import libsidplay.common.Emulation;
import libsidplay.common.Event;
import libsidplay.components.cart.CartridgeType;
import libsidplay.components.keyboard.KeyTableEntry;
import libsidplay.sidtune.SidTune;
import libsidutils.IOUtils;
import server.restful.servlets.ConvertServlet.ConvertServletParameters;
import sidplay.Player;
import sidplay.audio.SleepDriver;
import sidplay.player.State;
import ui.common.ConvenienceResult;
import ui.common.filefilter.CartFileFilter;
import ui.common.filefilter.DiskFileFilter;

public final class PlayerWithStatus {

	private static final DiskFileFilter DISK_FILE_FILTER = new DiskFileFilter();

	private static final CartFileFilter CART_FILE_FILTER = new CartFileFilter();

	private final Player player;

	private File diskImage;

	private File attachedCartridge;

	private CartridgeType attachedCartridgeType;

	private final int pressSpaceInterval;

	private final LocalDateTime created;

	private LocalDateTime validUntil;

	private final StatusText statusText;

	private int playCounter;

	public PlayerWithStatus(Player player, File diskImage, ConvenienceResult convenienceResult,
		ConvertServletParameters servletParameters) {
		this.player = player;
		this.diskImage = diskImage;
		attachedCartridge = convenienceResult.getAttatchedCartridge();
		attachedCartridgeType = convenienceResult.getAttachedCartridgeType();
		pressSpaceInterval = servletParameters.getPressSpaceInterval();
		created = LocalDateTime.now();
		validUntil = created.plusSeconds(RTMP_NOT_YET_PLAYED_TIMEOUT);
		statusText = new StatusText(player, servletParameters.getShowStatus(), servletParameters.getLocale());
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

	public void onKeepAlive(Long currentTime, Long bufferedEnd) {
		LocalDateTime maxDuration = created.plusSeconds(RTMP_EXCEEDS_MAXIMUM_DURATION);
		if (LocalDateTime.now().isBefore(maxDuration)) {
			validUntil = LocalDateTime.now().plusSeconds(HLS_NOT_YET_PLAYED_TIMEOUT);
		} else {
			validUntil = maxDuration;
		}
		player.getAudioDriver().lookup(SleepDriver.class)
				.ifPresent(sleepDriver -> sleepDriver.setClientTime(currentTime, bufferedEnd));
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
		diskImage = determineNextImage(diskImage, DISK_FILE_FILTER);
		if (diskImage != null && DISK_FILE_FILTER.accept(diskImage)) {
			player.insertDisk(IOUtils.extract(player.getConfig().getSidplay2Section().getTmpDir(), diskImage));
		}
		return diskImage;
	}

	public File insertNextCart() throws IOException {
		attachedCartridge = determineNextImage(attachedCartridge, CART_FILE_FILTER);
		if (attachedCartridge != null && CART_FILE_FILTER.accept(attachedCartridge)) {
			player.insertCartridge(attachedCartridgeType,
					IOUtils.extract(player.getConfig().getSidplay2Section().getTmpDir(), attachedCartridge));
		}
		return attachedCartridge;
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

	public void pressKey(KeyTableEntry key) {
		player.getC64().getEventScheduler().scheduleThreadSafeKeyEvent(
				Event.of("Virtual Keyboard Pressed", event -> player.getC64().getKeyboard().keyPressed(key)));
	}

	public void releaseKey(KeyTableEntry key) {
		player.getC64().getEventScheduler().scheduleThreadSafeKeyEvent(
				Event.of("Virtual Keyboard Released", event -> player.getC64().getKeyboard().keyReleased(key)));
	}

	public void typeKey(KeyTableEntry key) {
		player.getC64().getEventScheduler().scheduleThreadSafeKeyEvent(Event.of("Virtual Keyboard Pressed", event -> {

			if (key == KeyTableEntry.RESTORE) {
				player.getC64().getKeyboard().restore();
			} else {
				player.getC64().getKeyboard().keyPressed(key);

				player.getC64().getEventScheduler()
						.schedule(Event.of("Wait Until Virtual Keyboard Released", event2 -> {

							player.getC64().getEventScheduler()
									.scheduleThreadSafeKeyEvent(Event.of("Virtual Keyboard Released",
											event3 -> player.getC64().getKeyboard().keyReleased(key)));

						}), player.getC64().getClock().getCyclesPerFrame() << 2);
			}
		}));
	}

	public void joystick(int number, int value) {
		player.getC64().getEventScheduler().scheduleThreadSafeKeyEvent(Event.of("Virtual Joystick Pressed", event -> {

			player.getC64().setJoystick(number, () -> (byte) (0xff ^ value));

			player.getC64().getEventScheduler().schedule(Event.of("Wait Until Virtual Joystick Released", event2 -> {

				player.getC64().getEventScheduler().scheduleThreadSafeKeyEvent(
						Event.of("Virtual Joystick Released", event3 -> player.getC64().setJoystick(number, null)));

			}), player.getC64().getClock().getCyclesPerFrame() << 2);
		}));
	}

	private File determineNextImage(File imageFile, FileFilter fileFilter) {
		if (imageFile != null) {
			File[] files = imageFile.getParentFile().listFiles(fileFilter);
			List<File> filesList = Arrays.asList(Optional.ofNullable(files).orElse(new File[0])).stream()
					.filter(File::isFile).collect(Collectors.toList());
			Iterator<File> fileIterator = filesList.stream().sorted().iterator();
			while (fileIterator.hasNext()) {
				File file = fileIterator.next();
				if (file.equals(imageFile) && fileIterator.hasNext()) {
					return fileIterator.next();
				}
			}
			return filesList.stream().sorted().findFirst().orElse(imageFile);
		}
		return imageFile;
	}

	private void addPressSpaceListener() {
		if (pressSpaceInterval > 0) {

			player.stateProperty().addListener(event -> {
				if (event.getNewValue() == State.START) {

					long initDelay = SidTune.getInitDelay(player.getTune());

					player.getC64().getEventScheduler().schedule(Event.of("Virtual Keyboard Pressed", event2 -> {

						// press space every N seconds
						player.getC64().getKeyboard().keyPressed(SPACE);

						player.getC64().getEventScheduler().schedule(Event.of("Virtual Keyboard Released", event3 -> {

							player.getC64().getKeyboard().keyReleased(SPACE);

						}), player.getC64().getClock().getCyclesPerFrame() << 2);

						player.getC64().getEventScheduler().schedule(event2,
								pressSpaceInterval * (long) player.getC64().getClock().getCpuFrequency());

					}), initDelay + pressSpaceInterval * (long) player.getC64().getClock().getCpuFrequency());
				}
			});
		}
	}

	private void addStatusTextListener() {
		player.stateProperty().addListener(event -> {
			if (event.getNewValue() == State.START) {

				player.getC64().getEventScheduler().schedule(Event.of("Update Status Text", event2 -> {
					statusText.update(diskImage);

					player.getC64().getEventScheduler().schedule(event2,
							player.getC64().getClock().getCyclesPerFrame());
				}), 0);
			}
		});
	}

}
