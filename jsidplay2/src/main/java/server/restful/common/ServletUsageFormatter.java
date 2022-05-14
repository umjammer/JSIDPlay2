package server.restful.common;

import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;
import static server.restful.common.IServletSystemProperties.BASE_URL;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.beust.jcommander.DefaultUsageFormatter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.ParameterDescription;

import jakarta.servlet.http.HttpServletResponse;

public class ServletUsageFormatter extends DefaultUsageFormatter {

	private JCommander commander;
	private HttpServletResponse response;

	public ServletUsageFormatter(JCommander commander, HttpServletResponse response) {
		super(commander);
		this.commander = commander;
		this.response = response;
	}

	@Override
	public void appendMainLine(StringBuilder out, boolean hasOptions, boolean hasCommands, int indentCount,
			String indent) {
		response.setContentType(MIME_TYPE_TEXT.toString());
		String programName = commander.getProgramDisplayName();
		StringBuilder mainLine = new StringBuilder();
		mainLine.append(indent).append("Usage: ").append(BASE_URL).append(programName);

		if (commander.getMainParameter() != null && commander.getMainParameterDescription() != null) {
			mainLine.append("/").append(commander.getMainParameterDescription());
		}

		List<ParameterDescription> arguments = commander.getFields().values().stream()
				.filter(pd -> !pd.getParameter().hidden() && (pd.getParameterized().getType() != boolean.class
						|| pd.getParameterized().getParameter().arity() == 1))
				.sorted(commander.getParameterDescriptionComparator()).collect(Collectors.toList());

		if (arguments.size() > 0) {
			mainLine.append("?");
			arguments.forEach(parameterDescription -> {
				mainLine.append(getName(parameterDescription.getNames()));
				mainLine.append("=");
				if (!parameterDescription.getParameter().password()) {
					if (parameterDescription.getParameterized().getType() == UUID.class) {
						mainLine.append("12345678-0000-0000-0000-123456789012");
					} else {
						mainLine.append(parameterDescription.getDefault());
					}
				} else {
					mainLine.append("***");
				}
				if (!arguments.get(arguments.size() - 1).equals(parameterDescription)) {
					mainLine.append("&");
				}
			});
		}
		wrapDescription(out, indentCount, mainLine.toString());
		out.append("\n");
	}

	private String getName(String names) {
		String[] split = names.split(",");
		for (String name : split) {
			if (name.startsWith("--")) {
				return name.substring("--".length());
			} else if (name.startsWith("-")) {
				return name.substring("-".length());
			} else {
				return name;
			}
		}
		return names;
	}
}
