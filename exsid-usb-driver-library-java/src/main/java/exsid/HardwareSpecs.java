package exsid;

public class HardwareSpecs {
	/**
	 * exSID device model in use
	 */
	private int model;
	/**
	 * number of SID clocks spent in write ops
	 */
	private long writeCycles;
	/**
	 * number of SID clocks spent in read op before data is actually read
	 */
	private long readPreCycles;
	/**
	 * number of SID clocks spent in read op after data is actually read
	 */
	private long readPostCycles;
	/**
	 * read offset adjustment to align with writes (see function documentation)
	 */
	private long readOffsetCycles;
	/**
	 * number of SID clocks spent in chip select ioctl
	 */
	private long csioctlCycles;
	/**
	 * lowest number of SID clocks that can be accounted for in delay
	 */
	private long mindelCycles;
	/**
	 * maximum number of SID clocks that can be encoded in final delay for
	 * read()/write()
	 */
	private long maxAdj;
	/**
	 * long delay SID clocks offset
	 */
	private long ldelayOffs;

	public int getModel() {
		return model;
	}

	public void setModel(int model) {
		this.model = model;
	}

	public long getWriteCycles() {
		return writeCycles;
	}

	public void setWriteCycles(long writeCycles) {
		this.writeCycles = writeCycles;
	}

	public long getReadPreCycles() {
		return readPreCycles;
	}

	public void setReadPreCycles(long readPreCycles) {
		this.readPreCycles = readPreCycles;
	}

	public long getReadPostCycles() {
		return readPostCycles;
	}

	public void setReadPostCycles(long readPostCycles) {
		this.readPostCycles = readPostCycles;
	}

	public long getReadOffsetCycles() {
		return readOffsetCycles;
	}

	public void setReadOffsetCycles(long readOffsetCycles) {
		this.readOffsetCycles = readOffsetCycles;
	}

	public long getCsioctlCycles() {
		return csioctlCycles;
	}

	public void setCsioctlCycles(long csioctlCycles) {
		this.csioctlCycles = csioctlCycles;
	}

	public long getMindelCycles() {
		return mindelCycles;
	}

	public void setMindelCycles(long mindelCycles) {
		this.mindelCycles = mindelCycles;
	}

	public long getMaxAdj() {
		return maxAdj;
	}

	public void setMaxAdj(long maxAdj) {
		this.maxAdj = maxAdj;
	}

	public long getLdelayOffs() {
		return ldelayOffs;
	}

	public void setLdelayOffs(long ldelayOffs) {
		this.ldelayOffs = ldelayOffs;
	}

}