package builder.jexsid;

import static libsidplay.components.pla.PLA.MAX_SIDS;

import java.util.ArrayList;
import java.util.List;

import exsid.ExSID;
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

/**
 * 
 * Support of ExSID mini USB devices.
 *
 * @author Ken Händel
 *
 */
public class JExSIDBuilder implements HardwareSIDBuilder, Mixer {

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
	private static ExSID exSID;

	/**
	 * Number of ExSID devices.
	 */
	private static int deviceCount;

	/**
	 * Device names of ExSID devices.
	 */
	private static String[] deviceNames;

	/**
	 * Already used ExSID SIDs.
	 */
	private List<ExSIDEmu> sids = new ArrayList<>();

	private long lastSIDWriteTime;

	private int fastForwardFactor;

	private int[] delayInCycles = new int[MAX_SIDS];

	protected int lastSidNum = -1;

	public JExSIDBuilder(EventScheduler context, IConfig config, CPUClock cpuClock) {
		this.context = context;
		this.config = config;
		this.cpuClock = cpuClock;
		if (exSID == null) {
			exSID = new ExSID();
			init();
		}
	}

	private void init() {
		Runtime.getRuntime().addShutdownHook(new Thread(() -> exSID.exSID_exit()));

		if (exSID.exSID_init() < 0) {
			deviceNames = new String[0];
			return;
		}
		final String hardwareRevision = HardwareModel.get(exSID.exSID_hwmodel()).name();
		final String firmwareVersion = String.format(" fw%c%d", (exSID.exSID_hwversion() >> 8) & 0xff,
				exSID.exSID_hwversion() & 0xff);
		deviceCount = 2;
		deviceNames = new String[deviceCount];
		deviceNames[0] = hardwareRevision + " " + firmwareVersion + " #1";
		deviceNames[1] = hardwareRevision + " " + firmwareVersion + " #2";
	}

	public static void printInstallationHint() {
		if (OS.get() == OS.LINUX) {
			printLinuxInstallationHint();
		}
		System.err.println("Maybe you just forgot to plug-in your USB devices?");
		System.err.println();
	}

	private static void printLinuxInstallationHint() {
		System.err
				.println("If device cannot be used, please use this workaround (we grant access to the USB device)...");
		System.err.println();
		System.err.println("... On Ubuntu you do:");
		System.err.println("$ sudo vi /etc/udev/rules.d/93-exsid.rules");
		System.err.println(
				"ACTION==\"add\", ATTRS{idVendor}==\"0403\", ATTRS{idProduct}==\"6001\", MODE=\"0666\", RUN+=\"/bin/sh -c 'rmmod ftdi_sio && rmmod usbserial'\"");
		System.err.println("$ sudo udevadm control --reload-rules && sudo udevadm trigger");
		System.err.println("For ExSID+ you have to replace idProduct 6001 with 6015!");
		System.err.println();
		System.err.println("... and on Fedora you do:");
		System.err.println("$ sudo vi /etc/udev/rules.d/93-exsid.rules");
		System.err.println(
				"ACTION==\"add\", ATTRS{idVendor}==\"0403\", ATTRS{idProduct}==\"6001\", MODE=\"0666\", RUN+=\"/bin/sh -c 'echo -n $id:1.0 > /sys/bus/usb/drivers/ftdi_sio/unbind; echo -n $id:1.1 > /sys/bus/usb/drivers/ftdi_sio/unbind'\"");
		System.err.println("For ExSID+ you have to replace idProduct 6001 with 6015!");
	}

