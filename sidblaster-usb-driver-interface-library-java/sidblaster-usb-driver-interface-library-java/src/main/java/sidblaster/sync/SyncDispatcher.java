package sidblaster.sync;

import java.util.ArrayList;
import java.util.List;

import com.ftdi.FTD2XXException;

import sidblaster.Command;
import sidblaster.ICommandDispatcher;
import sidblaster.ISIDBlaster;
import sidblaster.d2xx.SIDBlasterEnumerator;

public class SyncDispatcher implements ICommandDispatcher {

	private List<ISIDBlaster> m_SIDBlasters = new ArrayList<>();

	@Override
	public int sendCommand(Command cmd) throws FTD2XXException, InterruptedException {
		if (cmd.getDevice() >= 0 && cmd.getDevice() < m_SIDBlasters.size()) {
			ISIDBlaster sidblaster = m_SIDBlasters.get(cmd.getDevice());
			switch (cmd.getCommand()) {
			case Write:
				sidblaster.write(cmd.getReg(), cmd.getData());
				break;
			case Delay:
				if (cmd.getDelay() > 0) {
					Thread.sleep(cmd.getDelay());
				}
				break;
			default:
				break;
			}
		}
		return 0;
	}

	@Override
	public boolean isAsync() {
		return false;
	}

	@Override
	public void initialize() throws FTD2XXException {
		int deviceID = 0;
		int deviceCount = SIDBlasterEnumerator.getInstance().deviceCount();
		for (int i = 0; i < deviceCount; ++i) {
			m_SIDBlasters.add(SIDBlasterEnumerator.getInstance().createInterface(deviceID++));
		}
		for (int i = 0; i < m_SIDBlasters.size(); ++i) {
			m_SIDBlasters.get(i).open();
		}
	}

	@Override
	public void uninitialize() throws FTD2XXException {
		for (int i = 0; i < m_SIDBlasters.size(); ++i) {
			m_SIDBlasters.get(i).close();
			SIDBlasterEnumerator.getInstance().releaseInterface(m_SIDBlasters.get(i));
		}
		m_SIDBlasters.clear();
	}

	@Override
	public int deviceCount() {
		return m_SIDBlasters.size();
	}

	@Override
	public void setWriteBufferSize(int bufferSize) {
		// TODO Auto-generated method stub
	}

}
