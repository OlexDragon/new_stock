package irt.components.beans;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import irt.components.beans.irt.Etc;
import irt.components.controllers.calibration.BtrRestController;
import irt.components.workers.IrtHttpRequest;

class OneCeUrlTest {
	private final static Logger logger = LogManager.getLogger();

	static Properties properties = new Properties();
	static OneCeUrl oneCeUrl;
	static OneCeUrl oneCeApiUrl;

	@BeforeAll
	public static void before() throws IOException {

		try(final InputStream is = OneCeUrlTest.class.getClassLoader().getResourceAsStream("application-w.properties");){
			properties.load(is);
			String scheme = properties.getProperty("irt.url.scheme");
			String userInfo = properties.getProperty("irt.url.userInfo");
			String url = properties.getProperty("irt.url");
			String urlApi = properties.getProperty("irt.url.api");

			oneCeUrl = new OneCeUrl(scheme, userInfo, url);
			oneCeApiUrl = new OneCeUrl(scheme, userInfo, urlApi);
		}
	}

	@Test
	void test() throws InterruptedException, ExecutionException, TimeoutException, IOException {

		final String path = "http://" + properties.getProperty("irt.url.login") + properties.getProperty("irt.url.api") + "/";
		URI uri = oneCeApiUrl.createUrl("travelers");
		logger.info("\n\t{}\n\t{}\n\t{}", properties.getProperty("irt.url.api"), path, uri);
		assertEquals(path + "travelers?$top=5", uri.toString());


		List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("sn", "2449037"));
		params.add(new BasicNameValuePair("section", "converter-tuning"));
		uri = oneCeApiUrl.createUrl("travelers", params);
		logger.info(uri);
		assertEquals(path + "travelers?sn=2449037&section=converter-tuning", uri.toString());

		String forObgect = IrtHttpRequest.getForString(uri, 5, TimeUnit.SECONDS);
		logger.error(forObgect);
        Map<String, String> result = BtrRestController.stringToMap(forObgect);
		logger.error(result);

		params.clear();
		params.add(new BasicNameValuePair("sn", "2449037"));
		params.add(new BasicNameValuePair("section", "unit-tuning-imd-3"));
		uri = oneCeApiUrl.createUrl("travelers", params);
		logger.info(uri);
		assertEquals(path + "travelers?sn=2449037&section=unit-tuning-imd-3", uri.toString());

		forObgect = IrtHttpRequest.getForString(uri, 5, TimeUnit.SECONDS);
		logger.error(forObgect);
		result = BtrRestController.stringToMap(forObgect);
		logger.error(result);
	}

	@Test
	void etcTest() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException {

		URL url = UriComponentsBuilder.newInstance()
				.scheme("http")
				.host("IRT-BUC-EMU3")
				.path("/device_debug_read.cgi")
				.build()
				.toUri()
				.toURL();

		final List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("devid", "1002"));
		params.add(new BasicNameValuePair("command", "regs"));
		params.add(new BasicNameValuePair("groupindex", "28"));
		final Etc etc = IrtHttpRequest.postForIrtObgect(url.toString(), Etc.class, params).get(10, TimeUnit.SECONDS);
		logger.error(etc);
	}

	@Test
	void createUrl() throws MalformedURLException {

		final List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("sn", "IRT-2525025".replaceAll("\\D", "")));
		params.add(new BasicNameValuePair("section", "converter"));
		URI uri = oneCeApiUrl.createUrl("travelers", params);

		logger.info(uri);
		assertEquals("http://" + properties.getProperty("irt.url.login") + properties.getProperty("irt.url") + "/category?$top=5", uri.toString());
	}
}
