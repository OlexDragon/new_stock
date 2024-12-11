package irt.components.services;

import static java.util.concurrent.TimeUnit.MINUTES;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.web.client.RestTemplate;

import lombok.Getter;

@Getter
public class HttpSerialPortServersKeeper {
	private final static Logger logger = LogManager.getLogger();


	private final Map<String, String> httpSerialPortServers = new HashMap<>();

	public synchronized String getPort(String hostName) {
		logger.traceEntry("hostName: {}; {}", hostName, httpSerialPortServers);
    	return httpSerialPortServers.get(hostName);
	}

	public synchronized void put(String hostName, String port) {
		logger.traceEntry("hostName: {}; port: {}", hostName, port);
    	httpSerialPortServers.put(hostName, port);
	}

	public URL getUrl(String hostName) {
		logger.traceEntry("hostName: {};", hostName);
		final String port = httpSerialPortServers.get(hostName);
		return Optional.ofNullable(port).map(p->{
			try {

				return new URL("http", hostName, ":" + port);

			} catch (MalformedURLException e) {
				logger.catching(e);
				return null;
			}
		}).orElse(null);
	}
	// Scheduled task to remove unexisting Serial Port Servers
	private final  ScheduledExecutorService service =  Executors.newScheduledThreadPool(1);
	private final  Runnable runnable = new Runnable() {
		
		@Override
		public void run() {

			final List<String> toRemove = new ArrayList<>();

			httpSerialPortServers.entrySet()
			.forEach(
					entry->{
						try {

							final URL url = new URL("http", entry.getKey(), ":" + entry.getValue() + "/ping/");
							new RestTemplate().getForObject(url.toURI(), Boolean.class);

						} catch (Exception e) {
							logger.info("Can not connect  to http://{}:{}/ping", entry.getKey(), entry.getValue());
//							logger.catching(e);
							toRemove.add(entry.getKey());
						}
					});

			removeUnreachable(toRemove);
		}
	};

	private synchronized void removeUnreachable(final List<String> toRemove) {
		if(!toRemove.isEmpty())
			toRemove.forEach(httpSerialPortServers::remove);
	}

	private final  ScheduledFuture<?> s = service.scheduleAtFixedRate(runnable, 30, 30, MINUTES);
}
