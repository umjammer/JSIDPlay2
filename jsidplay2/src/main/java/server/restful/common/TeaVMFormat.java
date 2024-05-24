package server.restful.common;

public enum TeaVMFormat {
	JS("JavaScript"), WASM("Web Assembly (WASM)");

	private String teaVMFormatName;

	private TeaVMFormat(String teaVMFormatName) {
		this.teaVMFormatName = teaVMFormatName;
	}

	public String getTeaVMFormatName() {
		return teaVMFormatName;
	}
}
