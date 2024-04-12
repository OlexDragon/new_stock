package irt.components.services;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SerialNumberScanerTest {
	private final Logger logger = LogManager.getLogger();

	@Value("${irt.onRender.serialNumber}") 	private String onRenderSN;
	@Autowired SerialNumberScaner scaner;

	@Test
	void test() throws IOException {
		scaner.scan();
	}

	@Test
	void existsTest() throws IOException, URISyntaxException {

		List<NameValuePair> params = new ArrayList<NameValuePair>(1);
		params.add(new BasicNameValuePair("sn","irt-2412001"));
		final StringBuffer sb = new StringBuffer(onRenderSN).append("/exists");
		logger.error(sb);
		assertTrue(sendRequest(sb, params));
	}

	@Test
	void endWithTest() throws IOException, URISyntaxException {

		List<NameValuePair> params = new ArrayList<NameValuePair>(1);
		params.add(new BasicNameValuePair("sn","2412001"));
		final StringBuffer sb = new StringBuffer(onRenderSN).append("/ends-with");
		logger.error(sb);
		final String sendRequest2 = sendRequest2(sb, params);
		logger.error(sendRequest2);
		assertTrue(sendRequest2.startsWith("{\"id\":"));
		assertNotNull(sendRequest2);
	}

	@Test
	void rmaReadyToShipReminderTest() throws IOException, URISyntaxException, InterruptedException {
		scaner.rmaReadyToShipReminder();
		Thread.sleep(3000);
	}

	@Test
	void rmaReadyToShipLocalTest() throws IOException, URISyntaxException, InterruptedException {
		 List<Long> list = scaner.rmaReadyToShipLocal();
		assertEquals(7, list.size());
	}

	public boolean sendRequest(final StringBuffer sbURL, List<NameValuePair> params) throws URISyntaxException {
		try {
			HttpClient httpclient = HttpClients.createDefault();
			HttpGet httpGet = new HttpGet(sbURL.toString());
			URI uri = new URIBuilder(httpGet.getURI()).addParameters(params).build();
			httpGet.setURI(uri);
			HttpResponse response = httpclient.execute(httpGet);

			return Optional.ofNullable(response.getEntity())

					.map(
							entity->{

								try (InputStream inputStream = entity.getContent()) {
									final BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream, StandardCharsets.UTF_8));
									return bufferedReader.lines().findAny().map(s->s.equals("true")).orElse(false);

								} catch (UnsupportedOperationException | IOException e) {
									logger.catching(e);
									return false;
								}
							}).orElse(false);

		} catch (IOException e) {
			logger.catching(e);
			return false;
		}
	}

	public String sendRequest2(final StringBuffer sbURL, List<NameValuePair> params) throws URISyntaxException {
		try {
			HttpClient httpclient = HttpClients.createDefault();
			HttpGet httpGet = new HttpGet(sbURL.toString());
			URI uri = new URIBuilder(httpGet.getURI()).addParameters(params).build();
			httpGet.setURI(uri);
			HttpResponse response = httpclient.execute(httpGet);

			return Optional.ofNullable(response.getEntity())

					.map(
							entity->{

								try (InputStream inputStream = entity.getContent()) {
									final BufferedReader bufferedReader = new BufferedReader( new InputStreamReader(inputStream, StandardCharsets.UTF_8));
									return bufferedReader.lines().collect(Collectors.joining("\n"));

								} catch (UnsupportedOperationException | IOException e) {
									logger.catching(e);
									return null;
								}
							}).orElse(null);

		} catch (IOException e) {
			logger.catching(e);
			return null;
		}
	}
}
