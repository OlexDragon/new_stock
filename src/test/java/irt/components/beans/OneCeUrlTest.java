package irt.components.beans;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;

import irt.components.beans.irt.Etc;
import irt.components.controllers.calibration.BtrRestController;
import irt.components.workers.HttpRequest;

class OneCeUrlTest {
	private final static Logger logger = LogManager.getLogger();

	@Test
	void test() throws InterruptedException, ExecutionException, TimeoutException, JsonMappingException, JsonProcessingException {
		final OneCeUrl oneCeUrl = new OneCeUrl("http://", "Oleksandr%20P.:Ki6kabiz@", "192.168.20.241/irt-prod-web/hs/api/");
		String url = oneCeUrl.createUrl("travelers");
		assertEquals("http://Oleksandr%20P.:Ki6kabiz@192.168.20.241/irt-prod-web/hs/api/travelers?$top=5", url);


		List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("sn", "2449037"));
		params.add(new BasicNameValuePair("section", "converter-tuning"));
		url = oneCeUrl.createUrl("travelers", params);
		assertEquals("http://Oleksandr%20P.:Ki6kabiz@192.168.20.241/irt-prod-web/hs/api/travelers?sn=2449037&section=converter-tuning", url);

		String forObgect = HttpRequest.getForString(url, 5, TimeUnit.SECONDS);
		logger.error(forObgect);
        Map<String, String> result = BtrRestController.stringToMap(forObgect);
		logger.error(result);

		params.clear();
		params.add(new BasicNameValuePair("sn", "2449037"));
		params.add(new BasicNameValuePair("section", "unit-tuning"));
		url = oneCeUrl.createUrl("travelers", params);
		assertEquals("http://Oleksandr%20P.:Ki6kabiz@192.168.20.241/irt-prod-web/hs/api/travelers?sn=2449037&section=unit-tuning", url);

		forObgect = HttpRequest.getForString(url, 5, TimeUnit.SECONDS);
		logger.error(forObgect);
		result = BtrRestController.stringToMap(forObgect);
		logger.error(result);
	}

	@Test
	void etcTest() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException {
		URL url = new URL("http", "irt-2415015", "/device_debug_read.cgi");
		final List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("devid", "1002"));
		params.add(new BasicNameValuePair("command", "regs"));
		params.add(new BasicNameValuePair("groupindex", "28"));
		final Etc etc = HttpRequest.postForIrtObgect(url.toString(), Etc.class, params).get(10, TimeUnit.SECONDS);
		logger.error(etc);
	}
}
