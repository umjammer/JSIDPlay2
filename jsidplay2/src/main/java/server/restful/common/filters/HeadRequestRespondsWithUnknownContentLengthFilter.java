package server.restful.common.filters;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
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
@WebFilter(filterName = "HeadRequestRespondsWithUnknownContentLengthFilter")
public final class HeadRequestRespondsWithUnknownContentLengthFilter extends HttpFilter {

	public void init(FilterConfig filterConfig) {
	}

	@Override
	public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if ("HEAD".equals(request.getMethod())) {
			response.setContentLengthLong(-1);
			response.setStatus(HttpServletResponse.SC_OK);
		} else {
			// let the request through and process as usual
			chain.doFilter(request, response);
		}
	}
}
