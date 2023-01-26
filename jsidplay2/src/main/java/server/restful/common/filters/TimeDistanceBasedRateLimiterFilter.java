package server.restful.common.filters;

import static org.apache.http.HttpStatus.SC_TOO_MANY_REQUESTS;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
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
public final class TimeDistanceBasedRateLimiterFilter implements Filter {

	public static final String FILTER_PARAMETER_MIN_TIME_BETWEEN_REQUESTS = "minTimeBetweenRequests";

	private final Map<String, Long> remoteAddrMap = new ConcurrentHashMap<>();
	private int minTimeBetweenRequests;

	@Override
	public void init(FilterConfig filterConfig) {
		minTimeBetweenRequests = Integer
				.parseInt(filterConfig.getInitParameter(FILTER_PARAMETER_MIN_TIME_BETWEEN_REQUESTS));
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
			throws IOException, ServletException {
		String clientIp = servletRequest.getRemoteAddr();
		Long lastTime = Optional.ofNullable(remoteAddrMap.put(clientIp, System.currentTimeMillis())).orElse(0L);

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
