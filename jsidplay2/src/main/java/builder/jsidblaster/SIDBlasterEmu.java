package builder.jsidblaster;

import static libsidplay.components.pla.PLA.MAX_SIDS;

import java.util.List;

import builder.resid.residfp.ReSIDfp;
import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.config.IEmulationSection;
import sidblaster.hardsid.HardSID;
import sidblaster.hardsid.WState;

/**
 *
 * @author Ken Händel
 *
 */
public class SIDBlasterEmu extends ReSIDfp {

	/**
	 * FakeStereo mode uses two chips using the same base address. Write commands
	 * are routed two both SIDs, while read command can be configured to be
	 * processed by a specific SID chip.
	 *
	 * @author ken
	 *
	 */
	public static class FakeStereo extends SIDBlasterEmu {
		private final IEmulationSection emulationSection;
		private final int prevNum;
		private final List<SIDBlasterEmu> sids;

		public FakeStereo(JSIDBlasterBuilder hardSIDBuilder, EventScheduler context, CPUClock cpuClock, HardSID hardSID,
				byte deviceId, int sidNum, ChipModel model, ChipModel defaultChipModel, List<SIDBlasterEmu> sids,
				IEmulationSection emulationSection) {
			super(hardSIDBuilder, context, cpuClock, hardSID, deviceId, sidNum, model, defaultChipModel);
			this.prevNum = sidNum - 1;
			this.sids = sids;
			this.emulationSection = emulationSection;
		}

		@Override
		public byte read(int addr) {
			if (emulationSection.getSidToRead().getSidNum() <= prevNum) {
				return sids.get(prevNum).read(addr);
			}
			return super.read(addr);
		}

		@Override
		public byte readInternalRegister(int addr) {
			if (emulationSection.getSidToRead().getSidNum() <= prevNum) {
				return sids.get(prevNum).readInternalRegister(addr);
			}
			return super.readInternalRegister(addr);
		}

		@Override
		public void write(int addr, byte data) {
			super.write(addr, data);
			sids.get(prevNum).write(addr, data);
		}
	}

	private final EventScheduler context;

	private final JSIDBlasterBuilder hardSIDBuilder;

	private final Event event;

	private final HardSID hardSID;

	private final byte deviceID;

	private String deviceName;

	private int sidNum;

	private final ChipModel chipModel;

	private boolean[] voiceMute = new boolean[4];

	private boolean[] filterDisable = new boolean[MAX_SIDS];

	public SIDBlasterEmu(JSIDBlasterBuilder hardSIDBuilder, EventScheduler context, CPUClock cpuClock, HardSID hardSID,
			byte deviceId, int sidNum, ChipModel model, ChipModel defaultSidModel) {
		super(context);
		this.hardSIDBuilder = hardSIDBuilder;
		this.context = context;
		this.hardSID = hardSID;
		this.deviceID = deviceId;
		this.sidNum = sidNum;
		this.chipModel = model;
		this.event = Event.of("HardSID Delay",
				event -> context.schedule(event, hardSIDBuilder.eventuallyDelay(), Event.Phase.PHI2));
		super.setChipModel(model == ChipModel.AUTO ? defaultSidModel : model);
		super.setClockFrequency(cpuClock.getCpuFrequency());
	}

	@Override
	public byte read(int addr) {
		byte emulatedRead = super.read(addr);
		if (hardSIDBuilder.isSidBlasterRead()) {
			final short clocksSinceLastAccess = (short) hardSIDBuilder.clocksSinceLastAccess();
			return hardSID.HardSID_Read(deviceID, clocksSinceLastAccess, (byte) addr);
		}
		return emulatedRead;
	}

	@Override
	public void write(int addr, byte data) {
		final short clocksSinceLastAccess = (short) hardSIDBuilder.clocksSinceLastAccess();
		switch (addr & 0x1f) {
		case 0x04:
		case 0x0b:
		case 0x12:
			if (voiceMute[(addr - 4) / 7]) {
				data &= 0xfe;
			}
			super.write(addr, data);
			break;
		case 0x17:
			if (filterDisable[sidNum]) {
				data &= 0xf0;
			}
			super.write(addr, data);
			break;
		case 0x18:
			// samples muted? Fade-in is allowed anyway
			if (voiceMute[3] && (data & 0xf) < (readInternalRegister(addr) & 0xf)) {
				return;
			}
			super.write(addr, data);
			break;

		default:
			super.write(addr, data);
			break;
		}
		final byte dataByte = data;
		doWriteDelayed(() -> {
			while (hardSID.HardSID_Try_Write(deviceID, clocksSinceLastAccess, (byte) addr, dataByte) == WState.BUSY)
				;
		});
	}

	private void doWriteDelayed(Runnable runnable) {
		if (hardSIDBuilder.getDelay(sidNum) > 0) {
			context.schedule(Event.of("Delayed SID output", event -> runnable.run()), hardSIDBuilder.getDelay(sidNum));
		} else {
			runnable.run();
		}
	}

	protected boolean lock() {
		boolean locked = hardSID.HardSID_Lock(deviceID);
		if (locked) {
			hardSID.HardSID_Reset(deviceID);
			reset((byte) 0xf);
			context.schedule(event, 0, Event.Phase.PHI2);
		}
		return locked;
	}

	protected void unlock() {
		hardSID.HardSID_Reset(deviceID);
		reset((byte) 0x0);
		context.cancel(event);
		hardSID.HardSID_Unlock(deviceID);
	}

	@Override
	public void setVoiceMute(int num, boolean mute) {
		super.setVoiceMute(num, mute);
		if (num < 4) {
			voiceMute[num] = mute;
		}
	}

	@Override
	public void setFilterEnable(IEmulationSection emulation, int sidNum) {
		super.setFilterEnable(emulation, sidNum);
		filterDisable[sidNum] = !emulation.isFilterEnable(sidNum);
	}

	public byte getDeviceId() {
		return deviceID;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public void setDeviceName(String deviceName) {
		this.deviceName = deviceName;
	}

	protected ChipModel getChipModel() {
		return chipModel;
	}

	public static final String credits() {
		final StringBuffer credits = new StringBuffer();
		credits.append("SIDBlaster Java version by Ken Händel <kschwiersch@yahoo.de> Copyright (©) 2020\n");
		credits.append("\tSupported by SIDBlaster-USB TicTac Edition (Andreas Schumm)\n");
		credits.append("\thttp://crazy-midi.de\n");
		credits.append("\tBased on SIDBlaster-USB by Davey (Das Phantom)\n");
		credits.append("\tDLL created by Stein Pedersen\n");
		credits.append("\tSIDBlaster test song by Hannes Malecki (Honey) of Welle: Erdball\n");
		return credits.toString();
	}

}
