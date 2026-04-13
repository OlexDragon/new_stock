package irt.components.controllers.components;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriUtils;

import irt.components.beans.SerialNumber;
import irt.components.workers.IrtHttpRequest;

@RestController
@RequestMapping("onrender")
public class OnRenderController {
	private final static Logger logger = LogManager.getLogger();

	@Value("${irt.onRender.serialNumber.get}")
	private String onRenderSNPath;

	@Value("${irt.onRender.serialNumber.save}")
	private String onRenderSNSavePath;

	@Value("${irt.onRender.serialNumber.exists}")
	private String existsPath;

	private HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10)) // Connection timeout
            .build();

	@GetMapping({"", "/", "check-sn", "exists"})
	Boolean isOnRenderSNExists(@RequestParam String sn) throws InterruptedException, ExecutionException, TimeoutException, IOException {

		final URI uri = URI.create(existsPath + "?sn=" + UriUtils.encode(sn, StandardCharsets.UTF_8));
		logger.debug("Checking SN: {}, URI: {}", sn, uri);

		HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(5)) // Request timeout
                .header("Accept", "application/json")
                .GET()
                .build();
		HttpResponse<String> stringResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		final String body = stringResponse.body();
		logger.info("Response code: {}, body: {}", stringResponse.statusCode(), body);
		return body.equalsIgnoreCase("true");
	}

	@GetMapping("sn")
	SerialNumber getOnRenderSN(@RequestParam String sn) throws InterruptedException, ExecutionException, TimeoutException, IOException {
		String url = onRenderSNPath + "?sn=" + sn.toUpperCase();
		logger.trace(url);
		final FutureTask<SerialNumber> ft = IrtHttpRequest.getForObgect(url, SerialNumber.class);
		return ft.get(5, TimeUnit.SECONDS);
	}
	@PostMapping("save-sn")
	String saveSN(@RequestParam String sn, @RequestParam String pn, @RequestParam String descr) throws InterruptedException, ExecutionException, TimeoutException, IOException {

		final URI uri = URI.create(onRenderSNSavePath + "?sn=" + UriUtils.encode(sn, StandardCharsets.UTF_8) + "&pn=" + UriUtils.encode(pn, StandardCharsets.UTF_8) + "&descr=" + UriUtils.encode(descr, StandardCharsets.UTF_8));
		logger.debug("Saving SN: {}, PN: {}, descr: {}, URI: {}", sn, pn, descr, uri);

		HttpRequest request = HttpRequest.newBuilder()
				.uri(uri)
				.timeout(Duration.ofSeconds(5)) // Request timeout
				.header("Accept", "application/json")
				.POST(HttpRequest.BodyPublishers.noBody())
				.build();

		HttpResponse<String> stringResponse = client.send(request, HttpResponse.BodyHandlers.ofString());
		final String body = stringResponse.body();
		logger.info("Response code: {}, body: {}", stringResponse.statusCode(), body);
		return body;
	}
}
