package irt.components.controllers.calibration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import irt.components.beans.OneCeHeader;
import irt.components.beans.OneCeUrl;

class OneCeHeaderTest {
	private final static Logger logger = LogManager.getLogger();

	@Test
	void test() throws InterruptedException, ExecutionException, TimeoutException {
		final OneCeUrl oneCeUrl = new OneCeUrl("http://", "Oleksandr%20P.:Ki6kabiz@", "192.168.20.241/irt-prod-web/hs/api/");
		final OneCeHeader oneCeHeader = OneCeRestController.getOneCHeader(oneCeUrl, "IRT-2509005").get(10, TimeUnit.SECONDS);
		logger.error(oneCeHeader);
		assertNotNull(oneCeHeader);
	}

}
