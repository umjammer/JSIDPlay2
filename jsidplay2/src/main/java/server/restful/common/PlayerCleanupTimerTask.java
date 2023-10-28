package server.restful.common;

import static java.util.Optional.ofNullable;
import static libsidutils.IOUtils.deleteDirectory;
import static server.restful.common.IServletSystemProperties.CLEANUP_DIRECTORY_PERIOD;
import static server.restful.common.IServletSystemProperties.MAXIMUM_DURATION_TEMP_DIRECTORIES;
import static server.restful.common.IServletSystemProperties.RTMP_PRINT_PLAYER_PERIOD;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TimerTask;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.catalina.Context;
import org.apache.juli.logging.Log;

import server.restful.servlets.ConvertServlet.ConvertServletParameters;
import sidplay.Player;
import sidplay.filefilter.UUIDFileFilter;
import ui.common.ConvenienceResult;

public final class PlayerCleanupTimerTask extends TimerTask {

	private static final Map<UUID, PlayerWithStatus> PLAYER_MAP = Collections.synchronizedMap(new HashMap<>());

	private static final UUIDFileFilter UUID_FILE_FILTER = new UUIDFileFilter();

	private final Log logger;

	private final File catalinaBaseFile;

	private int timerCounter;

	public PlayerCleanupTimerTask(Context context) {
		this.logger = context.getParent().getLogger();
		this.catalinaBaseFile = context.getCatalinaBase();
	}

	public static final void create(UUID uuid, Player player, File diskImage, ConvenienceResult convenienceResult,
			ConvertServletParameters servletParameters) {
		PLAYER_MAP.put(uuid, new PlayerWithStatus(player, diskImage, convenienceResult, servletParameters));
	}

	public static final void update(UUID uuid, Consumer<PlayerWithStatus> playerWithStatusConsumer) {
		ofNullable(PLAYER_MAP.get(uuid)).ifPresent(playerWithStatusConsumer);
	}

	public static final int count() {
		return PLAYER_MAP.size();
	}

	@Override
	public final void run() {
		Collection<Entry<UUID, PlayerWithStatus>> playerEntriesToRemove = PLAYER_MAP.entrySet().stream()
				.filter(entrySet -> entrySet.getValue().isInvalid()).collect(Collectors.toList());

		playerEntriesToRemove.forEach(this::quitPlayer);

		PLAYER_MAP.entrySet().removeAll(playerEntriesToRemove);

		if (timerCounter % RTMP_PRINT_PLAYER_PERIOD == 0) {
			PLAYER_MAP.entrySet().forEach(this::printPlayer);
		}
		if (timerCounter % CLEANUP_DIRECTORY_PERIOD == 0) {
			deleteOutdatedTempDirectories();
		}
		timerCounter++;
	}

	private void quitPlayer(Map.Entry<UUID, PlayerWithStatus> entry) {
		logger.info(String.format("CleanupPlayerTimerTask: AUTO-QUIT RTMP uuid=%s", entry.getKey()));
		entry.getValue().quitPlayer();
	}

	private void printPlayer(Entry<UUID, PlayerWithStatus> entry) {
		logger.info(String.format("CleanupPlayerTimerTask: RTMP is still running uuid=%s (valid until %s)",
				entry.getKey(), entry.getValue().getValidUntil()));
	}

	private void deleteOutdatedTempDirectories() {
		Arrays.asList(Optional.ofNullable(catalinaBaseFile.listFiles(UUID_FILE_FILTER)).orElse(new File[0])).stream()
				.filter(File::isDirectory).forEach(dir -> {
					try {
						LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(dir.lastModified()),
								ZoneId.systemDefault());
						if (dateTime.plusSeconds(MAXIMUM_DURATION_TEMP_DIRECTORIES).isBefore(LocalDateTime.now())) {
							logger.info(String.format("CleanupPlayerTimerTask: Delete temp. directory: %s", dir));
							deleteDirectory(dir);
						}
					} catch (IOException e) {
						logger.error(e.getMessage());
					}
				});
	}
}
