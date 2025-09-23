package irt.components.workers;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

class ProfileWorkerTest {
	Logger logger = LogManager.getLogger();

	@Test
	void test() throws IOException {
		final ProfileWorker profileWorker = new ProfileWorker("Z:/4alex/boards/profile", "IRT-1934005");
		logger.info(profileWorker);
		profileWorker.exists();
		final Optional<Double> gain = profileWorker.getGain();
		logger.info(gain);
		assertEquals("730", gain.get());
	}

}
