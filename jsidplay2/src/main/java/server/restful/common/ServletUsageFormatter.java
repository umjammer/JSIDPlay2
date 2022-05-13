package server.restful.common;

import static server.restful.common.ContentTypeAndFileExtensions.MIME_TYPE_TEXT;

import com.beust.jcommander.DefaultUsageFormatter;
import com.beust.jcommander.JCommander;

import jakarta.servlet.http.HttpServletResponse;

public class ServletUsageFormatter extends DefaultUsageFormatter {

	private HttpServletResponse response;

	public ServletUsageFormatter(JCommander commander, HttpServletResponse response) {
		super(commander);
		this.response = response;
	}

	@Override
	public void appendMainLine(StringBuilder out, boolean hasOptions, boolean hasCommands, int indentCount,
			String indent) {
		response.setContentType(MIME_TYPE_TEXT.toString());
		super.appendMainLine(out, false, hasCommands, indentCount, indent);
	}
}
