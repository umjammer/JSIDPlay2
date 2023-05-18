package server.restful.common.parameter;

import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.IServletSystemProperties.BASE_URL;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.beust.jcommander.DefaultUsageFormatter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.Strings;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ServletUsageFormatter extends DefaultUsageFormatter {

	private JCommander commander;
	private HttpServletRequest request;
	private HttpServletResponse response;
	private Exception exception;
	private String[] requestParameters;
	private ResourceBundle bundle;

	public ServletUsageFormatter(JCommander commander, HttpServletRequest request, HttpServletResponse response,
			String[] requestParameters) {
		super(commander);
		this.commander = commander;
		this.request = request;
		this.response = response;
		this.requestParameters = requestParameters;
		this.bundle = getBundle();
	}

	public void setException(Exception exception) {
		if (this.exception == null) {
			this.exception = exception;
		}
	}

	public Exception getException() {
		return exception;
	}

	@Override
	public void appendMainLine(StringBuilder out, boolean hasOptions, boolean hasCommands, int indentCount,
			String indent) {
		try {
			response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
			response.setContentType(MIME_TYPE_TEXT.toString());

			// optional error message
			if (exception != null) {
				out.append("Servlet-Parameter ERROR!");
				out.append("\n");
				out.append(exception.getClass().getSimpleName());
				out.append(": ");
				out.append(exception.getMessage());
				out.append("\n");
				out.append("\n");

				if (requestParameters.length > 0) {
					out.append("Current Servlet-Parameter options:");
					out.append("\n");
					out.append(Strings.join(" ", requestParameters));
					out.append("\n");
					out.append("\n");
				}
			}
			out.append("Example Usage:");
			out.append("\n");

			StringBuilder mainLine = new StringBuilder();
			mainLine.append(indent).append("HTTP-").append(request.getMethod()).append(" ");

			StringBuilder urlAsString = new StringBuilder();
			// Base url + servlet path
			urlAsString.append(BASE_URL).append(commander.getProgramDisplayName());

			// Request path
			if (commander.getMainParameter() != null) {
				urlAsString.append(bundle.getString("EXAMPLE_REQUEST_PATH"));
			}

			List<ParameterDescription> arguments = commander.getFields().values().stream()
					.filter(pd -> !pd.getParameter().hidden() && (Stream.of(Boolean.class, boolean.class)
							.noneMatch(pd.getParameterized().getType()::equals)
							|| pd.getParameterized().getParameter().arity() == 1))
					.sorted(commander.getParameterDescriptionComparator()).collect(Collectors.toList());

			// Query parameters
			if (arguments.size() > 0) {
				urlAsString.append("?");
				arguments.forEach(parameterDescription -> {

					urlAsString.append(getName(parameterDescription.getNames()));
					urlAsString.append("=");
					urlAsString.append(getExampleValue(parameterDescription));

					if (!arguments.get(arguments.size() - 1).equals(parameterDescription)) {
						urlAsString.append("&");
					}
				});
			}
			URL url = new URL(urlAsString.toString());
			URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(),
					url.getQuery(), url.getRef());

			String asciiString = uri.toASCIIString();

			mainLine.append(asciiString);
			wrapDescription(out, indentCount, mainLine.toString());
			out.append("\n");
			out.append("\n");

			// Request path description
			if (commander.getMainParameter() != null && commander.getMainParameterValue() != null) {
				out.append(indent).append("Servlet Path:");
				out.append("\n");
				out.append("      ");
				out.append(commander.getMainParameterValue().getDescription());
				out.append("\n");
				out.append("\n");
			}
			if (arguments.size() > 0) {
				out.append(indent).append("Servlet Parameter");
			}

		} catch (URISyntaxException | MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	private String getExampleValue(ParameterDescription parameterDescription) {
		if (parameterDescription.getParameter().password()) {
			return "********";
		} else if (parameterDescription.getDefault() != null
				|| !parameterDescription.getParameterAnnotation().required()) {
			return String.valueOf(parameterDescription.getDefault());
		} else if (Stream.of(Boolean.class, boolean.class)
				.anyMatch(parameterDescription.getParameterized().getType()::equals)) {
			return String.valueOf(false);
		} else {
			return String.valueOf(0);
		}
	}

	private ResourceBundle getBundle() {
		Object firstParameterObject = commander.getObjects().get(0);
		Parameters parameters = firstParameterObject.getClass().getAnnotation(Parameters.class);
		return ResourceBundle.getBundle(parameters.resourceBundle(), Locale.getDefault());
	}

	private String getName(String names) {
		String[] split = names.split(",");
		for (String name : split) {
			return getParameterName(name);
		}
		return getParameterName(names);
	}

	private String getParameterName(String name) {
		if (name.startsWith("--")) {
			return name.substring("--".length());
		} else if (name.startsWith("-")) {
			return name.substring("-".length());
		} else {
			return name;
		}
	}

}
