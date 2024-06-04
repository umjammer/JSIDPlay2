package server.restful.common;

public enum TeaVMFormat {
	JS("JavaScript", "2MB", "{ type: 'module' }"), WASM("WASM", "5MB", "");

	private String teaVMFormatName;
	private String approximateSize;
	private String workerAttributes;

	private TeaVMFormat(String teaVMFormatName, String approximateSize, String workerAttributes) {
		this.teaVMFormatName = teaVMFormatName;
		this.approximateSize = approximateSize;
		this.workerAttributes = workerAttributes;
	}

	public String getTeaVMFormatName() {
		return teaVMFormatName;
	}

	public String getApproximateSize() {
		return approximateSize;
	}

	public String getWorkerAttributes() {
		return workerAttributes;
	}
}
