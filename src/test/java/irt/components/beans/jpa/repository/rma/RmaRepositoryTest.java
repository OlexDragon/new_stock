package irt.components.beans.jpa.repository.rma;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import irt.components.ComponentsApp;
import irt.components.beans.jpa.rma.Rma;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ComponentsApp.class)
class RmaRepositoryTest {
	Logger logger = LogManager.getLogger();

	@Autowired RmaRepository rmaRepository;

	@Test
	void test() throws InterruptedException {

		final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("'RMA'yyMM");
		final LocalDate currentdate = LocalDate.now();
		final String format = currentdate.format(formatter);

		for(int i=0; i<15; i++) {
			final List<Rma> rmas = rmaRepository.findByRmaNumberStartsWith(format);
			logger.info(rmas);
		}

	}

}
