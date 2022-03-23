package sidblaster.d2xx;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.security.auth.Subject;

import com.ftdi.FTD2XXException;
import com.ftdi.FTDevice;

import sidblaster.SIDType;

public class D2XXManager {

	private List<FTDevice> devices = new ArrayList<>();

	private static D2XXManager instance = null;

	public static D2XXManager getInstance() {
		if (instance == null)
			instance = new D2XXManager();
		return instance;
	}

	public List<FTDevice> createDeviceList() throws FTD2XXException {
		devices = FTDevice.getDevices(false).stream().filter(device -> isValidDeviceInfo(device))
				.sorted((d1, d2) -> d1.getDevSerialNumber().compareTo(d2.getDevSerialNumber()))
				.collect(Collectors.toList());
		return devices;
	}

	public boolean isValidDeviceInfo(FTDevice device) {
		// Check if the FTDI is a real Sidblaster
		return device.getDevDescription().startsWith("SIDBlaster/USB");
	}

	public void cleanList() {
		devices.clear();
	}

	public int count() {
		return devices.size();
	}

	public void rescan() throws FTD2XXException {
		List<FTDevice> newDevices = FTDevice.getDevices(false).stream().filter(device -> isValidDeviceInfo(device))
				.sorted((d1, d2) -> d1.getDevSerialNumber().compareTo(d2.getDevSerialNumber()))
				.collect(Collectors.toList());
//		devices.clear();
		devices.addAll(newDevices);
	}

	public FTDevice getDevice(int index) {
		return devices.get(index);
	}

	public void update(Subject subject) throws FTD2XXException {
		rescan();
	}

	public void displayDevicesInfo() {
		System.out.printf("===================================\n");
		System.out.printf("Devices: %d\n", devices.size());
		System.out.printf("===================================\n");
		for (FTDevice ftDevice : devices) {
			D2XXDevice.displayInfo(ftDevice);
			System.out.printf("===================================\n");
		}
	}

	public String GetSerialNo(int index) {
		return devices.get(index).getDevSerialNumber();
	}

	public SIDType GetSIDType(int index) {
		return D2XXDevice.getSIDType(devices.get(index));
	}

	public void SetSIDType(int index, SIDType sidType) throws FTD2XXException, InterruptedException {
		D2XXDevice.setSIDType(devices.get(index), sidType);
	}

	public void SetSerialNo(int index, String serialNo) throws FTD2XXException, InterruptedException {
		D2XXDevice.setSerialNo(devices.get(index), serialNo);
	}

}
