package sidblaster.async;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import com.ftdi.FTD2XXException;

import sidblaster.Command;
import sidblaster.CommandEnum;

public class ThreadCommandReceiver extends CommandReceiver implements Runnable {

	private BlockingQueue<Command> queue = new ArrayBlockingQueue<Command>(256);

	private boolean flush;

	private volatile boolean devicesAvailable;

	public ThreadCommandReceiver() throws FTD2XXException {
		super();
	}

	@Override
	public void run() {
		try {
			for (int deviceNum = 0; deviceNum < deviceCount(); ++deviceNum) {
				executeCommand(new Command(deviceNum, CommandEnum.OpenDevice));
			}
			devicesAvailable = true;
			while (!abortSIDWriteThread) {
				while (commandsPending()) {
					executeCommand(tryGetCommand());
				}
			}
			for (int deviceNum = 0; deviceNum < deviceCount(); ++deviceNum) {
				executeCommand(new Command(deviceNum, CommandEnum.CloseDevice));
			}
		} catch (FTD2XXException | InterruptedException e) {
			e.printStackTrace();
		}
	}

	public boolean commandsPending() {
		return !queueIsEmpty() || flush;
	}

	public boolean queueIsEmpty() {
		return queue.isEmpty();
	}

	public void flush() {
		flush = true;
	}

	public void tryPutCommand(Command params) throws InterruptedException {
		queue.put(params);
	}

	private Command tryGetCommand() {
		if (flush) {
			flush = false;
			queue.clear();
			return new Command(0, CommandEnum.Flush);
		}
		return queue.poll();
	}

	public boolean isDevicesAvailable() {
		return devicesAvailable;
	}
}
