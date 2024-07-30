package irt.components.controllers.calibration;

import java.io.IOException;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import irt.components.beans.irt.HomePageInfo;

class CalibrationControllerTest {
	private final static Logger logger = LogManager.getLogger();

	@Test
	void test() throws IOException {
		final Optional<HomePageInfo> homePageInfo = CalibrationController.getHomePageInfo("IRT-2415015");
		logger.error(homePageInfo);
	}

}
