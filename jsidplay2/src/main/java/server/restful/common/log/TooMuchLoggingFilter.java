package server.restful.common.log;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

import server.restful.servlets.CountLogsServlet;
import server.restful.servlets.LogsServlet;
import server.restful.servlets.hls.OnKeepAliveServlet;
import server.restful.servlets.hls.ProxyServlet;

/**
 * Configurable log filter usable for certain appenders configured in
 * logconfig.properties. The purpose is to prevent log messages, that fire at a
 * very high frequency rate.
 */
public class TooMuchLoggingFilter implements Filter {

	@Override
	public boolean isLoggable(LogRecord record) {
		return !(record.getMessage().startsWith(OnKeepAliveServlet.class.getSimpleName())
				|| record.getMessage().startsWith(ProxyServlet.class.getSimpleName())
				|| record.getMessage().startsWith(LogsServlet.class.getSimpleName())
				|| record.getMessage().startsWith(CountLogsServlet.class.getSimpleName()));
	}
}
