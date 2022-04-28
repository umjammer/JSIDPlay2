package builder.jhardsid;

import static libsidplay.components.pla.PLA.MAX_SIDS;

import java.util.ArrayList;
import java.util.List;

import com.hardsid.usb.driver.HardSIDUSB;
import com.hardsid.usb.driver.OS;
import com.hardsid.usb.driver.SysMode;
import com.hardsid.usb.driver.WState;

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
 * Support of HARDSID USB devices like HardSID Uno, HardSID UPlay and HardSID4U.
 * 
 * @author Ken Händel
 *
 */
public class JHardSIDBuilder implements HardwareSIDBuilder, Mixer {

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

	private final HardSIDUSB hardSID;

	/**
	 * Number of HardSID devices.
	 */
	private static int deviceCount;

	/**
	 * Device names of HardSID devices.
	 */
	private static String[] deviceNames;

	/**
	 * Number of SIDs of the first HardSID device.
	 */
	private static int chipCount;

	/**
	 * Device number. If more devices are connected, we use just the first one.
	 */
	private byte deviceID;

	/**
	 * Already used HardSIDs.
	 */
	private List<JHardSIDEmu> sids = new ArrayList<>();

	private long lastSIDWriteTime;

	private int fastForwardFactor;

	private int[] delayInCycles = new int[MAX_SIDS];

	public JHardSIDBuilder(EventScheduler context, IConfig config, CPUClock cpuClock) {
		this.context = context;
		this.config = config;
		this.cpuClock = cpuClock;
		hardSID = new HardSIDUSB();
		hardSID.hardsid_usb_init(true, SysMode.SIDPLAY);
		deviceCount = hardSID.hardsid_usb_getdevcount();
		chipCount = hardSID.hardsid_usb_getsidcount(deviceID);
		deviceNames = new String[chipCount];
		for (int i = 1; i <= chipCount; i++) {
			deviceNames[i - 1] = "HardSID #" + i;
		}
	}

	@Override
	public void destroy() {
		hardSID.hardsid_usb_abortplay(deviceID);
		hardSID.hardsid_usb_close();
	}

	@Override
	public SIDEmu lock(SIDEmu oldHardSID, int sidNum, SidTune tune) {
		IAudioSection audioSection = config.getAudioSection();
		IEmulationSection emulationSection = config.getEmulationSection();
		ChipModel chipModel = ChipModel.getChipModel(emulationSection, tune, sidNum);
		ChipModel defaultSidModel = emulationSection.getDefaultSidModel();

		Integer chipNum = getModelDependantChipNum(chipModel);

		if (oldHardSID != null) {
			// always re-use hardware SID chips, if configuration changes
			// the purpose is to ignore chip model changes!
			return oldHardSID;
		}
		if (deviceID < deviceCount && chipNum != null && chipNum < chipCount) {
			JHardSIDEmu sid = createSID(deviceID, chipNum, sidNum, tune, chipModel, defaultSidModel);

			sid.lock();
			sid.setFilterEnable(emulationSection, sidNum);
			sid.setDigiBoost(emulationSection.isDigiBoosted8580());
			for (int voice = 0; voice < 4; voice++) {
				sid.setVoiceMute(voice, emulationSection.isMuteVoice(sidNum, voice));
			}
			sids.add(sid);
			setDeviceName(sidNum, deviceNames[chipNum]);
			setDelay(sidNum, audioSection.getDelay(sidNum));
			return sid;
		}
		System.err.printf("HARDSID ERROR: System doesn't have enough SID chips. Requested: (sidNum=%d)\n", sidNum);

		if (deviceCount == 0) {
			printInstallationHint();
		}
		return SIDEmu.NONE;
	}

	public static void printInstallationHint() {
		if (OS.get() == OS.WINDOWS) {
			printWindowsInstallationHint();
		} else if (OS.get() == OS.LINUX) {
			printLinuxInstallationHint();
		} else if (OS.get() == OS.MAC) {
			printMacInstallationHint();
		}
	}

	private static void printLinuxInstallationHint() {
		System.err.println("\"To give proper permissions, please type the following commands:\"");
		System.err.println("sudo vi /etc/udev/rules.d/hardsid4u.rules");
		System.err.println("\"Now, add the following single line:\"");
		System.err.println(
				"SUBSYSTEM==\"usb\",ATTR{idVendor}==\"6581\",ATTR{idProduct}==\"8580\",MODE=\"0660\",GROUP=\"plugdev\"");
		System.err.println("\"And finally type this command to refresh device configuration:\"");
		System.err.println("sudo udevadm trigger");
		System.err.println("You are ready to start :-)");
	}

