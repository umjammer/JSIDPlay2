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
		String clientIp = servletRequest.getRemoteAddr();
		requestTimers.putIfAbsent(clientIp, new RequestTimer());
		RequestTimer timer = requestTimers.get(clientIp);

		if (timer.getCount() < maxRequestsPerMinute) {
			timer.increment();
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

	private static class RequestTimer {
		private int count;
		private Timer timer = new Timer();

		public RequestTimer() {
			timer.scheduleAtFixedRate(new RequestTimerTask(), TimeUnit.MINUTES.toMillis(1),
					TimeUnit.MINUTES.toMillis(1));
		}

		public int getCount() {
			return count;
		}

		public void increment() {
			count++;
		}

		public void cancel() {
			timer.cancel();
		}

		private class RequestTimerTask extends TimerTask {
			@Override
			public void run() {
				count = 0;
			}
		}
	}
}
