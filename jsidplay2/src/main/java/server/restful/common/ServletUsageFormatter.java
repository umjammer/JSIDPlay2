package server.restful.common;

import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.IServletSystemProperties.BASE_URL;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;

import com.beust.jcommander.DefaultUsageFormatter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ServletUsageFormatter extends DefaultUsageFormatter {

	private JCommander commander;
	private HttpServletRequest request;
	private HttpServletResponse response;
	private Exception exception;

	public ServletUsageFormatter(JCommander commander, HttpServletRequest request, HttpServletResponse response) {
		super(commander);
		this.commander = commander;
		this.request = request;
		this.response = response;
	}

	public void setException(Exception Exception) {
		this.exception = Exception;
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
			}

			StringBuilder mainLine = new StringBuilder();
			mainLine.append(indent).append("HTTP-").append(request.getMethod()).append(" ");

			StringBuilder urlAsString = new StringBuilder();
			// Base url + servlet path
			urlAsString.append(BASE_URL).append(commander.getProgramDisplayName());

			// Request path
			if (commander.getMainParameter() != null && commander.getMainParameterValue() != null) {
				urlAsString.append("/").append(commander.getMainParameterValue().getParameterized().getName());
			}

			List<ParameterDescription> arguments = commander.getFields().values().stream()
					.filter(pd -> !pd.getParameter().hidden() && (pd.getParameterized().getType() != boolean.class
							|| pd.getParameterized().getParameter().arity() == 1))
					.sorted(commander.getParameterDescriptionComparator()).collect(Collectors.toList());

			// Query parameters
			if (arguments.size() > 0) {
				urlAsString.append("?");
				arguments.forEach(parameterDescription -> {
					String displayedDef = String.valueOf(parameterDescription.getDefault());

					urlAsString.append(getName(parameterDescription.getNames()));
					urlAsString.append("=");
					urlAsString.append(parameterDescription.getParameter().password() ? "********" : displayedDef);

					if (!arguments.get(arguments.size() - 1).equals(parameterDescription)) {
						urlAsString.append("&");
					}
				});
			}
			URL url = new URL(urlAsString.toString());
			URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(),
					url.getQuery(), url.getRef());

			String asciiString = uri.toASCIIString();
			if (commander.getMainParameter() != null && commander.getMainParameterValue() != null) {
				asciiString = asciiString.replace(commander.getMainParameterValue().getParameterized().getName(),
						"{" + commander.getMainParameterValue().getParameterized().getName() + "}");
			}
			mainLine.append(asciiString);
			wrapDescription(out, indentCount, mainLine.toString());
			out.append("\n");
			out.append("\n");

			// Request path description
			if (commander.getMainParameter() != null && commander.getMainParameterValue() != null) {
				out.append("{");
				out.append(commander.getMainParameterValue().getParameterized().getName());
				out.append("}: ");
				out.append(commander.getMainParameterValue().getDescription());
				out.append("\n");
				out.append("\n");
			}
			if (arguments.size() > 0) {
				out.append(indent).append("Servlet-Parameter");
			}

		} catch (URISyntaxException | MalformedURLException e) {
			throw new RuntimeException(e);
		}
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
