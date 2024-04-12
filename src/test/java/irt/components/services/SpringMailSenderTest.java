package irt.components.services;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import irt.components.beans.jpa.rma.Rma;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class SpringMailSenderTest {

	@Autowired private SpringMailSender mailSender;

	@Test
	void test() {
		mailSender.send("Test", "Test Message");
	}

	@Test
	void test2_1() throws InterruptedException {
		mailSender.send("Test", "Test Message", 1L, false);
		Thread.sleep(5000);
	}

	@Test
	void test2_2() throws InterruptedException {
		mailSender.send("Test", "Test Message", 1L, true);
		Thread.sleep(5000);
	}

	@Test
	void test3() throws InterruptedException {
		mailSender.send("Test", "Test Message", Rma.Status.READY);
		Thread.sleep(5000);
	}
}
