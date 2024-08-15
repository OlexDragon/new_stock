package irt.components.beans.irt;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class CalibrationRwInfoTest {
	private final Logger logger = LogManager.getLogger();

	@Test
	void test() throws JsonMappingException, JsonProcessingException {

		String json = "{\"linearizer\":[{},{},{},{},{}],\"dp\":{\"enable\":1,\"list\":[{\"name\":\"FCM\",\"index\":\"1001\",\"save_enable\":1,\"vars\":[{},{}]},{\"name\":\"HPBM1\",\"index\":\"1002\",\"save_enable\":1,\"vars\":[{\"index\":0,\"name\":\"G0\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":1,\"name\":\"G1\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":2,\"name\":\"G2\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":3,\"name\":\"G3\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":4,\"name\":\"G4\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":5,\"name\":\"G5\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":6,\"name\":\"G6\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":7,\"name\":\"G7\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":8,\"name\":\"G8\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":9,\"name\":\"G9\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":10,\"name\":\"G10\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":11,\"name\":\"G11\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":12,\"name\":\"G12\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":13,\"name\":\"G13\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":14,\"name\":\"G14\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":15,\"name\":\"G15\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{}]},{\"name\":\"HPBM2\",\"index\":\"1003\",\"save_enable\":1,\"vars\":[{\"index\":0,\"name\":\"G0\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":1,\"name\":\"G1\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":2,\"name\":\"G2\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":3,\"name\":\"G3\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":4,\"name\":\"G4\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":5,\"name\":\"G5\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":6,\"name\":\"G6\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":7,\"name\":\"G7\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":8,\"name\":\"G8\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":9,\"name\":\"G9\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":10,\"name\":\"G10\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":11,\"name\":\"G11\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":12,\"name\":\"G12\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":13,\"name\":\"G13\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":14,\"name\":\"G14\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{\"index\":15,\"name\":\"G15\",\"value\":2047,\"range\":{\"min\":0,\"max\":4095}},{}]},{}]},\"fan\":[{}],\"fcm_dacs\":{\"enable\":1,\"list\":[{\"index\":1,\"name\":\"DAC1\",\"value\":0},{\"index\":2,\"name\":\"DAC2\",\"value\":0},{\"index\":3,\"name\":\"DAC3\",\"value\":2047},{\"index\":4,\"name\":\"DAC4\",\"value\":2047},{}]},\"mute_pulse\":{\"status\":0,\"option\":[{\"name\":\"Disable\",\"value\":0},{\"name\":\"Enable\",\"value\":1}],\"duty_cycle\":{\"on\":2,\"off\":2}},\"last\":1}";
		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		 final CalibrationRwInfo info = mapper.readValue(json, CalibrationRwInfo.class);
		 logger.error(info);
		 final DigitalPotentiometers potentiometers = info.getDigitalPotentiometers();
		 assertTrue(potentiometers.getCalMode());
		 final List<UnitModule> list = potentiometers.getList();
		 assertFalse(list.isEmpty());
		 list.forEach(
				 module->{
					 logger.error(module);

					 if(module.getName()==null || !module.getName().equals("HPBM1"))
						 return;
					 assertEquals("HPBM1", module.getName());
					 assertEquals(1002, module.getIndex());
					 assertTrue(module.getSaveEnable());
					 
					 final List<DigitalPotentiometer> vars = module.getVars();
					 assertFalse(vars.isEmpty());
					 vars.forEach(
							 potentiometer->{
								 logger.error(potentiometer);
								 if(potentiometer.getIndex()==null || potentiometer.getIndex()!=0)
									 return;
								 assertEquals(0, potentiometer.getIndex());
								 assertEquals("G0", potentiometer.getName());
								 assertEquals(2047, potentiometer.getValue());
								 final Range range = potentiometer.getRange();
								 assertEquals(0, range.getMin());
								 assertEquals(4095, range.getMax());
							 });
				 });
	}

}
