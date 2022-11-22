package server.restful.common;

import static java.util.Optional.ofNullable;
import static server.restful.common.IServletSystemProperties.RTMP_CLEANUP_PLAYER_COUNTER;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.TimerTask;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.juli.logging.Log;

import server.restful.servlets.ConvertServlet.ConvertServletParameters;
import sidplay.Player;

public final class PlayerCleanupTimerTask extends TimerTask {

	private static final Map<UUID, PlayerWithStatus> PLAYER_MAP = Collections.synchronizedMap(new HashMap<>());

	private final Log logger;

	private int timerCounter;

	public PlayerCleanupTimerTask(Log logger) {
		this.logger = logger;
	}

	public static final void create(UUID uuid, Player player, File diskImage,
			ConvertServletParameters servletParameters, ResourceBundle resourceBundle) {
		PLAYER_MAP.put(uuid, new PlayerWithStatus(player, diskImage, servletParameters.getShowStatus(),
				Optional.ofNullable(servletParameters.getPressSpaceInterval()).orElse(0), resourceBundle));
	}

	public static final void update(UUID uuid, Consumer<PlayerWithStatus> playerWithStatusConsumer) {
		ofNullable(PLAYER_MAP.get(uuid)).ifPresent(playerWithStatusConsumer);
	}

	public static final int count() {
		return PLAYER_MAP.keySet().size();
	}

	@Override
	public final void run() {
		Collection<Entry<UUID, PlayerWithStatus>> playerEntriesToRemove = PLAYER_MAP.entrySet().stream()
				.filter(entrySet -> entrySet.getValue().isInvalid()).collect(Collectors.toList());

		playerEntriesToRemove.forEach(this::quitPlayer);

		PLAYER_MAP.entrySet().removeAll(playerEntriesToRemove);

		if (timerCounter++ % RTMP_CLEANUP_PLAYER_COUNTER == 0) {
			PLAYER_MAP.entrySet().forEach(this::printPlayer);
		}
	}

	private void quitPlayer(Map.Entry<UUID, PlayerWithStatus> entry) {
		logger.info("CleanupPlayerTimerTask: AUTO-QUIT RTMP stream of: " + entry.getKey());
		entry.getValue().quitPlayer();
	}

	private void printPlayer(Entry<UUID, PlayerWithStatus> entry) {
		logger.info(String.format("CleanupPlayerTimerTask: RTMP stream left: %s (valid until %s)", entry.getKey(),
				entry.getValue().getValidUntil()));
	}

}
