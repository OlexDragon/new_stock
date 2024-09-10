package irt.components.workers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import irt.components.services.SerialNumberScaner;

@SpringBootTest
class SerialNumberScanerTest {

	@Autowired SerialNumberScaner scaner;

	@Test
	void test() throws IOException {
		scaner.scan();
	}

	@Test
	void rmaReadyToShipLocalTest() throws IOException {
		final List<Long> list = scaner.rmaReadyToShipLocal();
		assertEquals(5, list.size());
	}
}
