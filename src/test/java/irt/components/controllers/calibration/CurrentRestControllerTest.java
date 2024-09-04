package irt.components.controllers.calibration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class CurrentRestControllerTest {
	private final static Logger logger = LogManager.getLogger();

	@Autowired private MockMvc mockMvc;

	@Test
	void test() throws Exception {
		logger.error("\n\nEntry");

		final String response = mockMvc.perform(
				post("/calibration/rest/current/module-info")
				.param("sn", "irt-2415015")
				.param("topId", "251.31"))
//		.andDo(print())
		.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		logger.error("\n\n{}\n\n", response);
	}

}
