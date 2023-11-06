package server.restful.common;

import static jakarta.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static java.lang.Thread.currentThread;

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
	protected Thread parentThread;

	public HttpAsyncContextRunnable(AsyncContext asyncContext, JSIDPlay2Servlet servlet) {
		this.asyncContext = asyncContext;
		this.servlet = servlet;
		this.parentThread = currentThread();

		asyncContext.addListener(new DefaultAsyncListener() {

			public void onTimeout(AsyncEvent event) throws IOException {
				servlet.warn("Asynchronous servlet timeout", parentThread);
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
				servlet.error(t, parentThread);
			} else {
				servlet.warn(t.getMessage(), parentThread);
			}
		}
	}

	@Override
	public final void run() {
		try {
			execute();
		} catch (Throwable t) {
			if (LOG.isLoggable(Level.FINEST)) {
				servlet.error(t, parentThread);
			} else {
				servlet.warn(t.getMessage(), parentThread);
			}
		} finally {
			complete();
		}
	}

	protected abstract void execute() throws IOException;
}
