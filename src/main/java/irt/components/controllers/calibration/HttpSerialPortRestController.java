package irt.components.controllers.calibration;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import irt.components.beans.irt.calibration.Command;
import irt.components.beans.irt.calibration.CommandBytesRequest;
import irt.components.beans.irt.calibration.CommandRequest;
import irt.components.beans.irt.calibration.RequestData;
import irt.components.services.HttpSerialPortServersKeeper;
import irt.components.workers.HttpRequest;

@RestController
@RequestMapping("serial_port/rest")
public class HttpSerialPortRestController {
	private final static Logger logger = LogManager.getLogger();

	@Autowired
	private HttpSerialPortServersKeeper serversKeeper;

	@PostMapping
    String calibrationInfo(@RequestParam String hostName, @RequestParam String port) {
    	logger.traceEntry("{}:{}", hostName, port);

    	serversKeeper.put(hostName, port);

    	return "Yee";
    }

	@PostMapping( value = "serial-ports", produces = "application/json;charset=utf-8")
    List<?> getSerialPorts(@RequestParam String hostName){
    	logger.traceEntry("hostName: {}", hostName );

    	return Optional.ofNullable(serversKeeper.getPort(hostName))
    	.flatMap(
    			port->{
    				try {

    					return Optional.of(new URL("http", hostName, ":" + port + "/serial-ports/"));

    				} catch (MalformedURLException e) {
    					logger.catching(e);
    					return Optional.empty();
    				}
    			})
    	.flatMap(url->{
			try {

				return Optional.of(HttpRequest.postForObgect(url.toString(), List.class, null).get(3, TimeUnit.SECONDS));

			} catch (InterruptedException | ExecutionException | TimeoutException e) {
				logger.warn("Can not get Serial Ports from {}", url);
				return Optional.empty();
			}
		})
    	.orElseGet(()->new ArrayList<>());
    }

	@PostMapping( value = "send", produces = "application/json;charset=utf-8")
	CommandRequest send(@RequestBody CommandRequest commandRequest){
    	logger.traceEntry("{}", commandRequest );

    	final List<Command> commands = commandRequest.getCommands();
    	if(commands.isEmpty()) {
    		logger.info("There are no commands in the request.");
    		return commandRequest.setError("There are no commands in the request.");
    	}

    	final String hostName = commandRequest.getHostName();
    	final String port = serversKeeper.getPort(hostName);

    	if(port==null) {
    		logger.warn("Serial Port Server: Unknown Host Name: " + hostName);
    		return commandRequest.setError("Serial Port Server: Unknown Host Name: " + hostName);
    	}

    	try {

    		final URL url =  new URL("http", hostName, ":" + port);
			logger.debug(url);

			return HttpRequest.postForObgect(url, CommandRequest.class, commandRequest).get(15, TimeUnit.SECONDS);

    	} catch (InterruptedException | ExecutionException | TimeoutException e) {

    		logger.warn("No Answer. {}", commandRequest);
			return commandRequest.setError(e.getLocalizedMessage());

    	} catch (MalformedURLException e) {
			logger.catching(e);
			return commandRequest.setError(e.getLocalizedMessage());
		}
	}

	@PostMapping( value = "send-bytes")
	CommandBytesRequest sendBytes(@RequestBody CommandBytesRequest commandRequest){
    	logger.traceEntry("{}", commandRequest );

    	final String hostName = commandRequest.getHostName();
		final String port = serversKeeper.getPort(hostName);
    	if(port==null) {
    		commandRequest.setErrorMessage("No connection to the HTTP Serial Port. Host Name: " + hostName);
    		return commandRequest;
    	}

		try {

			final URL url =  new URL("http", hostName, ":" + port + "/bytes");
			logger.debug(url);

			CommandBytesRequest result = new RestTemplate().postForObject(url.toURI(), commandRequest, CommandBytesRequest.class);
			return result;

		} catch (MalformedURLException | RestClientException | URISyntaxException e) {
			logger.catching(e);
			commandRequest.setErrorMessage(e.getLocalizedMessage());
		}
		return commandRequest;
	}

	@ExceptionHandler(TimeoutException.class)
	  public void conflict(Exception e) {
		logger.catching(e);
	  }

	@PostMapping( value = "read-bytes")
	CommandBytesRequest readBytes(@RequestBody RequestData requestData){
    	logger.traceEntry("{}", requestData );

    	final String hostName = requestData.getHostName();
		final String port = serversKeeper.getPort(hostName);
    	if(port==null) {
    		final CommandBytesRequest commandRequest = new CommandBytesRequest();
    		commandRequest.setErrorMessage("No connection to the HTTP Serial Port. Host Name: " + hostName);
    		return commandRequest;
    	}

		try {

			final URL url =  new URL("http", hostName, ":" + port + "/read");
			logger.debug(url);

			CommandBytesRequest result = new RestTemplate().postForObject(url.toURI(), requestData, CommandBytesRequest.class);
			return result;

		} catch (MalformedURLException | RestClientException | URISyntaxException e) {
			logger.catching(e);
			final CommandBytesRequest commandRequest = new CommandBytesRequest();
			commandRequest.setErrorMessage(e.getLocalizedMessage());
			return commandRequest;
		}
	}
}
