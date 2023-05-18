package server.restful.common.filters;

import static org.apache.http.HttpStatus.SC_TOO_MANY_REQUESTS;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Ensure a maximum number of requests per minute (server overload protection).
 * 
 * @author ken
 *
 */
public class TimeBasedRateLimiterFilter implements Filter {

	public static final String FILTER_PARAMETER_MAX_REQUESTS_PER_MINUTE = "maxRequestsPerMinute";

	private ConcurrentHashMap<String, RequestTimer> requestTimers = new ConcurrentHashMap<>();
	private int maxRequestsPerMinute;

	@Override
	public void init(FilterConfig filterConfig) {
		maxRequestsPerMinute = Integer
				.parseInt(filterConfig.getInitParameter(FILTER_PARAMETER_MAX_REQUESTS_PER_MINUTE));
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
			throws IOException, ServletException {
		final RequestTimer timer = requestTimers.compute(servletRequest.getRemoteAddr(), (clientIp, requestTimer) -> {
			if (requestTimer == null) {
				requestTimer = new RequestTimer(clientIp);
			}
			return requestTimer;
		});
		if (timer.increment() < maxRequestsPerMinute) {
			chain.doFilter(servletRequest, servletResponse);
		} else {
			// handle limit case, e.g. return status code 429 (Too Many Requests)
			HttpServletResponse response = (HttpServletResponse) servletResponse;
			response.sendError(SC_TOO_MANY_REQUESTS, "Too Many Requests");
		}
	}

	@Override
	public void destroy() {
		requestTimers.values().forEach(RequestTimer::cancel);
	}

	private final class RequestTimer {
		private final Timer timer;
		private int count;

		private RequestTimer(String clientIp) {
			this.timer = new Timer(TimeBasedRateLimiterFilter.class.getSimpleName() + "-Timer-" + clientIp, false);
			timer.schedule(new TimerTask() {

				@Override
				public void run() {
					cancel();
				}

				@Override
				public boolean cancel() {
					requestTimers.remove(clientIp);
					return super.cancel();
				}

			}, TimeUnit.MINUTES.toMillis(1));
		}

		public int increment() {
			return count++;
		}

		public void cancel() {
			timer.cancel();
		}
	}
}
