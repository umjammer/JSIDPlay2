package server.restful.common;

import com.beust.jcommander.DefaultUsageFormatter;
import com.beust.jcommander.JCommander;

public class ServletUsageFormatter extends DefaultUsageFormatter {

	public ServletUsageFormatter(JCommander commander) {
		super(commander);
	}

	@Override
	public void appendMainLine(StringBuilder out, boolean hasOptions, boolean hasCommands, int indentCount,
			String indent) {
		super.appendMainLine(out, false, hasCommands, indentCount, indent);
	}
}
