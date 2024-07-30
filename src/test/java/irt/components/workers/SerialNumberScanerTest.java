package irt.components.workers;

import java.io.IOException;

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
}
