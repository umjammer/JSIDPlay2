package server.restful.common.filters;

import static libsidutils.IOUtils.getPhysicalSize;
import static org.apache.http.HttpHeaders.CONTENT_LENGTH;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static server.restful.common.ServletUtil.thread;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.inject.Inject;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.UnavailableException;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import server.restful.common.log.MonitoringThread;

/**
 * Log request and response.
 * 
 * @author ken
 *
 */
@SuppressWarnings("serial")
@WebFilter(filterName = "RequestLogFilter", displayName = "RequestLogFilter", servletNames = {
		// hls
		"OnKeepAliveServlet", "ProxyServlet",
		// rtmp
		"InsertNextCartServlet", "InsertNextDiskServlet", "JoystickServlet", "OnPlayDoneServlet", "OnPlayServlet",
		"PressKeyServlet", "SetDefaultEmulationReSidFpServlet", "SetDefaultEmulationReSidServlet",
		"SetSidModel6581Servlet", "SetSidModel8580Servlet",
		// sidmapping
		"ExSIDMappingServlet", "HardSIDMappingServlet", "SIDBlasterMappingServlet",
		// whatssid
		"FindHashServlet", "FindTuneServlet", "InsertHashesServlet", "InsertTuneServlet", "TuneExistsServlet",
		"WhatsSidServlet",
		//
		"ConvertServlet", "DirectoryServlet", "DiskDirectoryServlet", "DownloadServlet", "FavoritesNamesServlet",
		"FavoritesServlet", "FiltersServlet", "PhotoServlet", "RandomHVSCServlet", "SpeechToTextServlet",
		"StartPageServlet", "StaticServlet", "STILServlet", "TuneInfoServlet", "UploadServlet", "WebJarsServlet",
		"LogsServlet", "CountLogsServlet" }, description = "Log request and response")
public final class RequestLogFilter extends HttpFilter {

	private static final Logger LOG = Logger.getLogger(RequestLogFilter.class.getName());

	public static final String FILTER_PARAMETER_SERVLET_NAME = "servletName";

	@Inject
	protected MonitoringThread monitoringThread;

	private ServletContext servletContext;
	private String servletName;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		servletContext = filterConfig.getServletContext();
		servletName = Optional.ofNullable(filterConfig.getInitParameter(FILTER_PARAMETER_SERVLET_NAME))
				.orElseThrow(() -> new UnavailableException(FILTER_PARAMETER_SERVLET_NAME));
	}

	@Override
	public void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws IOException, ServletException {

		log(thread() + user(request) + remoteAddr(request) + localAddr(request) + request(request) + memory() + cpu());

		// let the request through and process as usual
		chain.doFilter(request, response);

		log(thread() + response(response) + memory() + cpu());
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
		result.append(", ");
		return result.toString();
	}

	private String cpu() {
		StringBuilder result = new StringBuilder();
		result.append(String.format("CPU %d%%", (int) (monitoringThread.getAvarageUsagePerCPU() * 100.0) / 100));
		return result.toString();
	}

	public void log(String message) {
		servletContext.log(servletName + ": " + message);
	}

}
