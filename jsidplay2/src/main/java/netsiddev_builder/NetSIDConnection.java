package netsiddev_builder;

import static netsiddev.Response.BUSY;
import static netsiddev.Response.INFO;
import static netsiddev.Response.OK;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javafx.util.Pair;
import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.SamplingMethod;
import libsidplay.components.pla.PLA;
import libsidplay.sidtune.SidTune;
import netsiddev.Response;
import netsiddev_builder.commands.Flush;
import netsiddev_builder.commands.GetConfigCount;
import netsiddev_builder.commands.GetConfigInfo;
import netsiddev_builder.commands.GetVersion;
import netsiddev_builder.commands.Mute;
import netsiddev_builder.commands.NetSIDPkg;
import netsiddev_builder.commands.SetSIDClocking;
import netsiddev_builder.commands.SetSIDCount;
import netsiddev_builder.commands.SetSIDLevel;
import netsiddev_builder.commands.SetSIDModel;
import netsiddev_builder.commands.SetSIDPosition;
import netsiddev_builder.commands.SetSIDSampling;
import netsiddev_builder.commands.TryDelay;
import netsiddev_builder.commands.TryRead;
import netsiddev_builder.commands.TryReset;
import netsiddev_builder.commands.TryWrite;

public class NetSIDConnection {
	private static final Charset ISO_8859_1 = Charset.forName("ISO-8859-1");

	private static final int PORT = 6581;
	private static final String HOSTNAME = "127.0.0.1";

	private static final int MAX_WRITE_CYCLES = 4096;
	private static final int CMD_BUFFER_SIZE = 4096;
	private static final int REGULAR_DELAY = 0xFFFF;

	byte VERSION;
	private EventScheduler context;
	private static Socket connectedSocket;
	private List<NetSIDPkg> commands = new ArrayList<>();
	private TryWrite tryWrite;
	private byte readResult, configInfo[] = new byte[255];
	private long lastSIDWriteTime;
	private int fastForwardFactor;
	private boolean startTimeReached;
	private static Map<Pair<ChipModel, String>, Byte> filterNameToSIDModel = new HashMap<>();

