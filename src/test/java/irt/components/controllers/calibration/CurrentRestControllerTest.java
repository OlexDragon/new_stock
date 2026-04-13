package irt.components.controllers.calibration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.UriComponentsBuilder;

import irt.components.workers.IrtHttpRequest;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
//@AutoConfigureMockMvc
class CurrentRestControllerTest {
	private final static Logger logger = LogManager.getLogger();

	@Autowired private MockMvc mockMvc;

	@Test
	void moduleInfoTest() throws Exception {
		logger.error("\n\nEntry");

		final String response = mockMvc.perform(
				post("/calibration/rest/current/module-info")
				.param("sn", "irt-2001013")
				.param("topId", "251.31"))
//		.andDo(print())
		.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

		logger.error("\n\n{}\n\n", response);
	}

	@Test
	void deviceDebugContentTest() throws Exception {
		logger.error("\n\nEntry");

   		List<NameValuePair> params = new ArrayList<>();
		params.addAll(Arrays.asList(new BasicNameValuePair[]{new BasicNameValuePair("devid", "1"), new BasicNameValuePair("command", "hwinfo")}));
		Optional.ofNullable("100").ifPresent(gi->params.add(new BasicNameValuePair("groupindex", gi.toString())));

		String url = UriComponentsBuilder.newInstance()
				.scheme("http")
				.host("irt-2001013")
				.path("/device_debug_read.cgi")
				.toUriString();

		String response = IrtHttpRequest.postForString(url, params);
		logger.error("\n\n{}\n\n", response);
	}

	@Test
	void deviceDebugGetTest() throws Exception {
		logger.error("\n\n*** Entry ***");

		final String response = mockMvc.perform(
				get("/calibration/rest/device_debug_read")
				.param("sn", "irt-2001013")
				.param("devid", "1")
				.param("groupindex", "100")
				.param("command", "hwinfo"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		logger.error("\n\n*** '{}' ***\n\n", response);
	}
}
