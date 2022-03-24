package exsid;

public class SupportedDevices {

	private String description;
	private int pid;
	private int vid;

	/**
	 * hardware-dependent constants
	 */
	private HardwareSpecs hardwareSpecs = new HardwareSpecs();

	public SupportedDevices(String description, int pid, int vid, int model, long writeCycles, long readPreCycles,
			long readPostCycles, long readOffsetCycles, long csioctlCycles, long mindelCycles, long maxAdj,
			long ldelayOffs) {
		this.description = description;
		this.pid = pid;
		this.vid = vid;

		this.hardwareSpecs.setModel(model);
		this.hardwareSpecs.setWriteCycles(writeCycles);
		this.hardwareSpecs.setReadPreCycles(readPreCycles);
		this.hardwareSpecs.setReadPostCycles(readPostCycles);
		this.hardwareSpecs.setReadOffsetCycles(readOffsetCycles);
		this.hardwareSpecs.setCsioctlCycles(csioctlCycles);
		this.hardwareSpecs.setMindelCycles(mindelCycles);
		this.hardwareSpecs.setMaxAdj(maxAdj);
		this.hardwareSpecs.setLdelayOffs(ldelayOffs);
	}

	public String getDescription() {
		return description;
	}

	public int getPid() {
		return pid;
	}

	public int getVid() {
		return vid;
	}

	public HardwareSpecs getHardwareSpecs() {
		return hardwareSpecs;
	}
}
