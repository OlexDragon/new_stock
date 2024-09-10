package irt.components.services.converter;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import irt.components.beans.CurrentAlias;

class ListConverterTest {
	private final static Logger logger = LogManager.getLogger();

	@Test
	void test() {
		final CurrentAliastListConverter listConverter = new CurrentAliastListConverter();
		final String column = listConverter.convertToDatabaseColumn(new ArrayList<>());
		logger.error(column);
		assertEquals("[ ]", column);
		final List<CurrentAlias> entity = listConverter.convertToEntityAttribute("[]");
		logger.error(entity);
		assertTrue(entity.isEmpty());
	}

}
