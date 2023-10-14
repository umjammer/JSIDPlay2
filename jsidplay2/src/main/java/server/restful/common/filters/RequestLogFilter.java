package server.restful.common.filters;

import static libsidutils.IOUtils.getFileSize;
import static org.apache.http.HttpHeaders.CONTENT_LENGTH;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class RequestLogFilter implements Filter {

	private static final Logger LOG = Logger.getLogger(RequestLogFilter.class.getName());

	public static final String FILTER_PARAMETER_SERVLET_NAME = "servletName";

	private ServletContext servletContext;
	private String servletName;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		servletContext = filterConfig.getServletContext();
		servletName = filterConfig.getInitParameter(FILTER_PARAMETER_SERVLET_NAME);
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
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
		result.append(": ");
		return result.toString();
	}

	private String remoteAddr(ServletRequest request) {
		StringBuilder result = new StringBuilder();
		result.append("from ");
		result.append(request.getRemoteAddr());
		result.append(" (");
		result.append(request.getRemotePort());
		result.append(") ");
		return result.toString();
	}

	private String localAddr(ServletRequest request) {
		StringBuilder result = new StringBuilder();
		result.append("to ");
		result.append(request.getLocalAddr());
		result.append(" (");
		result.append(request.getLocalPort());
		result.append("), ");
		return result.toString();
	}

	private String user(ServletRequest request) {
		StringBuilder result = new StringBuilder();
		if (request instanceof HttpServletRequest) {
			HttpServletRequest httpServletRequest = (HttpServletRequest) request;

			result.append("user ");
			result.append(Optional.ofNullable(httpServletRequest.getRemoteUser()).orElse("<anonymous>"));
			result.append(", ");
		}
		return result.toString();
	}

	private String request(ServletRequest request) {
		StringBuilder result = new StringBuilder();
		if (request instanceof HttpServletRequest) {
			HttpServletRequest httpServletRequest = (HttpServletRequest) request;

			result.append(httpServletRequest.getMethod());
			result.append(" ");
			result.append(httpServletRequest.getRequestURI());
			if (httpServletRequest.getQueryString() != null) {
				result.append("?");
				result.append(httpServletRequest.getQueryString());
			}
			if (LOG.isLoggable(Level.FINEST)) {
				result.append(" ");
				Enumeration<String> headerNamesEnumeration = httpServletRequest.getHeaderNames();
				while (headerNamesEnumeration.hasMoreElements()) {
					String headerName = headerNamesEnumeration.nextElement();
					result.append(headerName + "=" + httpServletRequest.getHeader(headerName));
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
					result.append(getFileSize(request.getContentLengthLong()));
				}
			}
			result.append(", ");
		}
		return result.toString();
	}

	private String response(ServletResponse response) {
		StringBuilder result = new StringBuilder();
		if (response instanceof HttpServletResponse) {
			HttpServletResponse httpServletResponse = (HttpServletResponse) response;

			result.append("STATUS=");
			result.append(httpServletResponse.getStatus());

			if (LOG.isLoggable(Level.FINEST)) {
				result.append(" ");
				result.append(httpServletResponse.getHeaderNames().stream()
						.map(headerName -> headerName + "=" + httpServletResponse.getHeader(headerName))
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
		}
		return result.toString();
	}

	private String memory() {
		StringBuilder result = new StringBuilder();
		Runtime runtime = Runtime.getRuntime();
		result.append(getFileSize(runtime.totalMemory() - runtime.freeMemory()));
		result.append("/");
		result.append(getFileSize(runtime.maxMemory()));
		return result.toString();
	}

	public void log(String message) {
		servletContext.log(servletName + ": " + message);
	}

}
