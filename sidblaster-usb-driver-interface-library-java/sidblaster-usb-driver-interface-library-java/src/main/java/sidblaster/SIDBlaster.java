package sidblaster;

import static java.lang.Integer.valueOf;
import static java.lang.System.getProperty;

import java.nio.ByteBuffer;

import com.ftdi.FTD2XXException;
import com.ftdi.FTDevice;

import sidblaster.d2xx.D2XXDevice;

public class SIDBlaster extends ISIDBlaster {

	private Integer SIDBLASTERUSB_WRITEBUFFER_SIZE = valueOf(
			getProperty("SIDBLASTERUSB_WRITEBUFFER_SIZE", String.valueOf(DEFAULT_WRITE_BUFFER_SIZE)));

	private ByteBuffer buffer = ByteBuffer.allocate(
			SIDBLASTERUSB_WRITEBUFFER_SIZE != null ? SIDBLASTERUSB_WRITEBUFFER_SIZE : DEFAULT_WRITE_BUFFER_SIZE);

	private final FTDevice sid;

	public SIDBlaster(int deviceID, FTDevice device) {
		super(deviceID);
		sid = device;
	}

	@Override
	public int getWriteBufferSize() {
		return buffer.capacity();
	}

	@Override
	public void setWriteBufferSize(int size) {
		buffer = ByteBuffer.allocate(Math.max(MIN_WRITE_BUFFER_SIZE, Math.min(size, MAX_WRITE_BUFFER_SIZE)));
	}

	@Override
	public void open() throws FTD2XXException {
		if (!sid.isOpen()) {
			D2XXDevice.open(sid);
			D2XXDevice.initialize(sid);
			if (!sid.isOpen()) {
				throw new RuntimeException("Failed to initialize SIDBlaster: " + toString());
			}
			flush();
		}
	}

	@Override
	public void close() throws FTD2XXException {
		if (sid.isOpen()) {
			reset();
			D2XXDevice.close(sid);
		}
	}

	@Override
	public void sync() throws FTD2XXException {
		softFlush();
	}

	@Override
	public byte read(byte reg) throws FTD2XXException {
		softFlush();
		D2XXDevice.send(sid, new byte[] { (byte) (reg | 0xa0) }, 1);
		byte[] received;
		do {
			received = D2XXDevice.receive(sid);
		} while (received.length == 0);
		return received[0];
	}

	@Override
	public void mute(byte ch) throws FTD2XXException {
		softFlush();
		D2XXDevice.send(sid, new byte[] { (byte) ((ch * 7 + 0) | 0xe0), 0, (byte) (ch * 7 + 1), 0 });
	}

	@Override
	public void muteAll() throws FTD2XXException {
		softFlush();
		D2XXDevice.send(sid, new byte[] { (byte) 0xe0, 0, (byte) 0xe1, 0, (byte) 0xe7, 0, (byte) 0xe8, 0, (byte) 0xee,
				0, (byte) 0xef, 0 });
	}

	@Override
	public void reset() throws FTD2XXException {
		flush();
		sync();
		muteAll();
	}

	@Override
	public void write(byte reg, byte data) throws FTD2XXException {
		if (buffer.position() > 0) {
			softFlush();
		}
		D2XXDevice.send(sid, new byte[] { (byte) (reg | 0xe0), data });
	}

	@Override
	public void bufferWrite(byte reg, byte data) throws FTD2XXException {
		if (!buffer.hasRemaining()) {
			softFlush();
		}
		buffer.put(new byte[] { (byte) (reg | 0xe0), data });
	}

	@Override
	public void delay() {
	}

	@Override
	public void flush() {
		buffer.clear();
	}

	@Override
	public void softFlush() throws FTD2XXException {
		D2XXDevice.send(sid, buffer.array(), buffer.position());
		buffer.clear();
	}

	@Override
	public String toString() {
		return sid.getDevSerialNumber();
	}
}