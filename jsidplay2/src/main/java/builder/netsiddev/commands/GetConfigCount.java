package builder.netsiddev.commands;

import static server.netsiddev.Command.GET_CONFIG_COUNT;

public class GetConfigCount implements NetSIDPkg {

	@Override
	public byte[] toByteArray() {
		return new byte[] { (byte) GET_CONFIG_COUNT.ordinal(), 0, 0, 0 };
	}
}
