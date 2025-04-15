package irt.components.controllers.calibration;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
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

import irt.components.beans.irt.ConverterInfo;
import irt.components.beans.irt.HomePageInfo;
import irt.components.workers.HttpRequest;

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

		final List<NameValuePair> list = new ArrayList<>();
		list.add(new BasicNameValuePair("devid", "1"));
		list.add(new BasicNameValuePair("command", "config"));
		String url = new URL("http", "IRT-2508001", "/device_debug_read.cgi").toString();
		final ConverterInfo converterInfo = HttpRequest.postForIrtYaml(url, ConverterInfo.class, list).get(5, TimeUnit.SECONDS);

		logger.error("converterInfo: {}", converterInfo);
	}
}
