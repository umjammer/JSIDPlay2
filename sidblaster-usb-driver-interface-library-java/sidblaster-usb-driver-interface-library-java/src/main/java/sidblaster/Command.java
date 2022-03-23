package sidblaster;

public final class Command {

	private final CommandEnum command;
	private final int device;
	private final byte reg, data;
	private final long startTime;

	public Command(int device, CommandEnum commandEnum) {
		this(device, commandEnum, (byte) 0, (byte) 0, 0);
	}

	public Command(int device, CommandEnum commandEnum, byte reg, byte data) {
		this(device, commandEnum, reg, data, 0);
	}

	public Command(int device, CommandEnum commandEnum, byte reg, byte data, long startTime) {
		this.device = device;
		this.command = commandEnum;
		this.reg = reg;
		this.data = data;
		this.startTime = startTime;
	}

	public int getDevice() {
		return device;
	}

	public CommandEnum getCommand() {
		return command;
	}

	public byte getReg() {
		return reg;
	}

	public byte getData() {
		return data;
	}

	public long getDelay() {
		return startTime - System.currentTimeMillis();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(command);
		builder.append(":");
		builder.append(reg);
		builder.append("=");
		builder.append(data);
		return builder.toString();
	}
}
