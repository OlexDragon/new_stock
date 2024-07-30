package irt.components.beans.irt;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class UnitModuleTest {

	@Test
	void test() throws JsonMappingException, JsonProcessingException {

		String json = "{\"name\":\"HPBM1\",\"index\":\"1002\",\"save_enable\":1,\"vars\":[{\"index\":0,\"name\":\"G0\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}}]}";
		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		 final UnitModule module = mapper.readValue(json, UnitModule.class);
		 assertEquals("HPBM1", module.getName());
		 assertEquals(1002, module.getIndex());
		 assertEquals(1, module.getSaveEnable());
		 
		 final List<DigitalPotentiometer> vars = module.getVars();
		 assertFalse(vars.isEmpty());
		 vars.forEach(
				 potentiometer->{
					 assertEquals(0, potentiometer.getIndex());
					 assertEquals("G0", potentiometer.getName());
					 assertEquals(2047, potentiometer.getValue());
					 final Range range = potentiometer.getRange();
					 assertEquals(0, range.getMin());
					 assertEquals(4095, range.getMax());
				 });
	}

}
