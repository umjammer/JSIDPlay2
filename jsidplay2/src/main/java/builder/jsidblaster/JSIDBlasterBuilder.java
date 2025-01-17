package builder.jsidblaster;

import static libsidplay.components.pla.PLA.MAX_SIDS;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.common.HardwareSIDBuilder;
import libsidplay.common.Mixer;
import libsidplay.common.OS;
import libsidplay.common.SIDEmu;
import libsidplay.config.IAudioSection;
import libsidplay.config.IConfig;
import libsidplay.config.IEmulationSection;
import libsidplay.sidtune.SidTune;
import sidblaster.hardsid.HardSID;
import sidblaster.hardsid.HardSIDImpl;

/**
 * 
 * Support of SIDBlaster mini USB devices.
 *
 * @author Ken Händel
 *
 */
public class JSIDBlasterBuilder implements HardwareSIDBuilder, Mixer {

	private static final short REGULAR_DELAY = 512;

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

	private long lastSIDWriteTime;

	private int fastForwardFactor;

	private int[] delayInCycles = new int[MAX_SIDS];

	public JSIDBlasterBuilder(EventScheduler context, IConfig config, CPUClock cpuClock) {
		this.context = context;
		this.config = config;
		this.cpuClock = cpuClock;
		if (hardSID == null) {
			hardSID = new HardSIDImpl();
			init();
		}
	}

