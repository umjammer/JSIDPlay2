package server.restful.common.filters;

import static org.apache.http.HttpStatus.SC_TOO_MANY_REQUESTS;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

public class TimeBasedRateLimiterFilter implements Filter {

	public static final String FILTER_PARAMETER_MAX_REQUESTS_PER_MINUTE = "maxRequestsPerMinute";

	private ConcurrentHashMap<String, Timer> timers;
	private int maxRequestsPerMinute;

	@Override
	public void init(FilterConfig filterConfig) {
		maxRequestsPerMinute = Integer
				.parseInt(filterConfig.getInitParameter(FILTER_PARAMETER_MAX_REQUESTS_PER_MINUTE));
		timers = new ConcurrentHashMap<>();
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		String clientIp = request.getRemoteAddr();
		Timer timer = timers.get(clientIp);
		if (timer == null) {
			timer = new Timer();
			timers.put(clientIp, timer);
		}
		if (timer.getCount() >= maxRequestsPerMinute) {
			// handle limit case, e.g. return status code 429 (Too Many Requests)
			HttpServletResponse httpServletResponse = (HttpServletResponse) response;
			httpServletResponse.sendError(SC_TOO_MANY_REQUESTS, "Too Many Requests");
			return;
		}
		timer.increment();
		chain.doFilter(request, response);
	}

	@Override
	public void destroy() {
		for (Timer timer : timers.values()) {
			timer.cancel();
		}
	}

	private class Timer {
		private int count;
		private java.util.Timer timer;

		public Timer() {
			count = 0;
			timer = new java.util.Timer();
			timer.scheduleAtFixedRate(new TimerTask(), TimeUnit.MINUTES.toMillis(1), TimeUnit.MINUTES.toMillis(1));
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

		private class TimerTask extends java.util.TimerTask {
			@Override
			public void run() {
				count = 0;
			}
		}
	}
}
