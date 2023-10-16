package server.restful.common.filters;

import static org.apache.http.HttpStatus.SC_TOO_MANY_REQUESTS;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Ensure a maximum number of requests in parallel (server overload protection).
 * 
 * @author ken
 *
 */
public final class CounterBasedRateLimiterFilter extends HttpFilter {

	private static final long serialVersionUID = 1L;

	public static final String FILTER_PARAMETER_MAX_REQUESTS_PER_SERVLET = "maxRequestsPerServlet";

	private final AtomicInteger atomicServletRequestCounter = new AtomicInteger();
	private int maxRequestsPerServlet;

	@Override
	public void init(FilterConfig filterConfig) {
		maxRequestsPerServlet = Integer
				.parseInt(filterConfig.getInitParameter(FILTER_PARAMETER_MAX_REQUESTS_PER_SERVLET));
	}

	@Override
	public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		try {
			if (atomicServletRequestCounter.getAndIncrement() < maxRequestsPerServlet) {
				// let the request through and process as usual
				chain.doFilter(request, response);
			} else {
				// handle limit case, e.g. return status code 429 (Too Many Requests)
				response.sendError(SC_TOO_MANY_REQUESTS, "Too Many Requests");
			}
		} finally {
			atomicServletRequestCounter.getAndDecrement();
		}
	}
}
