package sidblaster;

import com.ftdi.FTD2XXException;

public abstract interface ICommandDispatcher {

	public abstract int sendCommand(Command cmd) throws FTD2XXException, InterruptedException;

	public abstract boolean isAsync();

	public abstract void initialize() throws FTD2XXException;

	public abstract void uninitialize() throws FTD2XXException, InterruptedException;

	public abstract int deviceCount() throws FTD2XXException;

	public abstract void setWriteBufferSize(int bufferSize);

}
