package sidblaster.async;

import com.ftdi.FTD2XXException;

import sidblaster.Command;
import sidblaster.CommandEnum;
import sidblaster.ICommandDispatcher;

public class AsyncDispatcher implements ICommandDispatcher {

	private ThreadCommandReceiver receiver;
	private Thread sidWriteThread;
	private boolean isInitialized;

	@Override
	public int sendCommand(Command cmd) throws FTD2XXException, InterruptedException {
		ensureInitialized();
		int retval = 0;
		while (!sidWriteThread.isAlive()) {
			Thread.yield();
		}
		if (cmd.getCommand() == CommandEnum.Flush) {
			receiver.flush();

			while (!receiver.queueIsEmpty()) {
				Thread.yield();
			}
			return 0;
		}
		receiver.tryPutCommand(cmd);

		if (cmd.getCommand() == CommandEnum.Read) {
			// XXX blocking
			while (!receiver.isReadResultReady()) {
				Thread.yield();
			}
			retval = receiver.readResult();
		}
		return retval;
	}

	@Override
	public void initialize() throws FTD2XXException {
		isInitialized = false;
	}

	public void ensureInitialized() throws FTD2XXException {
		if (!isInitialized) {
			assert (receiver == null);
			receiver = new ThreadCommandReceiver();
			sidWriteThread = new Thread(receiver);
			sidWriteThread.setDaemon(true);
			sidWriteThread.start();
			if (sidWriteThread.isAlive()) {
				while (!receiver.isDevicesAvailable()) {
					Thread.yield();
				}
			}
			isInitialized = true;
		}
	}

	@Override
	public void uninitialize() throws FTD2XXException, InterruptedException {
		if (isInitialized) {
			receiver.setAbortSIDWriteThread();
			sidWriteThread.join();
			receiver.uninitialize();
			receiver = null;
			isInitialized = false;
		}
	}

	@Override
	public boolean isAsync() {
		return true;
	}

	@Override
	public int deviceCount() throws FTD2XXException {
		ensureInitialized();
		return receiver.deviceCount();
	}

	@Override
	public void setWriteBufferSize(int bufferSize) {
		receiver.setWriteBufferSize(bufferSize);
	}

	@Override
	public void setLatencyTimer(short ms) throws FTD2XXException {
		receiver.setLatencyTimer(ms);
	}
}
