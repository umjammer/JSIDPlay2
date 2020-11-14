package builder.sidblaster;

import static libsidplay.common.Engine.SIDBLASTER;
import static libsidplay.components.pla.PLA.MAX_SIDS;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.sun.jna.Native;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.HardwareSIDBuilder;
import libsidplay.common.Mixer;
import libsidplay.common.SIDEmu;
import libsidplay.config.IAudioSection;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;
import sidplay.audio.AudioDriver;

/**
 *
 * @author Ken Händel
 *
 */
public class SidBlasterBuilder implements HardwareSIDBuilder, Mixer {

	private static final short REGULAR_DELAY = 4096;

	/**
	 * System event context.
	 */
	private EventScheduler context;

	/**
	 * Configuration
	 */
	private IConfig config;

	/**
	 * CPU clock.
	 */
	private CPUClock cpuClock;

	/**
	 * Native library wrapper.
	 */
	private static HardSID hardSID;

	/**
	 * Number of SIDBlaster devices.
	 */
	private static int deviceCount;

	/**
	 * Serial numbers of SIDBlaster devices.
	 */
	private static String[] serialNumbers;

	/**
	 * Already used SIDBlaster SIDs.
	 */
	private List<SIDBlasterEmu> sids = new ArrayList<>();

	protected long lastSIDWriteTime;

	private int fastForwardFactor;

	private int[] delayInCycles = new int[MAX_SIDS];

