package builder.jexsid;

import static libsidplay.components.pla.PLA.MAX_SIDS;

import java.util.List;
import java.util.Objects;

import builder.resid.residfp.ReSIDfp;
import exsid.ExSID;
import libsidplay.common.CPUClock;
import libsidplay.common.ChipModel;
import libsidplay.common.Event;
import libsidplay.common.EventScheduler;
import libsidplay.config.IEmulationSection;

/**
 *
 * @author Ken Händel
 *
 */
public class ExSIDEmu extends ReSIDfp {

	/**
	 * FakeStereo mode uses two chips using the same base address. Write commands
	 * are routed two both SIDs, while read command can be configured to be
	 * processed by a specific SID chip.
	 *
	 * @author ken
	 *
	 */
	public static class FakeStereo extends ExSIDEmu {
		private final IEmulationSection emulationSection;
		private final int prevNum;
		private final List<ExSIDEmu> sids;

		public FakeStereo(JExSIDBuilder jExSIDBuilder, EventScheduler context, CPUClock cpuClock, ExSID hardSID,
				byte deviceId, int sidNum, ChipModel model, ChipModel defaultChipModel, boolean stereo,
				List<ExSIDEmu> sids, IEmulationSection emulationSection) {
			super(jExSIDBuilder, context, cpuClock, hardSID, deviceId, sidNum, model, defaultChipModel, stereo);
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

	private final JExSIDBuilder jExSIDBuilder;

	private final Event event;

	private final ExSID exSID;

	private final byte deviceID;

	private String deviceName;

	private int sidNum;

	private final ChipModel chipModel;

	private boolean[] voiceMute = new boolean[4];

	private boolean[] filterDisable = new boolean[MAX_SIDS];

	public ExSIDEmu(JExSIDBuilder jExSIDBuilder, EventScheduler context, CPUClock cpuClock, ExSID exSID, byte deviceId,
			int sidNum, ChipModel model, ChipModel defaultSidModel, boolean stereo) {
		super(context);
		this.jExSIDBuilder = jExSIDBuilder;
		this.context = context;
		this.exSID = exSID;
		this.deviceID = deviceId;
		this.sidNum = sidNum;
		this.chipModel = model;
		this.event = Event.of("ExSID Delay",
				event -> context.schedule(event, jExSIDBuilder.eventuallyDelay(), Event.Phase.PHI2));

		super.setChipModel(model);
		super.setClockFrequency(cpuClock.getCpuFrequency());

		if (sidNum == 0) {
			exSID.exSID_audio_op(AudioOp.XS_AU_MUTE.getAudioOp());
			exSID.exSID_clockselect(cpuClock == CPUClock.PAL ? ClockSelect.XS_CL_PAL.getClockSelect()
					: ClockSelect.XS_CL_NTSC.getClockSelect());
			if (stereo) {
				exSID.exSID_audio_op(model == ChipModel.MOS6581 ? AudioOp.XS_AU_6581_8580.getAudioOp()
						: AudioOp.XS_AU_8580_6581.getAudioOp());
			} else {
				exSID.exSID_audio_op(model == ChipModel.MOS6581 ? AudioOp.XS_AU_6581_6581.getAudioOp()
						: AudioOp.XS_AU_8580_8580.getAudioOp());
			}
			exSID.exSID_audio_op(AudioOp.XS_AU_UNMUTE.getAudioOp());
		}
	}

	@Override
	public void write(int addr, byte data) {
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
		if (addr > 0x18) {
			// "Ragga Run.sid" denies to work!
			return;
		}
		doWriteDelayed(() -> {
			if (!Objects.equals(jExSIDBuilder.lastSidNum, sidNum)) {
				exSID.exSID_chipselect(chipModel == ChipModel.MOS8580 ? ChipSelect.XS_CS_CHIP1.getChipSelect()
						: ChipSelect.XS_CS_CHIP0.getChipSelect());
				jExSIDBuilder.lastSidNum = sidNum;
			}
			exSID.exSID_clkdwrite(0, (byte) addr, dataByte);
		});
	}

	@Override
	public void clock() {
		super.clock();
		final short clocksSinceLastAccess = (short) jExSIDBuilder.clocksSinceLastAccess();

		doWriteDelayed(() -> exSID.exSID_delay(clocksSinceLastAccess));
	}

	private void doWriteDelayed(Runnable runnable) {
		if (jExSIDBuilder.getDelay(sidNum) > 0) {
			context.schedule(Event.of("Delayed SID output", event -> runnable.run()), jExSIDBuilder.getDelay(sidNum));
		} else {
			runnable.run();
		}
	}

	protected boolean lock() {
		exSID.exSID_reset((byte) 0x0f);
		try {
			// Reset needs some time to complete, without this stereo tunes hung up the cart
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		reset((byte) 0xf);
		context.schedule(event, 0, Event.Phase.PHI2);
		return true;
	}

	protected void unlock() {
		exSID.exSID_reset((byte) 0);
		try {
			// Reset needs some time to complete, without this stereo tunes hung up the cart
			Thread.sleep(50);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		reset((byte) 0x0);
		context.cancel(event);
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
		credits.append("ExSID Java version by Ken Händel <kschwiersch@yahoo.de> Copyright (©) 2021\n");
		credits.append("\tHardware and driver code by Thibaut Thezan\n");
		credits.append("\thttp://hacks.slashdirt.org/hw/exsid/\n");
		return credits.toString();
	}

}
