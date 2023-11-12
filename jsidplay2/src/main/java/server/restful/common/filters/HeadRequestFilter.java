package server.restful.common.filters;

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
 * Ensure a maximum number of requests in parallel (server overload protection).
 * 
 * @author ken
 *
 */
@SuppressWarnings("serial")
@WebFilter(filterName = "HeadRequestFilter", servletNames = { "ConvertServlet" })
public final class HeadRequestFilter extends HttpFilter {

	public static final String FILTER_PARAMETER_CONTENT_TYPE = "contentType";

	private String contentType;

	public void init(FilterConfig filterConfig) throws ServletException {
		contentType = Optional.ofNullable(filterConfig.getInitParameter(FILTER_PARAMETER_CONTENT_TYPE))
				.orElseThrow(() -> new UnavailableException(FILTER_PARAMETER_CONTENT_TYPE));
	}

	@Override
	public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {
		if ("HEAD".equals(request.getMethod())) {
			response.setContentLengthLong(-1);
			response.setContentType(contentType);
		} else {
			// let the request through and process as usual
			chain.doFilter(request, response);
		}
	}
}
