package server.restful.common;

import static jakarta.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public abstract class HttpAsyncContextRunnable implements Runnable {

	private static final Logger LOG = Logger.getLogger(HttpAsyncContextRunnable.class.getName());

	private AsyncContext asyncContext;
	private JSIDPlay2Servlet servlet;
	protected Thread[] parentThreads;

	public HttpAsyncContextRunnable(AsyncContext asyncContext, JSIDPlay2Servlet servlet, Thread... parentThreads) {
		this.asyncContext = asyncContext;
		this.servlet = servlet;
		this.parentThreads = parentThreads;

		asyncContext.addListener(new DefaultAsyncListener() {

			public void onTimeout(AsyncEvent event) throws IOException {
				servlet.warn("Asynchronous servlet timeout", parentThreads);
				if (getResponse() != null) {
					getResponse().sendError(SC_SERVICE_UNAVAILABLE, "Asynchronous servlet timeout");
				}
				complete();
			}

			@Override
			public void onError(AsyncEvent event) throws IOException {
				complete();
			}
		});
	}

	protected HttpServletRequest getRequest() {
		return (HttpServletRequest) asyncContext.getRequest();
	}

	protected HttpServletResponse getResponse() {
		return (HttpServletResponse) asyncContext.getResponse();
	}

	private void complete() {
		try {
			asyncContext.complete();
		} catch (Throwable t) {
			if (LOG.isLoggable(Level.FINEST)) {
				servlet.warn(t.getMessage(), parentThreads);
			}
		}
	}

	@Override
	public final void run() {
		try {
			execute();
		} catch (Throwable t) {
			servlet.warn(t.getMessage(), parentThreads);
		} finally {
			complete();
		}
	}

	protected abstract void execute() throws IOException;
}
