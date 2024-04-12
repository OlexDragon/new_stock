package irt.components.beans;

import static java.util.concurrent.TimeUnit.MINUTES;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import lombok.Getter;

@Getter
public class HttpSerialPortServersKeeper {

	private final static long MILLIS = MINUTES.toMillis(30);

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

			final List<String> toRemove = new ArrayList<>();
			final long current = System.currentTimeMillis();

			timestamps.entrySet()
			.forEach(
					entry->{
						long differenceMillis = current - entry.getValue();
						if(differenceMillis > MILLIS)
							toRemove.add(entry.getKey());
					});

			toRemove.forEach(
					key->{
						httpSerialPortServers.remove(key);
						timestamps.remove(key);
					});
		}
	};

	private final  ScheduledFuture<?> s = service.scheduleAtFixedRate(runnable, 30, 30, MINUTES);
}
