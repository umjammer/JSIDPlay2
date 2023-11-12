package server.restful.common.filters;

import static org.apache.http.HttpStatus.SC_TOO_MANY_REQUESTS;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Ensure a maximum number of requests in parallel (server overload protection).
 * 
 * @author ken
 *
 */
@SuppressWarnings("serial")
@WebFilter(filterName = "CounterBasedRateLimiterFilter", servletNames = { "SpeechToTextServlet" })
public final class CounterBasedRateLimiterFilter extends HttpFilter {

	public static final String FILTER_PARAMETER_MAX_REQUESTS_PER_SERVLET = "maxRequestsPerServlet";

	private final AtomicInteger atomicServletRequestCounter = new AtomicInteger();
	private int maxRequestsPerServlet;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		maxRequestsPerServlet = Integer
				.parseInt(Optional.ofNullable(filterConfig.getInitParameter(FILTER_PARAMETER_MAX_REQUESTS_PER_SERVLET))
						.orElseThrow(() -> new UnavailableException(FILTER_PARAMETER_MAX_REQUESTS_PER_SERVLET)));
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
