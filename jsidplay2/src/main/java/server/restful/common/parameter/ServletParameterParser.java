package server.restful.common.parameter;

import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Stream.concat;
import static java.util.stream.Stream.empty;
import static java.util.stream.Stream.of;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 
 * Parse servlet parameters using JCommander based on annotated parameter
 * objects. Servlet path is treated like a main argument and servlet parameters
 * as options.
 * 
 * @author khaendel
 *
 */
public class ServletParameterParser {

	private final static List<String> VALUE_ABSENT = asList("", "null", "undefined");

	private final JCommander commander;
	private final ServletUsageFormatter usageFormatter;

	/**
	 * Parse request parameters and request path according to annotated parameters
	 * of the parameterObject.
	 * 
	 * @param request         servlet request to parse
	 * @param response        servlet response to output usage message
	 * @param parameterObject annotated parameters of the parameterObject
	 * @param webServlet      servlet to parse parameters for
	 */
	public ServletParameterParser(HttpServletRequest request, HttpServletResponse response, Object parameterObject,
			WebServlet webServlet) throws IOException {
		this(request, response, parameterObject, webServlet, false);
	}

	public ServletParameterParser(HttpServletRequest request, HttpServletResponse response, Object parameterObject,
			WebServlet webServlet, boolean acceptUnknownOptions) throws IOException {
		commander = JCommander.newBuilder().addObject(parameterObject).programName(createProgramName(webServlet))
				.columnSize(Integer.MAX_VALUE)
				.console(new PrintStreamConsole(
						new PrintStream(response.getOutputStream(), true, StandardCharsets.UTF_8.toString())))
				.acceptUnknownOptions(acceptUnknownOptions).build();
		usageFormatter = new ServletUsageFormatter(commander, request, response);
		commander.setUsageFormatter(usageFormatter);
		try {
			commander.parse(getRequestParameters(request));
		} catch (ParameterException e) {
			usageFormatter.setException(e);
		}
	}

	public void usage() {
		commander.usage();
	}

	public void setException(FileNotFoundException exception) {
		usageFormatter.setException(exception);
	}

	public boolean hasException() {
		return usageFormatter.getException() != null;
	}

	static String[] getRequestParameters(HttpServletRequest request) {
		return concat(
				Collections.list(request.getParameterNames()).stream()
						.flatMap(name -> asList(request.getParameterValues(name)).stream()
								.filter(v -> VALUE_ABSENT.stream().noneMatch(v::equals))
								.map(v -> v.endsWith("#") ? v.subSequence(0, v.length() - 1) : v)
								.map(v -> of((name.length() > 1 ? "--" : "-") + name, v)))
						.flatMap(Function.identity()),
				ofNullable(request.getPathInfo()).map(Stream::of).orElse(empty())).toArray(String[]::new);
	}

	private String createProgramName(WebServlet webServlet) {
		return asList(webServlet.urlPatterns()).stream().findFirst().map(str -> str.replaceAll("/[*]$", ""))
				.orElse("/???");
	}

}
