package server.restful.common.async;

import static java.lang.Thread.currentThread;
import static server.restful.common.ServletUtil.error;
import static server.restful.common.ServletUtil.warn;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
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
	private AtomicBoolean completed = new AtomicBoolean();

	protected Thread parentThread;

	public HttpAsyncContextRunnable(AsyncContext asyncContext, ServletContext servletContext) {
		this.asyncContext = asyncContext;
		this.servletContext = servletContext;
		this.parentThread = currentThread();

		asyncContext.addListener(new AsyncListener() {

			@Override
			public void onComplete(AsyncEvent event) throws IOException {
				getAndSetComplete();
			}

			public void onTimeout(AsyncEvent event) throws IOException {
				warn(servletContext, "Asynchronous servlet timeout", parentThread);
				getResponse().setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
				getResponse().getOutputStream().flush();
			}

			@Override
			public void onError(AsyncEvent event) throws IOException {
				if (LOG.isLoggable(Level.FINEST)) {
					error(servletContext, event.getThrowable());
				} else {
					warn(servletContext, event.getThrowable().getMessage());
				}
				if (!getAndSetComplete()) {
					asyncContext.complete();
				}
			}

			@Override
			public void onStartAsync(AsyncEvent event) throws IOException {
			}

		});
	}

	protected boolean isComplete() {
		return completed.get();
	}

	private boolean getAndSetComplete() {
		return completed.getAndSet(true);
	}

	private HttpServletRequest getRequest() {
		return (HttpServletRequest) asyncContext.getRequest();
	}

	private HttpServletResponse getResponse() {
		return (HttpServletResponse) asyncContext.getResponse();
	}

	@Override
	public final void run() {
		try {
			if (!isComplete()) {
				run(getRequest(), getResponse());
			}
		} catch (Throwable t) {
			if (LOG.isLoggable(Level.FINEST)) {
				error(servletContext, t, parentThread);
			} else {
				warn(servletContext, t.getMessage(), parentThread);
			}
		} finally {
			if (!getAndSetComplete()) {
				asyncContext.complete();
			}
		}
	}

	protected abstract void run(HttpServletRequest request, HttpServletResponse response) throws IOException;
}
