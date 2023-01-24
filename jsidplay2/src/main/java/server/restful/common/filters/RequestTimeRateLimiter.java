package server.restful.common.filters;

import static org.apache.http.HttpStatus.SC_TOO_MANY_REQUESTS;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Ensure a minimum time between requests (kind of DDOS protection).
 * 
 * @author ken
 *
 */
public final class RequestTimeRateLimiter implements Filter {

	private final Map<String, Long> remoteAddrMap = new ConcurrentHashMap<>();

	private final int minTimeBetweenRequests;

	public RequestTimeRateLimiter(int minTimeBetweenRequests) {
		this.minTimeBetweenRequests = minTimeBetweenRequests;
	}

	@Override
	public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
			final FilterChain chain) throws IOException, ServletException {
		Long lastTime = Optional
				.ofNullable(remoteAddrMap.put(servletRequest.getRemoteAddr(), System.currentTimeMillis())).orElse(0L);
		if (System.currentTimeMillis() - lastTime > minTimeBetweenRequests) {
			// let the request through and process as usual
			chain.doFilter(servletRequest, servletResponse);
		} else {
			// handle limit case, e.g. return status code 429 (Too Many Requests)
			HttpServletResponse response = (HttpServletResponse) servletResponse;
			response.sendError(SC_TOO_MANY_REQUESTS, "Too Many Requests");
		}
	}
}
