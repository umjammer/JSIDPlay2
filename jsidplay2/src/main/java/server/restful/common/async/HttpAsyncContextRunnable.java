package server.restful.common.async;

import static jakarta.servlet.http.HttpServletResponse.SC_SERVICE_UNAVAILABLE;
import static java.lang.Thread.currentThread;
import static server.restful.common.ServletUtil.error;
import static server.restful.common.ServletUtil.warn;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public abstract class HttpAsyncContextRunnable implements Runnable {

	private static final Logger LOG = Logger.getLogger(HttpAsyncContextRunnable.class.getName());

	private AsyncContext asyncContext;
	private ServletContext servletContext;
	protected Thread parentThread;

	// Prevent usage of servlet method parameters, use getter/setter, instead!
	@Deprecated
	protected HttpServletRequest request;
	@Deprecated
	protected HttpServletResponse response;

	public HttpAsyncContextRunnable(AsyncContext asyncContext, ServletContext servletContext) {
		this.asyncContext = asyncContext;
		this.servletContext = servletContext;
		this.parentThread = currentThread();

		asyncContext.addListener(new AsyncListener() {

			@Override
			public void onComplete(AsyncEvent event) throws IOException {
			}

			public void onTimeout(AsyncEvent event) throws IOException {
				warn(servletContext, "Asynchronous servlet timeout", parentThread);
				if (getResponse() != null) {
					getResponse().sendError(SC_SERVICE_UNAVAILABLE, "Asynchronous servlet timeout");
				}
				complete();
			}

			@Override
			public void onError(AsyncEvent event) throws IOException {
				complete();
			}

			@Override
			public void onStartAsync(AsyncEvent event) throws IOException {
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
		} catch (IllegalStateException e) {
			// The request associated with the AsyncContext has already completed processing
			// we ignore that here!
		}
	}

	@Override
	public final void run() {
		try {
			execute();
		} catch (Throwable t) {
			if (LOG.isLoggable(Level.FINEST)) {
				error(servletContext, t, parentThread);
			} else {
				warn(servletContext, t.getMessage(), parentThread);
			}
		} finally {
			complete();
		}
	}

	protected abstract void execute() throws IOException;
}
