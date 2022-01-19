package irt.components.workers;

import java.util.concurrent.ThreadFactory;

public class ThreadRunner {

	public static Thread runThread(Runnable runnable) {

		final Thread thread = newThread(runnable);
		thread.start();

		return thread;
	}

	public static Thread newThread(Runnable runnable) {

		final Thread thread = new Thread(runnable);
		int priority = thread.getPriority();

		if(priority>Thread.MIN_PRIORITY)
			thread.setPriority(--priority);

		thread.setDaemon(true);

		return thread;
	}

	public static ThreadFactory getThreadFactory() {
		return new ThreadFactory() {
			
			@Override
			public Thread newThread(Runnable runnable) {
				return ThreadRunner.newThread(runnable);
			}
		};
	}
}
