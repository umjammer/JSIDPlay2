package sidblaster.d2xx;

import java.util.ArrayList;
import java.util.List;

import com.ftdi.FTD2XXException;
import com.ftdi.FTDevice;

import sidblaster.ISIDBlaster;
import sidblaster.ISIDBlasterEnumerator;
import sidblaster.SIDBlaster;

public class SIDBlasterEnumerator implements ISIDBlasterEnumerator {

	private List<FTDevice> devices;
	private List<Boolean> devicesAllocated;

	private static SIDBlasterEnumerator INSTANCE = new SIDBlasterEnumerator();

	public static SIDBlasterEnumerator getInstance() {
		return INSTANCE;
	}

	@Override
	public int deviceCount() throws FTD2XXException {
		D2XXManager manager = D2XXManager.getInstance();
		manager.rescan();
		devices = new ArrayList<>();
		devicesAllocated = new ArrayList<>();
		for (int i = 0; i < manager.count(); ++i) {
			devices.add(manager.getDevice(i));
			devicesAllocated.add(false);
		}
		return devices.size();
	}

	@Override
	public ISIDBlaster createInterface(int deviceID) {
		if (deviceID < devices.size()) {
			assert (!devicesAllocated.get(deviceID));
			FTDevice sid = devices.get(deviceID);
			devicesAllocated.set(deviceID, true);
			return new SIDBlaster(deviceID, sid);
		}
		return null;
	}

	@Override
	public void releaseInterface(ISIDBlaster sidblaster) {
		if (sidblaster.deviceID() < devices.size()) {
			devicesAllocated.set(sidblaster.deviceID(), false);
		}
	}

}