	private static void printMacInstallationHint() {
		System.err.println("Unknown things to do... N.Y.T");
		System.err.println("This should work out-of-the box!? Is a different device driver loaded?");
	}

	private static void printWindowsInstallationHint() {
		System.err.println(
				"Go to \"Control Panel / Hardware / Device Manager\" and uninstall previous HardSID driver, and then reboot!");
		System.err.println("Now install Zadigs USB driver installation from that web-site: https://zadig.akeo.ie/");
		System.err.println(
				"Click install for device (658x 8580) and WinUSB, where x=1-3. These settings were already proposed by the installer for me.");
		System.err.println("You are ready to start :-)");
	}

	@Override
	public void unlock(final SIDEmu sidEmu) {
		JHardSIDEmu hardSid = (JHardSIDEmu) sidEmu;
		hardSid.unlock();
		sids.remove(sidEmu);
	}

	@Override
	public int getDeviceCount() {
		return chipCount;
	}

	public static String[] getDeviceNames() {
		return deviceNames;
	}

	@Override
	public Integer getDeviceId(int sidNum) {
		return sidNum < sids.size() ? Integer.valueOf(sids.get(sidNum).getChipNum()) : null;
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
	public void setAudioDriver(AudioDriver audioDriver) {
	}

	@Override
	public void start() {
	}

	@Override
	public void fadeIn(double fadeIn) {
		System.err.println("Fade-in unsupported by HardSID");
		// XXX unsupported by HardSID
	}

	@Override
	public void fadeOut(double fadeOut) {
		System.err.println("Fade-out unsupported by HardSID");
		// XXX unsupported by HardSID
	}

	@Override
	public void setVolume(int sidNum, float volume) {
		System.err.println("Volume unsupported by HardSID");
		// XXX unsupported by HardSID
	}

	@Override
	public void setBalance(int sidNum, float balance) {
		System.err.println("Balance unsupported by HardSID");
		// XXX unsupported by HardSID
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
		while (hardSID.hardsid_usb_flush(deviceID) == WState.BUSY) {
			try {
				Thread.sleep(0);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private JHardSIDEmu createSID(byte deviceId, int chipNum, int sidNum, SidTune tune, ChipModel chipModel,
			ChipModel defaultChipModel) {
		final IEmulationSection emulationSection = config.getEmulationSection();

		if (SidTune.isFakeStereoSid(emulationSection, tune, sidNum)) {
			return new JHardSIDEmu.FakeStereo(this, context, cpuClock, hardSID, deviceId, chipNum, sidNum, chipModel,
					defaultChipModel, sids, emulationSection);
		} else {
			return new JHardSIDEmu(this, context, cpuClock, hardSID, deviceId, chipNum, sidNum, chipModel,
					defaultChipModel);
		}
	}

	/**
	 * Get HardSID device index based on the desired chip model.
	 *
	 * @param chipModel desired chip model
	 * @return SID index of the desired HardSID device
	 */
	private Integer getModelDependantChipNum(final ChipModel chipModel) {
		int sid6581 = config.getEmulationSection().getHardsid6581();
		int sid8580 = config.getEmulationSection().getHardsid8580();

		// use next free slot (prevent wrong type)
		for (int chipNum = 0; chipNum < chipCount; chipNum++) {
			if (!isChipNumAlreadyUsed(chipNum) && isChipModelMatching(chipModel, chipNum)) {
				return chipNum;
			}
		}
		// Nothing matched? use next free slot
		for (int chipNum = 0; chipNum < chipCount; chipNum++) {
			if (chipCount > 2 && (chipNum == sid6581 || chipNum == sid8580)) {
				// more SIDs available than configured? still skip wrong type
				continue;
			}
			if (!isChipNumAlreadyUsed(chipNum)) {
				return chipNum;
			}
		}
		// no slot left
		return null;
	}

	private boolean isChipModelMatching(final ChipModel chipModel, int chipNum) {
		int sid6581 = config.getEmulationSection().getHardsid6581();
		int sid8580 = config.getEmulationSection().getHardsid8580();

		return chipNum == sid6581 && chipModel == ChipModel.MOS6581
				|| chipNum == sid8580 && chipModel == ChipModel.MOS8580;
	}

	private boolean isChipNumAlreadyUsed(final int chipNum) {
		return sids.stream().filter(sid -> chipNum == sid.getChipNum()).findFirst().isPresent();
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

			while (hardSID.hardsid_usb_delay(deviceID, REGULAR_DELAY >> fastForwardFactor) == WState.BUSY) {
				try {
					Thread.sleep(0);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
		return REGULAR_DELAY;
	}

}
