package server.restful.common.filters;

import static org.apache.http.HttpStatus.SC_TOO_MANY_REQUESTS;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Ensure a minimum time between requests (kind of DDOS protection).
 * 
 * @author ken
 *
 */
@SuppressWarnings("serial")
@WebFilter(filterName = "TimeDistanceBasedRateLimiterFilter", displayName = "TimeDistanceBasedRateLimiterFilter", description = "Ensure a minimum time between requests (kind of DDOS protection)", servletNames = {
		"StartPageServlet" })
public final class TimeDistanceBasedRateLimiterFilter extends HttpFilter {

	public static final String FILTER_PARAMETER_MIN_TIME_BETWEEN_REQUESTS = "minTimeBetweenRequests";

	private final Map<String, Long> remoteAddrMap = new ConcurrentHashMap<>();
	private int minTimeBetweenRequests;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		minTimeBetweenRequests = Integer
				.parseInt(Optional.ofNullable(filterConfig.getInitParameter(FILTER_PARAMETER_MIN_TIME_BETWEEN_REQUESTS))
						.orElseThrow(() -> new UnavailableException(FILTER_PARAMETER_MIN_TIME_BETWEEN_REQUESTS)));
	}

	@Override
	public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		final Long lastTime = Optional
				.ofNullable(remoteAddrMap.put(request.getRemoteAddr(), System.currentTimeMillis())).orElse(0L);
		if (System.currentTimeMillis() - lastTime > minTimeBetweenRequests) {
			// let the request through and process as usual
			chain.doFilter(request, response);
		} else {
			// handle limit case, e.g. return status code 429 (Too Many Requests)
			response.sendError(SC_TOO_MANY_REQUESTS, "Too Many Requests");
		}
	}

	@Override
	public void destroy() {
		remoteAddrMap.clear();
	}

}
