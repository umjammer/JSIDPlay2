package server.restful.common.filters;

import static org.apache.http.HttpStatus.SC_TOO_MANY_REQUESTS;
import static server.restful.common.PlayerCleanupTimerTask.count;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Ensure a maximum number of RTMP video creations in parallel (server overload
 * protection).
 * 
 * @author ken
 *
 */
public final class RTMPBasedRateLimiterFilter implements Filter {

	public static final String FILTER_PARAMETER_MAX_RTMP_PER_SERVLET = "maxRtmpPerServlet";

	private int maxRtmpPerServlet;

	@Override
	public void init(FilterConfig filterConfig) {
		maxRtmpPerServlet = Integer.parseInt(filterConfig.getInitParameter(FILTER_PARAMETER_MAX_RTMP_PER_SERVLET));
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
			throws IOException, ServletException {
		if (count() < maxRtmpPerServlet) {
			// let the request through and process as usual
			chain.doFilter(servletRequest, servletResponse);
		} else {
			// handle limit case, e.g. return status code 429 (Too Many Requests)
			HttpServletResponse response = (HttpServletResponse) servletResponse;
			response.sendError(SC_TOO_MANY_REQUESTS, "Too Many Requests");
		}
	}
}
