package server.restful.common.parameter;

import static jakarta.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static jakarta.servlet.http.HttpServletResponse.SC_OK;
import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.IServletSystemProperties.BASE_URL;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
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
	private ResourceBundle bundle;

	public ServletUsageFormatter(JCommander commander, HttpServletRequest request, HttpServletResponse response) {
		super(commander);
		this.commander = commander;
		this.request = request;
		this.response = response;
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
			response.setStatus(exception != null ? SC_INTERNAL_SERVER_ERROR : SC_OK);
			response.setContentType(MIME_TYPE_TEXT.toString());

			if (exception != null) {
				appendErrorMessage(out);

				appendCurrentRequestParameters(out);
			}
			appendExampleRequest(out, indentCount, indent);

			if (commander.getMainParameter() != null && commander.getMainParameterValue() != null) {
				appendRequestPath(out, indent);
			}

			if (commander.getFields().size() > 0) {
				out.append(indent).append("Servlet Parameter").append("\n");
			}

		} catch (URISyntaxException | MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	private void appendErrorMessage(StringBuilder out) {
		out.append("Servlet-Parameter ERROR!");
		out.append("\n");
		out.append(exception.getClass().getSimpleName());
		out.append(": ");
		out.append(exception.getMessage());
		out.append("\n");
		out.append("\n");
	}

	private void appendCurrentRequestParameters(StringBuilder out) {
		String[] requestParameters = ServletParameterParser.getRequestParameters(request);
		if (requestParameters.length > 0) {
			out.append("Current Servlet-Parameter options:");
			out.append("\n");
			out.append(Strings.join(" ", requestParameters));
			out.append("\n");
			out.append("\n");
		}
	}

	private void appendExampleRequest(StringBuilder out, int indentCount, String indent)
			throws MalformedURLException, URISyntaxException {
		out.append("Example Usage:");
		out.append("\n");

		wrapDescription(out, indentCount, createExampleUsage(indent));
		out.append("\n");
		out.append("\n");
	}

	private void appendRequestPath(StringBuilder out, String indent) {
		out.append(indent).append("Servlet Path:");
		out.append("\n");
		out.append("      ");
		out.append(commander.getMainParameterValue().getDescription());
		out.append("\n");
		out.append("\n");
	}

	private String createExampleUsage(String indent) throws MalformedURLException, URISyntaxException {
		StringBuilder result = new StringBuilder();

		// protocol
		result.append(indent).append("HTTP(S)-");

		// request method
		result.append(request.getMethod()).append(" ");

		StringBuilder urlAsString = new StringBuilder();

		// base URL
		urlAsString.append(BASE_URL);

		// servlet path
		urlAsString.append(commander.getProgramDisplayName());

		// request path
		if (commander.getMainParameter() != null) {
			urlAsString.append(bundle.getString("EXAMPLE_REQUEST_PATH"));
		}

		// request parameters
		Iterator<ParameterDescription> it = commander.getFields().values().stream()
				.filter(pd -> !pd.getParameter().hidden()).sorted(commander.getParameterDescriptionComparator())
				.collect(Collectors.toList()).iterator();
		if (it.hasNext()) {
			urlAsString.append("?");
			while (it.hasNext()) {
				ParameterDescription parameterDescription = it.next();

				urlAsString.append(getName(parameterDescription.getNames()));
				urlAsString.append("=");
				urlAsString.append(createExampleParameterValue(parameterDescription));

				if (it.hasNext()) {
					urlAsString.append("&");
				}
			}
		}
		URL url = new URL(urlAsString.toString());
		URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(),
				url.getQuery(), url.getRef());
		result.append(uri.toASCIIString());

		return result.toString();
	}

	private String createExampleParameterValue(ParameterDescription parameterDescription) {
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
		return Arrays.asList(names.split(",")).stream().findFirst().map(this::getParameterName)
				.orElseGet(() -> getParameterName(names));
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
