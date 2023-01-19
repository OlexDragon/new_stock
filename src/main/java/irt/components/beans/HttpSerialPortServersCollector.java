package irt.components.beans;

import static java.util.concurrent.TimeUnit.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import lombok.Getter;

@Getter
public class HttpSerialPortServersCollector {

	private final Map<String, String> httpSerialPortServers = new HashMap<>();
	private final Map<String, Long> timestamps = new HashMap<>();

	public synchronized void put(String hostName, String port) {
    	httpSerialPortServers.put(hostName, port);
    	timestamps.put(hostName, System.currentTimeMillis());
	}

	public String getPort(String hostName) {
    	return httpSerialPortServers.get(hostName);
	}

	// Scheduled task to remove unexisting Serial Port Servers
	private final  ScheduledExecutorService service =  Executors.newScheduledThreadPool(1);
	private final  Runnable runnable = new Runnable() {
		
		@Override
		public void run() {
			final long current = System.currentTimeMillis();
			final Set<Entry<String, Long>> entrySet = new HashSet<>(timestamps.entrySet());
			entrySet.forEach(
					e->{
						long differenceMillis = e.getValue() - current;
						if(differenceMillis > MINUTES.toMillis(30)) {

							final String key = e.getKey();

							synchronized(this) {
								httpSerialPortServers.remove(key);
								timestamps.remove(key);
							}
						};
					});
		}
	};
	private final  ScheduledFuture<?> s = service.scheduleAtFixedRate(runnable, 30, 30, MINUTES);
}
