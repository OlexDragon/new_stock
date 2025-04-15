package irt.components.services;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;

import irt.components.beans.jpa.rma.Comment;
import irt.components.beans.jpa.rma.Rma;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class RmaServiceTest {
	final static Logger logger = LogManager.getLogger();

	@Autowired RmaServiceWeb web;
	@Autowired RmaServiceLocal local;

	@Test
	void test() {

		final List<Long> rmaIdsByStatus = web.rmaIdsByStatus(Rma.Status.FINALIZED, Rma.Status.FIXED, Rma.Status.READY);
		assertEquals(1, rmaIdsByStatus.size());
	}

	@Test
	void dateTest() {
		Optional<Comment> comments = web.findLastRmaComment(16L);
		logger.error("{}", comments);

		comments = local.findLastRmaComment(1L);
		logger.error("{}", comments);
	}
}