	@Override
	public SIDEmu lock(SIDEmu oldSIDEmu, int sidNum, SidTune tune) {
		IAudioSection audioSection = config.getAudioSection();
		IEmulationSection emulationSection = config.getEmulationSection();
		ChipModel chipModel = ChipModel.getChipModel(emulationSection, tune, sidNum);
		ChipModel defaultSidModel = emulationSection.getDefaultSidModel();
		boolean stereo = SidTune.isSIDUsed(emulationSection, tune, 1);

		if (oldSIDEmu != null) {
			// always re-use hardware SID chips, if configuration changes
			// the purpose is to ignore chip model changes!
			return oldSIDEmu;
		}

		// Use exSID fake stereo (simultaneous write to both chips -> no delay possible)
		// or
		// JSIDPlay2 fake stereo (address both chips separately -> shared bandwidth)!
		if (emulationSection.isExsidFakeStereo()) {
			if (sidNum == 0 && SidTune.isFakeStereoSid(emulationSection, tune, 1)) {
				lastSidNum = sidNum;
				exSID.exSID_chipselect(ChipSelect.XS_CS_BOTH.getChipSelect());
			}
			if (sidNum == 1 && SidTune.isFakeStereoSid(emulationSection, tune, 1)) {
				return SIDEmu.NONE;
			}
		}

		// stereo SIDs with same chipmodel must be forced to use a different device,
		// therefore:
		if (sidNum == 1 && sids.get(0).getChipModel() == chipModel) {
			chipModel = chipModel == ChipModel.MOS6581 ? ChipModel.MOS8580 : ChipModel.MOS6581;
		}
		Integer deviceId = sidNum;

		if (deviceId < deviceCount) {
			ExSIDEmu sid = createSID(deviceId.byteValue(), sidNum, tune, chipModel, defaultSidModel, stereo);

			if (sid.lock()) {
				sid.setFilterEnable(emulationSection, sidNum);
				sid.setDigiBoost(emulationSection.isDigiBoosted8580());
				for (int voice = 0; voice < 4; voice++) {
					sid.setVoiceMute(voice, emulationSection.isMuteVoice(sidNum, voice));
				}
				sids.add(sid);
				setDeviceName(sidNum, deviceNames[deviceId]);
				setDelay(sidNum, audioSection.getDelay(sidNum));
				return sid;
			}
		}
		System.err.printf("EXSID ERROR: System doesn't have enough SID chips. Requested: (sidNum=%d)\n", sidNum);
		if (deviceCount == 0) {
			printInstallationHint();
		}
		return SIDEmu.NONE;
	}

	@Override
	public void unlock(SIDEmu sidEmu) {
		ExSIDEmu sid = (ExSIDEmu) sidEmu;
		sids.remove(sid);
		sid.unlock();
	}

	@Override
	public int getDeviceCount() {
		return deviceCount;
	}

	public static String[] getDeviceNames() {
		return deviceNames;
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
		System.err.println("Fade-in unsupported by ExSID");
		// XXX unsupported by ExSID
	}

	@Override
	public void fadeOut(double fadeOut) {
		System.err.println("Fade-out unsupported by ExSID");
		// XXX unsupported by ExSID
	}

	@Override
	public void setVolume(int sidNum, float volume) {
		System.err.println("Volume unsupported by ExSID");
		// XXX unsupported by ExSID
	}

	@Override
	public void setBalance(int sidNum, float balance) {
		System.err.println("Balance unsupported by ExSID");
		// XXX unsupported by ExSID
	}

	public int getDelay(int sidNum) {
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
	}

	private ExSIDEmu createSID(byte deviceId, int sidNum, SidTune tune, ChipModel chipModel, ChipModel defaultChipModel,
			boolean stereo) {
		final IEmulationSection emulationSection = config.getEmulationSection();

		if (SidTune.isFakeStereoSid(emulationSection, tune, sidNum)) {
			return new ExSIDEmu.FakeStereo(this, context, cpuClock, exSID, deviceId, sidNum, chipModel,
					defaultChipModel, stereo, sids, emulationSection);
		} else {
			return new ExSIDEmu(this, context, cpuClock, exSID, deviceId, sidNum, chipModel, defaultChipModel, stereo);
		}
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
			exSID.exSID_delay((short) (REGULAR_DELAY >> fastForwardFactor));
		}
		return REGULAR_DELAY;
	}
}
