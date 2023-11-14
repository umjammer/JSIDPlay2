package server.restful.common.filters;

import static org.apache.http.HttpStatus.SC_TOO_MANY_REQUESTS;
import static server.restful.common.rtmp.PlayerCleanupTimerTask.count;

import java.io.IOException;
import java.util.Optional;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Ensure a maximum number of RTMP video creations in parallel (server overload
 * protection).
 * 
 * @author ken
 *
 */
@SuppressWarnings("serial")
@WebFilter(filterName = "RTMPBasedRateLimiterFilter", displayName = "RTMPBasedRateLimiterFilter", servletNames = {
		"ConvertServlet",
		"WhatsSidServlet" }, description = "Ensure a maximum number of RTMP video creations in parallel (server overload protection)")
public final class RTMPBasedRateLimiterFilter extends HttpFilter {

	public static final String FILTER_PARAMETER_MAX_RTMP_PER_SERVLET = "maxRtmpPerServlet";

	private int maxRtmpPerServlet;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		maxRtmpPerServlet = Integer
				.parseInt(Optional.ofNullable(filterConfig.getInitParameter(FILTER_PARAMETER_MAX_RTMP_PER_SERVLET))
						.orElseThrow(() -> new UnavailableException(FILTER_PARAMETER_MAX_RTMP_PER_SERVLET)));
	}

	@Override
	public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if (count() < maxRtmpPerServlet) {
			// let the request through and process as usual
			chain.doFilter(request, response);
		} else {
			// handle limit case, e.g. return status code 429 (Too Many Requests)
			response.sendError(SC_TOO_MANY_REQUESTS, "Too Many Requests");
		}
	}
}
