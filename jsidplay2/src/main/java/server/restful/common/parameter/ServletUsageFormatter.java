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
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.beust.jcommander.DefaultUsageFormatter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;
import com.beust.jcommander.Parameters;
import com.beust.jcommander.Strings;
import com.beust.jcommander.WrappedParameter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Change usage message to represent a servlet call. Servlet path is treated
 * like a main argument and servlet parameters as options.
 * 
 * @author khaendel
 *
 */
public class ServletUsageFormatter extends DefaultUsageFormatter {

	private static final Logger LOG = Logger.getLogger(ServletUsageFormatter.class.getName());

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
		response.setStatus(exception != null ? SC_INTERNAL_SERVER_ERROR : SC_OK);
		response.setContentType(MIME_TYPE_TEXT.toString());

		if (LOG.isLoggable(Level.FINEST)) {
			appendCurrentRequestParameters(out, indent);
		}

		if (exception != null) {
			appendErrorMessage(out);
		}

		out.append(indent).append("Usage: ");
		out.append(commander.getProgramDisplayName());
		if (commander.getMainParameter() != null && commander.getMainParameterValue() != null) {
			out.append("[<servlet path>]");
		}
		if (hasOptions) {
			out.append("?[<servlet parameters>]");
		}
		out.append("\n");

		if (commander.getMainParameter() != null && commander.getMainParameterValue() != null) {
			appendServletPath(out, indentCount, indent);
		}
	}

	@Override
	public void appendAllParametersDetails(StringBuilder out, int indentCount, String indent,
			List<ParameterDescription> sortedParameters) {
		if (sortedParameters.size() > 0) {
			out.append("\n");
			out.append(indent).append("  Servlet Parameters:\n");
		}

		for (ParameterDescription pd : sortedParameters) {
			WrappedParameter parameter = pd.getParameter();
			String description = pd.getDescription();
			boolean hasDescription = !description.isEmpty();

			// First line, command name
			out.append(indent).append("  ").append(parameter.required() ? "* " : "  ")
					.append(getServletParameterNames(pd.getNames())).append("\n");

			if (hasDescription) {
				wrapDescription(out, indentCount, s(indentCount) + description);
			}
			Object def = pd.getDefault();

			if (pd.isDynamicParameter()) {
				String syntax = "Syntax: " + parameter.names()[0] + "key" + parameter.getAssignment() + "value";

				if (hasDescription) {
					out.append(newLineAndIndent(indentCount));
				} else {
					out.append(s(indentCount));
				}
				out.append(syntax);
			}

			if (def != null && !pd.isHelp()) {
				String displayedDef = Strings.isStringEmpty(def.toString()) ? "<empty string>" : def.toString();
				String defaultText = "Default: " + (parameter.password() ? "********" : displayedDef);

				if (hasDescription) {
					out.append(newLineAndIndent(indentCount));
				} else {
					out.append(s(indentCount));
				}
				out.append(defaultText);
			}
			Class<?> type = pd.getParameterized().getType();

			if (type.isEnum()) {
				@SuppressWarnings({ "unchecked", "rawtypes" })
				String valueList = EnumSet.allOf((Class<? extends Enum>) type).toString();
				String possibleValues = "Possible Values: " + valueList;

				// Prevent duplicate values list, since it is set as 'Options: [values]' if the
				// description
				// of an enum field is empty in ParameterDescription#init(..)
				if (!description.contains("Options: " + valueList)) {
					if (hasDescription) {
						out.append(newLineAndIndent(indentCount));
					} else {
						out.append(s(indentCount));
					}
					out.append(possibleValues);
				}
			}
			out.append("\n");
		}
		try {
			appendExampleUsage(out, indentCount, indent);
		} catch (URISyntaxException | MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	private void appendCurrentRequestParameters(StringBuilder out, String indent) {
		String[] requestParameters = ServletParameterParser.getRequestParameters(request);
		if (requestParameters.length > 0) {
			out.append(indent)
					.append("  Internal Info: Current servlet parameters converted to command line arguments:");
			out.append("\n");
			out.append(Strings.join(" ", requestParameters));
			out.append("\n");
			out.append("\n");
		}
	}

	private void appendErrorMessage(StringBuilder out) {
		out.append(exception.getClass().getSimpleName());
		out.append(":");
		out.append(newLineAndIndent(2));
		out.append(exception.getMessage().replaceAll("[mM]ain parameters? are", "servlet path is")
				.replaceAll("[mM]ain parameters?", "servlet path").replaceAll("[oO]ption", "servlet parameter")
				.replaceAll("--", "").replaceAll("-", ""));
		out.append("\n");
		out.append("--------------------------------------------------------------------------------");
		out.append("\n");
		out.append("\n");
	}

	private void appendServletPath(StringBuilder out, int indentCount, String indent) {
		out.append(newLineAndIndent(2)).append("Servlet Path:");
		out.append("\n");
		out.append(indent).append("  ")
				.append(commander.getMainParameterValue().getParameter().required() ? "* " : "  ");
		out.append(commander.getMainParameterValue().getDescription());
		out.append("\n");
	}

	private void appendExampleUsage(StringBuilder out, int indentCount, String indent)
			throws MalformedURLException, URISyntaxException {
		out.append(newLineAndIndent(2)).append("Example:");
		out.append("\n");

		wrapDescription(out, indentCount, s(indentCount) + createExampleUsage(indent));
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
				ParameterDescription def = it.next();

				urlAsString.append(getExampleServletParameterName(def.getNames()));
				urlAsString.append("=");
				urlAsString.append(def.getParameter().password() ? "********" : String.valueOf(def.getDefault()));

				if (it.hasNext()) {
					urlAsString.append("&");
				}
			}
		}
		URL url = new URI(urlAsString.toString()).toURL();
		URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(),
				url.getQuery(), url.getRef());
		result.append(uri.toASCIIString());

		return result.toString();
	}

	private String getExampleServletParameterName(String names) {
		return Arrays.asList(names.split(", ")).stream().map(this::getParameterName)
				.max(Comparator.comparingInt(String::length)).orElse("");
	}

	private ResourceBundle getBundle() {
		Object firstParameterObject = commander.getObjects().get(0);
		Parameters parameters = firstParameterObject.getClass().getAnnotation(Parameters.class);
		return ResourceBundle.getBundle(parameters.resourceBundle(), Locale.getDefault());
	}

	private String getServletParameterNames(String names) {
		return Arrays.asList(names.split(", ")).stream().map(this::getParameterName).collect(Collectors.joining(", "));
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

	/**
	 * Returns new line followed by indent-many spaces.
	 *
	 * @return new line followed by indent-many spaces
	 */
	private static String newLineAndIndent(int indent) {
		return "\n" + s(indent);
	}

}
