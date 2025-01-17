package builder.netsiddev.commands;

import static server.netsiddev.Command.MUTE;

public class Mute implements NetSIDPkg {
	private final byte sidNum;
	private final byte voice;
	private final byte mute;

	public Mute(byte sidNum, byte voice, boolean mute) {
		this.sidNum = sidNum;
		this.voice = voice;
		this.mute = (byte) (mute ? 1 : 0);
	}

	@Override
	public byte[] toByteArray() {
		return new byte[] { (byte) MUTE.ordinal(), sidNum, 0, 0, voice, mute };
	}
}
