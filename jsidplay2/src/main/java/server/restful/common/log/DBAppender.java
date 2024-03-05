package server.restful.common.log;

import static server.restful.JSIDPlay2Server.freeDebugEntityManager;
import static server.restful.JSIDPlay2Server.getDebugEntityManager;

import java.io.IOException;
import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import ui.entities.debug.service.DebugService;

public class DBAppender extends Handler {

	@Override
	public void publish(LogRecord record) {
		try {
			DebugService debugService = new DebugService(getDebugEntityManager());
			debugService.save(record);
		} catch (IOException e) {
			reportError(null, e, ErrorManager.WRITE_FAILURE);
		} finally {
			freeDebugEntityManager();
		}
	}

	@Override
	public void flush() {
	}

	@Override
	public void close() throws SecurityException {
	}

}