	private void init() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> uninitialize()));

		deviceCount = hardSID.HardSID_Devices();
		hardSID.HardSID_SetWriteBufferSize((byte) config.getEmulationSection().getSidBlasterWriteBufferSize());
		serialNumbers = new String[deviceCount];
		for (byte deviceId = 0; deviceId < deviceCount; deviceId++) {
			serialNumbers[deviceId] = hardSID.HardSID_GetSerial(deviceId);
		}
	}

	public static void printInstallationHint() {
		if (OS.get() == OS.LINUX) {
			printLinuxInstallationHint();
		}
		System.err.println("Maybe you just forgot to plug-in your USB devices?");
		System.err.println("Use the magic wand of the SIDBlaster combobox to configure your SIDBlaster devices!");
		System.err.println();
	}

	private static void printLinuxInstallationHint() {
		System.err
				.println("If device cannot be used, please use this workaround (we grant access to the USB device)...");
		System.err.println();
		System.err.println("... On Ubuntu you do:");
		System.err.println("$ sudo vi /etc/udev/rules.d/91-sidblaster.rules");
		System.err.println(
				"ACTION==\"add\", ATTRS{idVendor}==\"0403\", ATTRS{idProduct}==\"6001\", MODE=\"0666\", RUN+=\"/bin/sh -c 'rmmod ftdi_sio && rmmod usbserial'\"");
		System.err.println("$ sudo udevadm control --reload-rules && sudo udevadm trigger");
		System.err.println();
		System.err.println("... and on Fedora you do:");
		System.err.println("$ sudo vi /etc/udev/rules.d/91-sidblaster.rules");
		System.err.println(
				"ACTION==\"add\", ATTRS{idVendor}==\"0403\", ATTRS{idProduct}==\"6001\", MODE=\"0666\", RUN+=\"/bin/sh -c 'echo -n $id:1.0 > /sys/bus/usb/drivers/ftdi_sio/unbind; echo -n $id:1.1 > /sys/bus/usb/drivers/ftdi_sio/unbind'\"");
	}

	@Override
	public SIDEmu lock(SIDEmu oldHardSID, int sidNum, SidTune tune) {
		IAudioSection audioSection = config.getAudioSection();
		IEmulationSection emulationSection = config.getEmulationSection();
		ChipModel chipModel = ChipModel.getChipModel(emulationSection, tune, sidNum);
		ChipModel defaultSidModel = emulationSection.getDefaultSidModel();

		SimpleEntry<Integer, ChipModel> deviceIdAndChipModel = getModelDependantDeviceId(chipModel, sidNum,
				emulationSection.getSidBlasterSerialNumber());

		Integer deviceId = deviceIdAndChipModel.getKey();
		ChipModel model = deviceIdAndChipModel.getValue();

		if (oldHardSID != null) {
			// always re-use hardware SID chips, if configuration changes
			// the purpose is to ignore chip model changes!
			return oldHardSID;
		}
		if (deviceId != null && deviceId < deviceCount) {
			SIDBlasterEmu sid = createSID(deviceId.byteValue(), sidNum, tune, model, defaultSidModel);

			if (sid.lock()) {
				sid.setFilterEnable(emulationSection, sidNum);
				sid.setDigiBoost(emulationSection.isDigiBoosted8580());
				for (int voice = 0; voice < 4; voice++) {
					sid.setVoiceMute(voice, emulationSection.isMuteVoice(sidNum, voice));
				}
				sids.add(sid);
				setDeviceName(sidNum, serialNumbers[deviceId]);
				setDelay(sidNum, audioSection.getDelay(sidNum));
				setLatencyTimer(emulationSection.getSidBlasterLatencyTimer());
				return sid;
			}
		}
		System.err.printf("SIDBLASTER ERROR: System doesn't have enough SID chips. Requested: (sidNum=%d)\n", sidNum);
		if (deviceCount == 0) {
			printInstallationHint();
		}
		return SIDEmu.NONE;
	}

	@Override
	public void unlock(final SIDEmu sidEmu) {
		SIDBlasterEmu sid = (SIDBlasterEmu) sidEmu;
		sids.remove(sid);
		sid.unlock();
	}

	@Override
	public int getDeviceCount() {
		return deviceCount;
	}

	public static String[] getSerialNumbers() {
		return serialNumbers;
	}

	public boolean isSidBlasterRead() {
		return config.getEmulationSection().isSidBlasterRead();
	}

	public static SIDType getSidType(int deviceId) {
		sidblaster.SIDType hardSID_GetSIDType = hardSID.HardSID_GetSIDType((byte) deviceId);
		return SIDType.to(hardSID_GetSIDType);
	}

	public static int setSidType(int deviceId, SIDType sidType) {
		sidblaster.SIDType hardSID_GetSIDType = SIDType.from(sidType);
		return hardSID.HardSID_SetSIDType((byte) deviceId, hardSID_GetSIDType);
	}

	public static int setSerial(int deviceId, String serialNo) {
		return hardSID.HardSID_SetSerial((byte) deviceId, serialNo);
	}

	public static void uninitialize() {
		if (hardSID != null) {
			hardSID.HardSID_Uninitialize();
		}
	}

	@Override
	public Integer getDeviceId(int sidNum) {
		return sidNum < sids.size() ? Integer.valueOf(sids.get(sidNum).getDeviceId()) : null;
	}

	@Override
	public String getDeviceName(int sidNum) {
		return sidNum < sids.size() ? sids.get(sidNum).getDeviceName() : null;
	}

	public void setDeviceName(int sidNum, String serialNo) {
		if (sidNum < sids.size()) {
			sids.get(sidNum).setDeviceName(serialNo);
		}
	}

	@Override
	public ChipModel getDeviceChipModel(int sidNum) {
		return sidNum < sids.size() ? sids.get(sidNum).getChipModel() : null;
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

	public int getDelay(int sidNum) {
		return delayInCycles[sidNum];
	}

	@Override
	public void setDelay(int sidNum, int delay) {
		delayInCycles[sidNum] = (int) (cpuClock.getCpuFrequency() / 1000. * delay);
	}

	public void setLatencyTimer(short ms) {
		hardSID.HardSID_SetLatencyTimer(ms);
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
		for (SIDBlasterEmu sid : sids) {
			hardSID.HardSID_Flush(sid.getDeviceId());
		}
	}

	private SIDBlasterEmu createSID(byte deviceId, int sidNum, SidTune tune, ChipModel chipModel,
			ChipModel defaultChipModel) {
		final IEmulationSection emulationSection = config.getEmulationSection();

		if (SidTune.isFakeStereoSid(emulationSection, tune, sidNum)) {
			return new SIDBlasterEmu.FakeStereo(this, context, cpuClock, hardSID, deviceId, sidNum, chipModel,
					defaultChipModel, sids, emulationSection);
		} else {
			return new SIDBlasterEmu(this, context, cpuClock, hardSID, deviceId, sidNum, chipModel, defaultChipModel);
		}
	}

	/**
	 * Get SIDBlaster device index based on the desired chip model.
	 *
	 * @param chipModel              desired chip model
	 * @param sidNum                 current SID number
	 * @param sidBlasterSerialNumber hard-wired serial number of device to test with
	 *                               (null - choose best fitting device)
	 * @return SID index of the desired SIDBlaster device
	 */
	private SimpleEntry<Integer, ChipModel> getModelDependantDeviceId(final ChipModel chipModel, int sidNum,
			String sidBlasterSerialNumber) {
		if (sidBlasterSerialNumber == null) {
			// DEFAULT: choose best fitting device for sound output

			final Map<String, ChipModel> deviceMap = config.getEmulationSection().getSidBlasterDeviceMap();

			// use next free slot (prevent wrong type)
			for (int deviceId = 0; deviceId < deviceCount; deviceId++) {
				String serialNo = serialNumbers[deviceId];

				if (!isSerialNumAlreadyUsed(serialNo) && chipModel == deviceMap.get(serialNo)) {
					return new SimpleEntry<>(deviceId, chipModel);
				}
			}
			// Nothing matched? Use next free slot (no matter what type)
			for (int deviceId = 0; deviceId < deviceCount; deviceId++) {
				String serialNo = serialNumbers[deviceId];

				if (!isSerialNumAlreadyUsed(serialNo) && deviceMap.get(serialNo) != null) {
					return new SimpleEntry<>(deviceId, deviceMap.get(serialNo));
				}
			}
		} else {
			// TEST: Choose one specific device for sound output

			for (int deviceId = 0; deviceId < deviceCount; deviceId++) {
				if (Objects.equals(serialNumbers[deviceId], sidBlasterSerialNumber)) {
					return new SimpleEntry<Integer, ChipModel>(deviceId, chipModel);
				}
			}
		}
		// no slot left
		return new SimpleEntry<>(null, null);
	}

	private boolean isSerialNumAlreadyUsed(String serialNo) {
		return sids.stream().filter(sid -> Objects.equals(sid.getDeviceName(), serialNo)).findFirst().isPresent();
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
