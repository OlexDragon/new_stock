package irt.components.services.converter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

class JSonMapOfMapConverterTest {
	private final static Logger logger = LogManager.getLogger();

	@Test
	void test() {
		final JSonMapOfMapConverter converter = new JSonMapOfMapConverter();
		
		Map<String, Map<String, String>> m = new HashMap<>();
		Map<String, String> inner = new HashMap<>();
		m.put("first", inner);

		inner.put("first.1", "Test1");
		inner.put("first.2", "Test2");
		inner.put("first.3", "Test3");
		inner.put("first.4", "Test4");
		inner.put("first.5", "Test5");
		inner.put("first.6", "Test6");

		final String convertToDatabaseColumn = converter.convertToDatabaseColumn(m);
		logger.error(convertToDatabaseColumn);

		final Map<String, Map<String, String>> convertToEntityAttribute = converter.convertToEntityAttribute(convertToDatabaseColumn);
		assertTrue(m.equals(convertToEntityAttribute));
		assertTrue(m.get("first").equals(convertToEntityAttribute.get("first")));
	}

}
