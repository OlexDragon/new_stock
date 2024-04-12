package irt.components.beans.jpa.repository.rma;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import irt.components.beans.jpa.rma.Rma;
import irt.components.beans.jpa.rma.Rma.Status;

@SpringBootTest
class RmaRepositoryTest2 {

	@Autowired private RmaRepository rmaRepository;

	@Test
	void test() {
		final String sn = "IRT-2122005";
		final List<Rma> s = rmaRepository.findBySerialNumberAndStatusIn(sn, Status.SHIPPED);
		assertFalse(s.isEmpty());

		assertFalse(rmaRepository.existsBySerialNumberAndStatusNotIn(sn, Status.SHIPPED, Status.CLOSED));
	}

}
