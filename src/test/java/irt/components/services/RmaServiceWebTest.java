package irt.components.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import irt.components.beans.jpa.rma.Rma;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class RmaServiceWebTest {

	@Autowired RmaServiceWeb web;

	@Test
	void test() {

		final List<Long> rmaIdsByStatus = web.rmaIdsByStatus(Rma.Status.FINALIZED, Rma.Status.FIXED, Rma.Status.READY);
		assertEquals(1, rmaIdsByStatus.size());
	}

}