	public SidBlasterBuilder(EventScheduler context, IConfig config, CPUClock cpuClock) {
		this.context = context;
		this.config = config;
		this.cpuClock = cpuClock;
		if (hardSID == null) {
			try {
				hardSID = Native.load("hardsid", HardSID.class);
				init();
			} catch (UnsatisfiedLinkError e) {
				System.err.println("Error: Windows is required to use " + SIDBLASTER + " soundcard!");
				throw e;
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void init() {
		hardSID.InitHardSID_Mapper();
		deviceCount = hardSID.HardSID_Devices();
		serialNumbers = new String[deviceCount];
		for (byte deviceId = 0; deviceId < deviceCount; deviceId++) {
			serialNumbers[deviceId] = hardSID.GetSerial(deviceId);
		}
	}

	@Override
	public SIDEmu lock(SIDEmu oldHardSID, int sidNum, SidTune tune) {
		IAudioSection audioSection = config.getAudioSection();
		IEmulationSection emulationSection = config.getEmulationSection();
		ChipModel chipModel = ChipModel.getChipModel(emulationSection, tune, sidNum);

		SimpleEntry<Integer, ChipModel> deviceIdAndChipModel = getModelDependantDeviceId(chipModel, sidNum);
		Integer deviceId = deviceIdAndChipModel.getKey();
		ChipModel model = deviceIdAndChipModel.getValue();

		if (deviceId != null && deviceId < deviceCount) {
			if (oldHardSID != null) {
				// always re-use hardware SID chips, if configuration changes
				// the purpose is to ignore chip model changes!
				return oldHardSID;
			}
			SIDBlasterEmu hsid = createSID(deviceId.byteValue(), sidNum, tune, model);
			hsid.setDeviceName(serialNumbers[deviceId]);

			if (hsid.lock()) {
				sids.add(hsid);
				setDelay(sidNum, audioSection.getDelay(sidNum));
				return hsid;
			}
		}
		System.err.println(/* throw new RuntimeException( */
				String.format("SIDBLASTER ERROR: System doesn't have enough SID chips. Requested: (sidNum=%d)",
						sidNum));
		return SIDEmu.NONE;
	}

	private SIDBlasterEmu createSID(byte deviceId, int sidNum, SidTune tune, ChipModel chipModel) {
		if (SidTune.isFakeStereoSid(config.getEmulationSection(), tune, sidNum)) {
			return new SIDBlasterEmu.FakeStereo(context, config, this, hardSID, deviceId, sidNum, chipModel, sids);
		} else {
			return new SIDBlasterEmu(context, this, hardSID, deviceId, sidNum, chipModel);
		}
	}

	@Override
	public void unlock(final SIDEmu sidEmu) {
		SIDBlasterEmu hardSid = (SIDBlasterEmu) sidEmu;
		hardSid.unlock();
		sids.remove(sidEmu);
	}

	@Override
	public int getDeviceCount() {
		return deviceCount;
	}

	@Override
	public Integer getDeviceId(int sidNum) {
		return sidNum < sids.size() ? Integer.valueOf(sids.get(sidNum).getDeviceId()) : null;
	}

	@Override
	public String getDeviceName(int sidNum) {
		return sidNum < sids.size() ? sids.get(sidNum).getDeviceName() : null;
	}

	@Override
	public ChipModel getDeviceChipModel(int sidNum) {
		return sidNum < sids.size() ? sids.get(sidNum).getChipModel() : null;
	}

	@Override
	public void setAudioDriver(AudioDriver audioDriver) {
	}

	@Override
	public void start() {
	}

	@Override
	public void fadeIn(double fadeIn) {
		System.err.println("Fade-in unsupported by SIDBlaster");
		// XXX unsupported by SIDBlaster
	}

	@Override
	public void fadeOut(double fadeOut) {
		System.err.println("Fade-out unsupported by SIDBlaster");
		// XXX unsupported by SIDBlaster
	}

	@Override
	public void setVolume(int sidNum, float volume) {
		System.err.println("Volume unsupported by SIDBlaster");
		// XXX unsupported by SIDBlaster
	}

	@Override
	public void setBalance(int sidNum, float balance) {
		System.err.println("Balance unsupported by SIDBlaster");
		// XXX unsupported by SIDBlaster
	}

	public int getDelayInCycles(int sidNum) {
		return delayInCycles[sidNum];
	}

	@Override
	public void setDelay(int sidNum, int delay) {
		delayInCycles[sidNum] = (int) (cpuClock.getCpuFrequency() / 1000. * delay);
	}

	@Override
	public void fastForward() {
		fastForwardFactor++;
	}

	@Override
	public void normalSpeed() {
		fastForwardFactor = 0;
	}

	@Override
	public boolean isFastForward() {
		return fastForwardFactor != 0;
	}

	@Override
	public int getFastForwardBitMask() {
		return (1 << fastForwardFactor) - 1;
	}

	@Override
	public void pause() {
		for (SIDBlasterEmu hSid : sids) {
			hardSID.HardSID_Flush(hSid.getDeviceId());
		}
	}

	/**
	 * Get SIDBlaster device index based on the desired chip model.
	 *
	 * @param chipModel desired chip model
	 * @param sidNum    current SID number
	 * @return SID index of the desired SIDBlaster device
	 */
	private AbstractMap.SimpleEntry<Integer, ChipModel> getModelDependantDeviceId(final ChipModel chipModel,
			int sidNum) {
		Map<String, ChipModel> deviceMap = config.getEmulationSection().getSidBlasterDeviceMap();

		// use next free slot (prevent wrong type)
		for (byte deviceId = 0; deviceId < deviceCount; deviceId++) {
			if (!isSerialNumAlreadyUsed(serialNumbers[deviceId])
					&& isChipModelMatching(chipModel, deviceMap, serialNumbers[deviceId])) {
				return new AbstractMap.SimpleEntry<>(Integer.valueOf(deviceId), chipModel);
			}
		}
		// Nothing matched? Use next free slot (no matter what type)
		for (byte deviceId = 0; deviceId < deviceCount; deviceId++) {
			if (!isSerialNumAlreadyUsed(serialNumbers[deviceId])) {
				return new AbstractMap.SimpleEntry<>(Integer.valueOf(deviceId), deviceMap.get(serialNumbers[deviceId]));
			}
		}
		// no slot left
		return new AbstractMap.SimpleEntry<>(null, null);
	}

	private boolean isChipModelMatching(final ChipModel chipModel, Map<String, ChipModel> deviceMap,
			String serialNumber) {
		return deviceMap.get(serialNumber) == null || deviceMap.get(serialNumber) == chipModel;
	}

	private boolean isSerialNumAlreadyUsed(String serialNumber) {
		return sids.stream().filter(sid -> Objects.equals(sid.getDeviceName(), serialNumber)).findFirst().isPresent();
	}

	int clocksSinceLastAccess() {
		final long now = context.getTime(Event.Phase.PHI2);
		int diff = (int) (now - lastSIDWriteTime);
		lastSIDWriteTime = now;
		return diff >> fastForwardFactor;
	}

	long eventuallyDelay() {
		final long now = context.getTime(Event.Phase.PHI2);
		int diff = (int) (now - lastSIDWriteTime);
		if (diff > REGULAR_DELAY) {
			lastSIDWriteTime += REGULAR_DELAY;
			hardSID.HardSID_Delay((byte) 0, (short) (REGULAR_DELAY >> fastForwardFactor));
		}
		return REGULAR_DELAY;
	}
}
