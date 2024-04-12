package irt.components.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import irt.components.beans.jpa.rma.Rma.Status;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class RmaServiceWebTest {

	@Autowired RmaServiceWeb web;

	@Test
	void test() {

		final List<Long> rmaIdsByStatus = web.rmaIdsByStatus(Status.CREATED);
		assertEquals(rmaIdsByStatus.size(), 6);
	}

}
