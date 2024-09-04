package irt.components.controllers.calibration;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import irt.components.beans.irt.HomePageInfo;

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

}
