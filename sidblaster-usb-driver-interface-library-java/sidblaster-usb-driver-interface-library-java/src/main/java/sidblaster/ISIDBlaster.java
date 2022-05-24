package sidblaster;

import com.ftdi.FTD2XXException;

public abstract class ISIDBlaster {

	public static final double PAL_CLOCK = 985248.4;
	public static final double NTSC_CLOCK = 1022727.14;

	public static final int MIN_WRITE_BUFFER_SIZE = 0;
	public static final int DEFAULT_WRITE_BUFFER_SIZE = 16;
	public static final int MAX_WRITE_BUFFER_SIZE = 256;

	private int m_DeviceID;

	public ISIDBlaster(int deviceID) {
		m_DeviceID = deviceID;
	}

	public int deviceID() {
		return m_DeviceID;
	}

	public abstract int getWriteBufferSize();

	public abstract void setWriteBufferSize(int size);

	public abstract void setLatencyTimer(short ms) throws FTD2XXException;

	public abstract void open() throws FTD2XXException;

	public abstract void close() throws FTD2XXException;

	public abstract void reset() throws FTD2XXException;

	public abstract byte read(byte reg) throws FTD2XXException;

	public abstract void mute(byte ch) throws FTD2XXException;

	public abstract void muteAll() throws FTD2XXException;

	public abstract void sync() throws FTD2XXException;

	public abstract void delay();

	public abstract void write(byte reg, byte data) throws FTD2XXException;

	public abstract void bufferWrite(byte reg, byte data) throws FTD2XXException;

	public abstract void flush();

	public abstract void softFlush() throws FTD2XXException;

}
