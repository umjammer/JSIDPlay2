package sidblaster.d2xx;

import static java.lang.Integer.valueOf;
import static java.lang.System.getenv;

import com.ftdi.EEPROMData;
import com.ftdi.FTD2XXException;
import com.ftdi.FTDevice;
import com.ftdi.Parity;
import com.ftdi.StopBits;
import com.ftdi.WordLength;

import sidblaster.SIDType;

public class D2XXDevice {

	private final static int FT_READ_TIMEOUT = 1000;
	private final static int FT_WRITE_TIMEOUT = 1000;
	private final static int FT_BAUD_RATE = valueOf(getenv().getOrDefault("SIDBLASTERUSB_BAUDRATE", "500000")); // FT_BAUD_115200
	private final static short FT_LATENCY_TIMER = Short
			.valueOf(getenv().getOrDefault("SIDBLASTERUSB_LATENCY_TIMER", "2"));

	public static void open(FTDevice device) throws FTD2XXException {
		device.open();
	}

	public static void close(FTDevice device) throws FTD2XXException {
		device.close();
	}

	public static int read(FTDevice device, byte[] buffer, int count) throws FTD2XXException {
		return device.read(buffer, 0, count);
	}

	public static int write(FTDevice device, byte[] buffer, int count) throws FTD2XXException {
		return device.write(buffer, 0, count);
	}

	public static boolean IsOpen(FTDevice device) {
		return device != null && device.isOpen();
	}

	public static void initialize(FTDevice device) throws FTD2XXException {
		device.setTimeouts(FT_READ_TIMEOUT, FT_WRITE_TIMEOUT);
		device.setBaudRate(FT_BAUD_RATE);
		device.setDataCharacteristics(WordLength.BITS_8, StopBits.STOP_BITS_1, Parity.PARITY_NONE);
		device.setLatencyTimer(FT_LATENCY_TIMER);
	}

	public static void setLatencyTimer(FTDevice device, short ms) throws FTD2XXException, IllegalArgumentException {
		device.setLatencyTimer(ms);
	}

	public static SIDType getSIDType(FTDevice device) {
		if (device.getDevDescription().startsWith("SIDBlaster/USB/")) {
			String sidType = device.getDevDescription().substring("SIDBlaster/USB/".length());
			if (sidType.equals("6581")) {
				return SIDType.MOS6581;
			} else if (sidType.equals("8580")) {
				return SIDType.MOS8580;
			}
		}
		return SIDType.NONE;
	}

	public static void setSIDType(FTDevice device, SIDType sidType) throws FTD2XXException, InterruptedException {
		if (!device.isOpen()) {
			device.open();
		}
		EEPROMData read = device.readEEPROM();

		switch (sidType) {
		case MOS6581:
			read.setDescription("SIDBlaster/USB/6581");
			break;
		case MOS8580:
			read.setDescription("SIDBlaster/USB/8580");
			break;
		case NONE:
		default:
			read.setDescription("SIDBlaster/USB");
			break;
		}

		device.writeEEPROM(read);

		Thread.sleep(1000);
	}

	public static void setSerialNo(FTDevice device, String serialNo) throws FTD2XXException, InterruptedException {
		if (!device.isOpen()) {
			device.open();
		}
		EEPROMData read = device.readEEPROM();
		read.setSerialNumber(serialNo);

		device.writeEEPROM(read);

		Thread.sleep(1000);
	}

	public static void displayInfo(FTDevice device) {
		System.out.printf("%18s%s\n", "FT Device type: ", device.getDevType());
		System.out.printf("%18s%s\n", "Serial number: ", device.getDevSerialNumber());
		System.out.printf("%18s%s\n", "Description: ", device.getDevDescription());
		System.out.printf("%18s0x%08X\n", "VID&PID: ", device.getDevID());
		System.out.printf("%18s%d\n", "Is opened: ", device.isOpen() ? 1 : 0);
		System.out.printf("%18s0x%08X\n", "Location ID: ", device.getDevLocationID());
	}

	public static void send(FTDevice device, byte[] buffer) throws FTD2XXException {
		send(device, buffer, buffer.length);
	}

	public static void send(FTDevice device, byte[] buffer, int count) throws FTD2XXException {
		if (count != 0) {
			device.write(buffer, 0, count);
		}
	}

	public static byte[] receive(FTDevice device) throws FTD2XXException {
		int rxBytes = device.getQueueStatus();
		if (rxBytes > 0) {
			byte buffer[] = new byte[rxBytes];
			read(device, buffer, rxBytes);
			return buffer;
		}
		return new byte[0];
	}
}
