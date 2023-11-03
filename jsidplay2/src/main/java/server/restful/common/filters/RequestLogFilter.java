package server.restful.common.filters;

import static libsidutils.IOUtils.getPhysicalSize;
import static org.apache.http.HttpHeaders.CONTENT_LENGTH;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
@WebFilter(filterName = "RequestLogFilter")
public final class RequestLogFilter extends HttpFilter {

	private static final Logger LOG = Logger.getLogger(RequestLogFilter.class.getName());

	private ServletContext servletContext;
	private String filterName;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		servletContext = filterConfig.getServletContext();
		filterName = filterConfig.getFilterName();
	}

	@Override
	public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		log(thread() + user(request) + remoteAddr(request) + localAddr(request) + request(request) + memory());

		// let the request through and process as usual
		chain.doFilter(request, response);

		log(thread() + response(response) + memory());
	}

	private String thread() {
		return thread(Thread.currentThread());
	}

	private String thread(Thread thread) {
		StringBuilder result = new StringBuilder();
		result.append(thread.getName());
		result.append(" (");
		result.append(thread.getId());
		result.append(")");
		result.append(": ");
		return result.toString();
	}

	private String remoteAddr(HttpServletRequest request) {
		StringBuilder result = new StringBuilder();
		result.append("from ");
		result.append(request.getRemoteAddr());
		result.append(" (");
		result.append(request.getRemotePort());
		result.append(") ");
		return result.toString();
	}

	private String localAddr(HttpServletRequest request) {
		StringBuilder result = new StringBuilder();
		result.append("to ");
		result.append(request.getLocalAddr());
		result.append(" (");
		result.append(request.getLocalPort());
		result.append("), ");
		return result.toString();
	}

	private String user(HttpServletRequest request) {
		StringBuilder result = new StringBuilder();
		result.append("user ");
		result.append(Optional.ofNullable(request.getRemoteUser()).orElse("<anonymous>"));
		result.append(", ");
		return result.toString();
	}

	private String request(HttpServletRequest request) {
		StringBuilder result = new StringBuilder();
		result.append("REQUEST: ");
		result.append(request.getMethod());
		result.append(" ");
		result.append(request.getRequestURI());
		if (request.getQueryString() != null) {
			result.append("?");
			result.append(request.getQueryString());
		}
		if (LOG.isLoggable(Level.FINEST)) {
			result.append(" ");
			Enumeration<String> headerNamesEnumeration = request.getHeaderNames();
			while (headerNamesEnumeration.hasMoreElements()) {
				String headerName = headerNamesEnumeration.nextElement();
				result.append(headerName + "=" + request.getHeader(headerName));
				if (headerNamesEnumeration.hasMoreElements()) {
					result.append(", ");
				}
			}
		} else {
			if (request.getContentType() != null) {
				result.append(" ");
				result.append(CONTENT_TYPE);
				result.append("=");
				result.append(request.getContentType());
			}
			if (request.getContentLengthLong() != -1L) {
				result.append(", ");
				result.append(CONTENT_LENGTH);
				result.append("=");
				result.append(getPhysicalSize(request.getContentLengthLong()));
			}
		}
		result.append(", ");
		return result.toString();
	}

	private String response(HttpServletResponse response) {
		StringBuilder result = new StringBuilder();
		result.append("RESPONSE: STATUS=");
		result.append(response.getStatus());

		if (LOG.isLoggable(Level.FINEST)) {
			result.append(" ");
			result.append(response.getHeaderNames().stream()
					.map(headerName -> headerName + "=" + response.getHeader(headerName))
					.collect(Collectors.joining(", ")));
		} else {
			if (response.getContentType() != null) {
				result.append(" ");
				result.append(CONTENT_TYPE);
				result.append("=");
				result.append(response.getContentType());
			}
		}
		result.append(", ");
		return result.toString();
	}

	private String memory() {
		StringBuilder result = new StringBuilder();
		Runtime runtime = Runtime.getRuntime();
		result.append(getPhysicalSize(runtime.totalMemory() - runtime.freeMemory()));
		result.append("/");
		result.append(getPhysicalSize(runtime.maxMemory()));
		return result.toString();
	}

	public void log(String message) {
		servletContext.log(filterName + ": " + message);
	}

}
