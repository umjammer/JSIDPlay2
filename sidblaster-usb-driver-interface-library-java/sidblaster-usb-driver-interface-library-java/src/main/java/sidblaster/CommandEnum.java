package sidblaster;

/**
 * Keeps the order of command ids the same as the original windows host
 * 
 * @author ken
 *
 */
public enum CommandEnum {
	Reset(0), Delay(1), Write(2), Read(3), Sync(4), Flush(5), Mute(6), MuteAll(7), SoftFlush(8), Lock(9),
	MuteLine(10), Filter(10), Unlock(11), OpenDevice(12), CloseDevice(13), NOP(14);

	private int value;

	private CommandEnum(int value) {
		this.value = value;
	}

	public int getValue() {
		return value;
	}
}