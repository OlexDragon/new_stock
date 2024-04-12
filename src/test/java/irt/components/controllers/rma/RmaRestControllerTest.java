package irt.components.controllers.rma;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class RmaRestControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void test() throws Exception {
		mockMvc.perform(get("/rma/rest/ready-to-add").param("sn", "IRT-1503002")).andDo(print()).andExpect(status().isOk()).andExpect(content().string("true"));
		mockMvc.perform(get("/rma/rest/ready-to-add").param("sn", "IRT-1815013")).andDo(print()).andExpect(status().isOk()).andExpect(content().string("false"));
		mockMvc.perform(get("/rma/rest/ready-to-add").param("sn", "IRT-2122005")).andDo(print()).andExpect(status().isOk()).andExpect(content().string("true"));
	}

}
