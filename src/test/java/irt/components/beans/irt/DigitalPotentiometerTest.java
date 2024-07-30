package irt.components.beans.irt;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class DigitalPotentiometerTest {

	@Test
	void test() throws JsonMappingException, JsonProcessingException {

		String json = "{\"index\":0,\"name\":\"G0\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}}";
		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		 final DigitalPotentiometer potentiometer = mapper.readValue(json, DigitalPotentiometer.class);
		 assertEquals(0, potentiometer.getIndex());
		 assertEquals("G0", potentiometer.getName());
		 assertEquals(2047, potentiometer.getValue());
		 final Range range = potentiometer.getRange();
		 assertEquals(0, range.getMin());
		 assertEquals(4095, range.getMax());
	}

}
