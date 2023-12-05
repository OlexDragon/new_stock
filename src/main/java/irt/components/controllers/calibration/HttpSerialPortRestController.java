package irt.components.controllers.calibration;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.client.ClientProtocolException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import irt.components.beans.HttpSerialPortServersCollector;
import irt.components.beans.irt.calibration.Command;
import irt.components.beans.irt.calibration.CommandRequest;
import irt.components.workers.HttpRequest;

@RestController
@RequestMapping("serial_port/rest")
public class HttpSerialPortRestController {
	private final static Logger logger = LogManager.getLogger();

	@Autowired
	private HttpSerialPortServersCollector httpSerialPortServersCollector;

	@PostMapping
    String calibrationInfo(@RequestParam String hostName, @RequestParam String port) {
    	logger.traceEntry("{}:{}", hostName, port);

    	httpSerialPortServersCollector.put(hostName, port);

    	return "Yee";
    }

	@PostMapping( value = "serial-ports", produces = "application/json;charset=utf-8")
    List<?> getSerialPorts(@RequestParam String hostName) throws URISyntaxException, ClientProtocolException, IOException, InterruptedException, ExecutionException, TimeoutException {
    	logger.traceEntry("hostName: {}", hostName );

    	final String port = httpSerialPortServersCollector.getPort(hostName);
    	final URL url =  new URL("http", hostName, ":" + port + "/serial-ports/");
		logger.debug(url);

		return HttpRequest.postForObgect(url.toString(), List.class, null).get(1, TimeUnit.SECONDS);
    }

	@PostMapping( value = "send", produces = "application/json;charset=utf-8")
	CommandRequest send(@RequestBody CommandRequest commandRequest) throws URISyntaxException, ClientProtocolException, IOException, InterruptedException, ExecutionException, TimeoutException {
    	logger.traceEntry("{}", commandRequest );

    	final List<Command> commands = commandRequest.getCommands();
    	if(commands.isEmpty())
    			return null;

    	final String hostName = commandRequest.getHostName();
    	final String port = httpSerialPortServersCollector.getPort(hostName);
    	final URL url =  new URL("http", hostName, ":" + port);
		logger.debug(url);

    	return HttpRequest.postForObgect(url, CommandRequest.class, commandRequest).get(15, TimeUnit.SECONDS);
	}

	@ExceptionHandler(TimeoutException.class)
	  public void conflict(Exception e) {
		logger.catching(e);
	  }
}
