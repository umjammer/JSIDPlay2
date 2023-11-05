package server.restful.common;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class DefaultThreadFactory implements ThreadFactory {

	private ThreadFactory threadFactory;
	private String namePostfix;

	public DefaultThreadFactory(String namePostfix) {
		this.namePostfix = namePostfix;
		this.threadFactory = Executors.defaultThreadFactory();
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread newThread = threadFactory.newThread(r);
		newThread.setName(newThread.getName() + namePostfix);
		return newThread;
	}

}
