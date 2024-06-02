package server.restful.common;

public enum TeaVMFormat {
	JS("JavaScript", "2MB"), WASM("WASM", "5MB");

	private String teaVMFormatName;
	private String approximateSize;

	private TeaVMFormat(String teaVMFormatName, String approximateSize) {
		this.teaVMFormatName = teaVMFormatName;
		this.approximateSize = approximateSize;
	}

	public String getTeaVMFormatName() {
		return teaVMFormatName;
	}
	
	public String getApproximateSize() {
		return approximateSize;
	}
}
