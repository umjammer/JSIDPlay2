package sidblaster;

import com.ftdi.FTD2XXException;

public interface ISIDBlasterEnumerator {

	int deviceCount() throws FTD2XXException;

	ISIDBlaster createInterface(int deviceID);

	void releaseInterface(ISIDBlaster sidblaster);

}
