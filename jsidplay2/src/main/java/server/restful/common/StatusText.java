package server.restful.common;

import java.io.File;
import java.util.Objects;
import java.util.ResourceBundle;

import libsidutils.status.Status;
import sidplay.Player;
import sidplay.audio.xuggle.XuggleVideoDriver;

public class StatusText {

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(StatusText.class.getName());

	private static final int WAIT_FOR_SCROLL_IN_SECONDS = 10;

	private static final int SCROLL_EVERY_NTH_FRAME = 2;

	private final Player player;

	private final Status status;

	private final double waitForScrollInFrames;

	private final boolean showStatus;

	private Boolean currentDirection;
	private boolean newDirection;
	private String lastStatusText;
	private int lastStatusTextX, statusScrollCounter;

	public StatusText(Player player, boolean showStatus) {
		this.player = player;
		status = new Status(player, RESOURCE_BUNDLE);
		waitForScrollInFrames = WAIT_FOR_SCROLL_IN_SECONDS * player.getC64().getClock().getScreenRefresh();
		this.showStatus = showStatus;
	}

	public void update(File diskImage) {
		String newStatusText = createStatusText(diskImage);
		player.getAudioDriver().lookup(XuggleVideoDriver.class).ifPresent(xuggleVideoDriver -> {
			int statusTextX = xuggleVideoDriver.getStatusTextX();
			int statusTextOverflow = xuggleVideoDriver.getStatusTextOverflow();

			if (!Objects.equals(newStatusText, lastStatusText) || statusTextX != lastStatusTextX) {
				xuggleVideoDriver.setStatusText(newStatusText);
				lastStatusText = newStatusText;
				lastStatusTextX = statusTextX;
			}
			if (currentDirection == null) {
				// wait for scroll start
				if (statusScrollCounter++ >= waitForScrollInFrames) {
					currentDirection = newDirection;
					statusScrollCounter = 0;
				}
			} else if (!currentDirection) {
				// scroll forward
				if (statusScrollCounter++ == SCROLL_EVERY_NTH_FRAME) {
					if (statusTextOverflow > 0) {
						xuggleVideoDriver.setStatusTextX(statusTextX + 1);
					} else {
						// scroll has finished, change direction
						newDirection = !currentDirection;
						currentDirection = null;
					}
					statusScrollCounter = 0;
				}
			} else {
				// scroll backwards
				if (statusScrollCounter++ == SCROLL_EVERY_NTH_FRAME) {
					if (statusTextX > 0) {
						xuggleVideoDriver.setStatusTextX(statusTextX - 1);
					} else {
						// scroll has finished, change direction
						newDirection = !currentDirection;
						currentDirection = null;
					}
					statusScrollCounter = 0;
				}
			}
		});
	}

	private String createStatusText(File diskImage) {
		StringBuilder result = new StringBuilder();

		if (Boolean.TRUE.equals(showStatus)) {
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
			result.append(replaceIllegalFilenameCharacters(diskImage.getName()));
		}
		return result.toString();
	}

	private String replaceIllegalFilenameCharacters(final String str) {
		return str.replace('_', '-');
	}

}