	public NetSIDConnection(EventScheduler context, SidTune tune) {
		this.context = context;
		try {
			if (connectedSocket == null || !connectedSocket.isConnected()) {
				connectedSocket = new Socket(HOSTNAME, PORT);
				VERSION = getVersion();
				// Get all available SIDs
				for (byte config = 0; config < getConfigCount(); config++) {
					byte[] chipModel = new byte[1];
					String filter = getConfigInfo(config, chipModel);
					ChipModel model = chipModel[0] == 1 ? ChipModel.MOS8580 : ChipModel.MOS6581;
					filterNameToSIDModel.put(new Pair<>(model, filter), config);
				}
				// Initialize SIDs on server side
				commands.add(new SetSIDCount((byte) PLA.MAX_SIDS));
				for (byte sidNum = 0; sidNum < PLA.MAX_SIDS; sidNum++) {
					commands.add(new SetSIDModel(sidNum, sidNum));
				}
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private byte getVersion() {
		return addReadCommandAfterFlushingWrites(() -> new GetVersion());
	}

	public static List<String> getFilterNames(ChipModel model) {
		return filterNameToSIDModel.keySet().stream().filter(p -> p.getKey() == model).map(p -> p.getValue())
				.sorted((s1, s2) -> s1.compareToIgnoreCase(s2)).collect(Collectors.toList());
	}

	public void setSIDByFilterName(byte sidNum, final ChipModel chipModel, final String filterName) {
		addCommandsAfterFlushingWrites(() -> {
			Optional<Pair<ChipModel, String>> filter = filterNameToSIDModel.keySet().stream()
					.filter(p -> p.getKey() == chipModel && p.getValue().equals(filterName)).findFirst();
			if (filter.isPresent()) {
				return new NetSIDPkg[] { new SetSIDModel(sidNum, filterNameToSIDModel.get(filter.get())) };
			}
			System.err.println("Undefined Filter: " + filterName + ", will use first available filter, instead!");
			return new NetSIDPkg[] { new SetSIDModel(sidNum, (byte) 0) };
		});
	}

	private byte getConfigCount() {
		return addReadCommandAfterFlushingWrites(() -> new GetConfigCount());
	}

	private String getConfigInfo(byte sidNum, byte[] chipModel) {
		addCommandsAfterFlushingWrites(() -> new NetSIDPkg[] { new GetConfigInfo(sidNum) });
		chipModel[0] = readResult;
		int i = 0;
		for (; configInfo[i] != 0 && i < configInfo.length; i++) {
		}
		return new String(configInfo, 0, i, ISO_8859_1);
	}

	public void setClockFrequency(byte sidNum, double cpuFrequency) {
		addCommandsAfterFlushingWrites(() -> new NetSIDPkg[] { new SetSIDClocking(sidNum, cpuFrequency) });
	}

	public void setSampling(byte sidNum, SamplingMethod sampling) {
		addCommandsAfterFlushingWrites(() -> new NetSIDPkg[] { new SetSIDSampling(sidNum, (byte) sampling.ordinal()) });
	}

	public void flush(byte sidNum) {
		addCommandsAfterFlushingWrites(() -> new NetSIDPkg[] { new Flush(sidNum) });
	}

	public void reset(byte sidNum, byte volume) {
		addCommandsAfterFlushingWrites(() -> new NetSIDPkg[] { new Flush(sidNum), new TryReset(sidNum, volume) });
	}

	public void mute(byte sidNum, byte voice, boolean mute) {
		addCommandsAfterFlushingWrites(() -> new NetSIDPkg[] { new Mute(sidNum, voice, mute) });
	}

	public void setVolume(byte sidNum, float volume) {
		addCommandsAfterFlushingWrites(() -> new NetSIDPkg[] { new SetSIDLevel(sidNum, (byte) (volume * 5)) });
	}

	public void setBalance(byte sidNum, float balance) {
		addCommandsAfterFlushingWrites(
				() -> new NetSIDPkg[] { new SetSIDPosition(sidNum, (byte) (200 * (1 - balance) - 100)) });
	}

	public byte read(byte sidNum, byte addr) {
		return addReadCommandAfterFlushingWrites(() -> new TryRead(sidNum, clocksSinceLastAccess(), addr));
	}

	public void write(byte sidNum, byte addr, byte data) {
		if (startTimeReached) {
			try {
				while (tryWrite(sidNum, clocksSinceLastAccess(), addr, data) == BUSY) {
					// Try_Write sleeps for us
				}
			} catch (InterruptedException | IOException e) {
				throw new RuntimeException(e);
			}
		} else {
			delay(sidNum, clocksSinceLastAccess());
		}
	}

	private void delay(byte sidNum, int cycles) {
		addCommandsAfterFlushingWrites(() -> new NetSIDPkg[] { new TryDelay(sidNum, cycles) });
	}

	private byte addReadCommandAfterFlushingWrites(Supplier<NetSIDPkg> cmdToAdd) {
		addCommandsAfterFlushingWrites(() -> new NetSIDPkg[] { cmdToAdd.get() });
		return readResult;
	}

	private void addCommandsAfterFlushingWrites(Supplier<NetSIDPkg[]> cmdToAdd) {
		try {
			/* deal with unsubmitted writes */
			flush(false);

			for (NetSIDPkg netSIDPkg : cmdToAdd.get()) {
				commands.add(netSIDPkg);
			}
			flush(false);
		} catch (IOException | InterruptedException e) {
			throw new RuntimeException(e);
		}

	}

	// Add a SID write to the ring buffer, until it is full,
	// then send it to JSIDPlay2 to be queued there and executed
	private Response tryWrite(byte sidNum, int cycles, byte reg, byte data) throws InterruptedException, IOException {
		/*
		 * flush writes after a bit of buffering. If no flush, then returns OK
		 * and we queue more. If flush attempt fails, we must cancel.
		 */
		if (maybe_send_writes_to_server() == BUSY) {
			sleepDependingOnCyclesSent();
			return BUSY;
		}

		if (commands.isEmpty()) {
			/* start new write buffering sequence */
			tryWrite = new TryWrite(sidNum);
			commands.add(tryWrite);
		}
		/* add write to queue */
		tryWrite.addWrite(cycles, (byte) (reg | (sidNum << 5)), data);
		/*
		 * NB: if flush attempt fails, we have nevertheless queued command
		 * locally and thus are allowed to return OK in any case.
		 */
		maybe_send_writes_to_server();

		return OK;
	}

	private Response flush(boolean giveUpIfBusy) throws IOException, InterruptedException {
		while (!commands.isEmpty()) {
			final NetSIDPkg cmd = commands.remove(0);

			connectedSocket.getOutputStream().write(cmd.toByteArrayWithLength());
			int rc = connectedSocket.getInputStream().read();
			if (rc == -1) {
				throw new RuntimeException("Server closed the connection!");
			}
			switch (Response.values()[rc]) {
			case READ:
			case COUNT:
			case INFO:
			case VERSION:
				// read result / configuration count / chip model
				readResult = (byte) connectedSocket.getInputStream().read();
				// INFO: 0 terminated name
				for (int i = 0; rc == INFO.ordinal() && i < configInfo.length; i++) {
					connectedSocket.getInputStream().read(configInfo, i, 1);
					if (configInfo[i] == 0)
						break;
				}
			case OK:
				continue;
			case BUSY:
				commands.add(0, cmd);
				if (giveUpIfBusy) {
					return BUSY;
				}
				sleepDependingOnCyclesSent();
				continue;
			default:
				throw new RuntimeException("Server error: Unexpected response!");
			}
		}
		return OK;
	}

	private void sleepDependingOnCyclesSent() throws InterruptedException {
		if (tryWrite.getCyclesSentToServer() > (MAX_WRITE_CYCLES * 3 >> 2)) {
			Thread.sleep(Math.max(1, tryWrite.getCyclesSentToServer() / 1000 - 3));
		}
	}

	private Response maybe_send_writes_to_server() throws IOException, InterruptedException {
		/* flush writes after a bit of buffering */
		if (commands.size() == CMD_BUFFER_SIZE
				|| (tryWrite != null && tryWrite.getCyclesSentToServer() > MAX_WRITE_CYCLES)) {
			if (flush(true) == BUSY) {
				return BUSY;
			}
		}
		return OK;
	}

	private int clocksSinceLastAccess() {
		final long now = context.getTime(Event.Phase.PHI2);
		int diff = (int) (now - lastSIDWriteTime);
		lastSIDWriteTime = now;
		return diff >> fastForwardFactor;
	}

	long eventuallyDelay(byte sidNum) {
		final long now = context.getTime(Event.Phase.PHI2);
		int diff = (int) (now - lastSIDWriteTime) >> fastForwardFactor;
		if (diff > REGULAR_DELAY << 1) { // next writes must not be too soon!
			lastSIDWriteTime += REGULAR_DELAY;
			delay(sidNum, REGULAR_DELAY);
		}
		return REGULAR_DELAY;
	}

	public void start() {
		startTimeReached = true;
	}

	public void fastForward() {
		fastForwardFactor++;
	}

	public void normalSpeed() {
		fastForwardFactor = 0;
	}

	public boolean isFastForward() {
		return fastForwardFactor != 0;
	}

	public int getFastForwardBitMask() {
		return (1 << fastForwardFactor) - 1;
	}

}
