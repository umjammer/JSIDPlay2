package server.restful.common;

public enum TeaVMFormat {
	JS("JavaScript UMD", "2MB", "classic"), JS_EM2015("JavaScript ECMAScript 2015", "2MB", "module"),
	WASM("WASM", "5MB", "classic");

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
