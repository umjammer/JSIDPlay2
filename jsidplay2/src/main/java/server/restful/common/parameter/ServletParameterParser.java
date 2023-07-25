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
import java.util.function.Function;
import java.util.stream.Stream;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 
 * Parse servlet parameters using JCommander based on annotated parameter
 * objects.
 * 
 * @author khaendel
 *
 */
public class ServletParameterParser {

	private final JCommander commander;
	private final ServletUsageFormatter usageFormatter;

	/**
	 * Parse request parameters and request path according to annotated parameters
	 * of the parameterObject.
	 * 
	 * @param request         servlet request to parse
	 * @param response        servlet response to output usage message
	 * @param parameterObject annotated parameters of the parameterObject
	 * @param programName     for usage message
	 */
	public ServletParameterParser(HttpServletRequest request, HttpServletResponse response, Object parameterObject,
			String programName) throws IOException {
		this(request, response, parameterObject, programName, false);
	}

	public ServletParameterParser(HttpServletRequest request, HttpServletResponse response, Object parameterObject,
			String programName, boolean acceptUnknownOptions) throws IOException {
		commander = JCommander.newBuilder().addObject(parameterObject).programName(programName)
				.columnSize(Integer.MAX_VALUE)
				.console(new PrintStreamConsole(
						new PrintStream(response.getOutputStream(), true, StandardCharsets.UTF_8.toString())))
				.acceptUnknownOptions(acceptUnknownOptions).build();
		String[] requestParameters = getRequestParameters(request);
		usageFormatter = new ServletUsageFormatter(commander, request, response, requestParameters);
		commander.setUsageFormatter(usageFormatter);
		try {
			commander.parse(requestParameters);
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

	private String[] getRequestParameters(HttpServletRequest request) {
		return concat(
				Collections.list(request.getParameterNames()).stream()
						.flatMap(name -> asList(request.getParameterValues(name)).stream()
								.filter(v -> !"null".equals(v) && !"undefined".equals(v))
								.map(v -> of((name.length() > 1 ? "--" : "-") + name, v)))
						.flatMap(Function.identity()),
				ofNullable(request.getPathInfo()).map(Stream::of).orElse(empty())).toArray(String[]::new);
	}

}
