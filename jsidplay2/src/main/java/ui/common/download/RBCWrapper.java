package ui.common.download;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

final class RBCWrapper implements ReadableByteChannel {
	private RBCWrapperDelegate delegate;
	private long expectedSize;
	private ReadableByteChannel rbc;
	private long readSoFar;

	RBCWrapper(ReadableByteChannel rbc, long expectedSize, RBCWrapperDelegate delegate) {
		this.delegate = delegate;
		this.expectedSize = expectedSize;
		this.rbc = rbc;
	}

	@Override
	public void close() throws IOException {
		rbc.close();
	}

	@Override
	public boolean isOpen() {
		return rbc.isOpen();
	}

	@Override
	public int read(ByteBuffer bb) throws IOException {
		int n;
		if ((n = rbc.read(bb)) > 0) {
			readSoFar += n;
			double progress = expectedSize > 0 ? readSoFar * 100.0 / expectedSize : 0.0;
			delegate.rbcProgressCallback(progress);
		}

		return n;
	}
}