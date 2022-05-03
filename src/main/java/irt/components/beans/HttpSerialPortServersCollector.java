package irt.components.beans;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;

@Getter
public class HttpSerialPortServersCollector {

	private final Map<String, String> httpSerialPortServers = new HashMap<>();

	public void put(String hostName, String port) {
    	httpSerialPortServers.put(hostName, port);
	}

	public String getPort(String hostName) {
    	return httpSerialPortServers.get(hostName);
	}

}
