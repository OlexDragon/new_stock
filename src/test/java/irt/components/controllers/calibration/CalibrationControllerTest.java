package irt.components.controllers.calibration;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import irt.components.beans.irt.ConverterInfo;
import irt.components.beans.irt.HomePageInfo;
import irt.components.workers.IrtHttpRequest;

class CalibrationControllerTest {
	private final static Logger logger = LogManager.getLogger();

//	static {
//
//		Configurator.setRootLevel(Level.ALL);
//	}
	@Test
	void test() throws IOException, InterruptedException, ExecutionException, TimeoutException {
		logger.error("Start");
		final Optional<HomePageInfo> homePageInfo = CalibrationController.getHomePageInfo("192.168.30.151", 500);
		logger.error(homePageInfo);
	}

	@Test
	void configTest() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException{

		String url = UriComponentsBuilder.newInstance()
				.scheme("http")
				.host("IRT-BUC-EMU3")
				.path("/device_debug_read.cgi")
				.toUriString();

		final List<NameValuePair> list = new ArrayList<>();
		list.add(new BasicNameValuePair("devid", "1"));
		list.add(new BasicNameValuePair("command", "config"));
		final ConverterInfo converterInfo = IrtHttpRequest.postForIrtYaml(url, ConverterInfo.class, list).get(5, TimeUnit.SECONDS);

		logger.error("converterInfo: {}", converterInfo);
	}
}
