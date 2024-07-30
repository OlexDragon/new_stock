package irt.components.beans.irt;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class RangeTest {

	@Test
	void test() throws JsonMappingException, JsonProcessingException {
		String json = "{ \"min\": 0, \"max\": 4095}";
		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		 final Range range = mapper.readValue(json, Range.class);
		 assertEquals(0, range.getMin());
		 assertEquals(4095, range.getMax());
	}

}
