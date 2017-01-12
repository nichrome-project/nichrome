package nichrome.mln.util;

import java.util.concurrent.ThreadFactory;

public class NamedThreadFactory implements ThreadFactory {
	private final String name;
	private final boolean daemon;

	public NamedThreadFactory(String name, boolean daemon) {
		this.name = name;
		this.daemon = daemon;
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, this.name);
		t.setDaemon(this.daemon);
		return t;
	}
}
