package sidblaster.hardsid;

import com.ftdi.FTD2XXException;

import sidblaster.Command;
import sidblaster.CommandEnum;
import sidblaster.ICommandDispatcher;
import sidblaster.ISIDBlaster;
import sidblaster.SIDType;
import sidblaster.async.AsyncDispatcher;
import sidblaster.d2xx.D2XXManager;

public class HardSIDImpl implements HardSID {

	private final static short HARDSID_VERSION = 0x0203;

	private ICommandDispatcher g_CommandDispatcher = new AsyncDispatcher();
//	private ICommandDispatcher g_CommandDispatcher = new SyncDispatcher();

	private D2XXManager x_Manager = D2XXManager.getInstance();

	private long startTime, c64Time;

	public HardSIDImpl() {
		try {
			g_CommandDispatcher.initialize();
		} catch (FTD2XXException e) {
			e.printStackTrace();
		}
	}

	@Override
	public short HardSID_Version() {
		return HARDSID_VERSION;
	}

	@Override
	public byte HardSID_Devices() {
		try {
			return (byte) g_CommandDispatcher.deviceCount();
		} catch (FTD2XXException e) {
			e.printStackTrace();
			return 0;
		}
	}

	@Override
	public void HardSID_Delay(byte DeviceID, short Cycles) {
		c64Time += Cycles & 0xffff;
		Command cmd = new Command(DeviceID, CommandEnum.Delay, (byte) 0, (byte) 0,
				startTime + (long) (c64Time * 1000. / ISIDBlaster.PAL_CLOCK));

		try {
			g_CommandDispatcher.sendCommand(cmd);
		} catch (FTD2XXException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void HardSID_Write(byte DeviceID, short Cycles, byte SID_reg, byte data) {
		c64Time += Cycles & 0xffff;
		Command cmd = new Command(DeviceID, CommandEnum.Write, SID_reg, data,
				startTime + (long) (c64Time * 1000. / ISIDBlaster.PAL_CLOCK));
		try {
			while (g_CommandDispatcher.sendCommand(cmd) != 0) {
				Thread.yield();
			}
		} catch (FTD2XXException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public byte HardSID_Read(byte DeviceID, short Cycles, byte SID_reg) {
		c64Time += Cycles & 0xffff;
		Command cmd = new Command(DeviceID, CommandEnum.Read, SID_reg, (byte) 0,
				startTime + (long) (c64Time * 1000. / ISIDBlaster.PAL_CLOCK));
		int result;
		try {
			result = g_CommandDispatcher.sendCommand(cmd);
			if (result != 0) {
				return (byte) (result >> 8);
			} else {
				return 0;
			}
		} catch (FTD2XXException | InterruptedException e) {
			e.printStackTrace();
			return 0;
		}
	}

	@Override
	public void HardSID_Flush(byte DeviceID) {
		Command cmd = new Command(DeviceID, CommandEnum.Flush);
		try {
			g_CommandDispatcher.sendCommand(cmd);
		} catch (FTD2XXException | InterruptedException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void HardSID_SoftFlush(byte DeviceID) {
		Command cmd = new Command(DeviceID, CommandEnum.SoftFlush);
		try {
			g_CommandDispatcher.sendCommand(cmd);
		} catch (FTD2XXException | InterruptedException e) {
			e.printStackTrace();
		}

	}

	@Override
	public boolean HardSID_Lock(byte DeviceID) {
		Command cmd = new Command(DeviceID, CommandEnum.Lock);
		try {
			g_CommandDispatcher.sendCommand(cmd);
			return true;
		} catch (FTD2XXException | InterruptedException e) {
			e.printStackTrace();
			return false;
		}

	}

	@Override
	public void HardSID_Filter(byte DeviceID, boolean filter) {
		Command cmd = new Command(DeviceID, CommandEnum.Filter, (byte) 0, (byte) (filter ? 1 : 0));
		try {
			g_CommandDispatcher.sendCommand(cmd);
		} catch (FTD2XXException | InterruptedException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void HardSID_Reset(byte DeviceID) {
		this.c64Time = 0;
		this.startTime = System.currentTimeMillis();

		Command cmd = new Command(DeviceID, CommandEnum.Reset);
		try {
			g_CommandDispatcher.sendCommand(cmd);
		} catch (FTD2XXException | InterruptedException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void HardSID_Sync(byte DeviceID) {
		Command cmd = new Command(DeviceID, CommandEnum.Sync);
		try {
			g_CommandDispatcher.sendCommand(cmd);
		} catch (FTD2XXException | InterruptedException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void HardSID_Mute(byte DeviceID, byte voice, boolean mute) {
		if (mute) {
			Command cmd = new Command(DeviceID, CommandEnum.MuteAll, (byte) 0, voice);
			try {
				g_CommandDispatcher.sendCommand(cmd);
			} catch (FTD2XXException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void HardSID_MuteAll(byte DeviceID, boolean mute) {
		if (mute) {
			Command cmd = new Command(DeviceID, CommandEnum.MuteAll);
			try {
				g_CommandDispatcher.sendCommand(cmd);
			} catch (FTD2XXException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public void InitHardSID_Mapper() {

	}

	@Override
	public byte GetHardSIDCount() {
		return HardSID_Devices();
	}

	@Override
	public void WriteToHardSID(byte DeviceID, byte SID_reg, byte data) {
		HardSID_Write(DeviceID, (short) 0, SID_reg, data);
	}

	@Override
	public byte ReadFromHardSID(byte DeviceID, byte SID_reg) {
		Command cmd = new Command(DeviceID, CommandEnum.Read, SID_reg, (byte) 0);
		try {
			return (byte) g_CommandDispatcher.sendCommand(cmd);
		} catch (FTD2XXException | InterruptedException e) {
			e.printStackTrace();
			return 0;
		}

	}

	@Override
	public void MuteHardSID_Line(boolean mute) {

	}

	@Override
	public void HardSID_Reset2(byte DeviceID, byte volume) {

	}

	@Override
	public void HardSID_Unlock(byte DeviceID) {
		Command cmd = new Command(DeviceID, CommandEnum.Unlock);
		try {
			g_CommandDispatcher.sendCommand(cmd);
		} catch (FTD2XXException | InterruptedException e) {
			e.printStackTrace();
		}

	}

	@Override
	public WState HardSID_Try_Write(byte DeviceID, short Cycles, byte SID_reg, byte data) {
		c64Time += Cycles & 0xffff;
		Command cmd = new Command(DeviceID, CommandEnum.Write, SID_reg, data,
				startTime + (long) (c64Time * 1000. / ISIDBlaster.PAL_CLOCK));
		try {
			if (g_CommandDispatcher.sendCommand(cmd) == 0) {
				return WState.OK;
			} else {
				return WState.BUSY;
			}
		} catch (FTD2XXException | InterruptedException e) {
			e.printStackTrace();
			return WState.ERROR;
		}

	}

	@Override
	public String HardSID_GetSerial(byte DeviceID) {
		return x_Manager.GetSerialNo(DeviceID);
	}

	@Override
	public void HardSID_SetWriteBufferSize(byte bufferSize) {
		g_CommandDispatcher.setWriteBufferSize(bufferSize);
	}

	@Override
	public SIDType HardSID_GetSIDType(byte DeviceID) {
		return x_Manager.GetSIDType(DeviceID);
	}

	@Override
	public void HardSID_Uninitialize() {
		try {
			g_CommandDispatcher.uninitialize();
		} catch (FTD2XXException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setDebug(boolean enabled) {
		throw new RuntimeException("N.Y.I.");
	}

	@Override
	public short GetDLLVersion() {
		return HARDSID_VERSION;
	}

	@Override
	public void MuteHardSID(byte deviceID, byte channel, boolean mute) {
		throw new RuntimeException("N.Y.I.");
	}

	@Override
	public void MuteHardSIDAll(byte deviceID, boolean mute) {
		throw new RuntimeException("N.Y.I.");
	}

	@Override
	public boolean HardSID_Group(byte deviceID, boolean enable, byte groupID) {
		throw new RuntimeException("N.Y.I.");
	}

	@Override
	public void HardSID_Mute2(byte DeviceID, byte channel, boolean mute, boolean manual) {
		throw new RuntimeException("N.Y.I.");
	}

	@Override
	public void HardSID_OtherHardware() {
		throw new RuntimeException("N.Y.I.");
	}

	@Override
	public short HardSID_Clock(byte DeviceID, byte preset) {
		throw new RuntimeException("N.Y.I.");
	}

	@Override
	public int HardSID_SetSIDType(byte DeviceID, SIDType sidType) {
		try {
			x_Manager.SetSIDType(DeviceID, sidType);
			return 0;
		} catch (FTD2XXException | InterruptedException e) {
			e.printStackTrace();
			return 1;
		}
	}

	@Override
	public int HardSID_SetSerial(byte DeviceID, String serialNo) {
		try {
			x_Manager.SetSerialNo(DeviceID, serialNo);
			return 0;
		} catch (FTD2XXException | InterruptedException e) {
			e.printStackTrace();
			return 1;
		}
	}

}
