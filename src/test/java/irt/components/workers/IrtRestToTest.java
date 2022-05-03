package irt.components.workers;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import irt.components.beans.irt.BiasBoard;
import irt.components.beans.irt.Info;
import irt.components.beans.irt.parentheses.Parentheses;

class IrtRestToTest {
	Logger logger = LogManager.getLogger();

	@Test
	void textToJSonTest() throws JsonMappingException, JsonProcessingException {

		String s = "Product name: Olex setup\r\n"
				+ "  Serial number: OP-2123100\r\n"
				+ "  Part number: IPA-CB00590-RMS0\r\n"
				+ "\r\n"
				+ "  Device ID: {250.2.2}\r\n"
				+ "  Software version: 2.10.7\r\n"
				+ "  Build: Jan 28 2021, 18:20:19\r\n"
				+ "\r\n"
				+ "  Uptime: 7163547\r\n"
				+ "";

		final String json = IrtRestTo.json(s);
		final String expected = "{\"Product name\":\"Olex setup\",\"Serial number\":\"OP-2123100\",\"Part number\":\"IPA-CB00590-RMS0\",\"Device ID\":\"{250.2.2}\",\"Software version\":\"2.10.7\",\"Build\":\"Jan 28 2021, 18:20:19\",\"Uptime\":\"7163547\"}";
		logger.error("\n{}\n{}\n{}", s, expected, json);

		assertEquals(expected, json);

		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		final Info readValue = mapper.readValue(json, Info.class);
		logger.error("\n\t{}", readValue);
	}

	@Test
	void javaScriptToJSonTest() throws JsonMappingException, JsonProcessingException {

		String s = "calib_ro_info = { bias: { title: 'On-board sensors:', class: 'biasinfo', visible: 1, power: { value: '438', unit: 'mV' }, refl_power: { value: '0', unit: 'mV' }, temperature: '-20.5', last:1}, epsu: {title: 'External PSU monitor:', class: 'psuinfo', visible: 0, data: [{}] }, last: 1 };";

		final String json = IrtRestTo.json(s);
		final String expected = "{\"bias\":{\"title\":\"On-board sensors:\",\"class\":\"biasinfo\",\"visible\":1,\"power\":{\"value\":\"438\",\"unit\":\"mV\" },\"refl_power\":{\"value\":\"0\",\"unit\":\"mV\"},\"temperature\":\"-20.5', last:1}, epsu: {title: 'External PSU monitor:', class: 'psuinfo', visible: 0, data: [{}] }, last: 1 }";
		logger.error("\n{}\n{}\n{}", s, expected, json);

		assertEquals(expected, json);

		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		final BiasBoard readValue = mapper.readValue(json, BiasBoard.class);
		logger.error("\n\t{}", readValue);
	}

	@Test
	void scanTest1() {
		String text = " {test }";

		AtomicInteger index = new AtomicInteger();

		Parentheses parent = new Parentheses(null, null, null);
		IrtRestTo.scan(text, index, parent);
		logger.error("\n{}", parent);

		List<Parentheses> internalParentheses = parent.getInternalParentheses();
		assertEquals(1, internalParentheses.size());

		final Parentheses parentheses = internalParentheses.get(0);
		final Integer startIndex = parentheses.getStartIndex();
		assertNotNull(startIndex);
		assertEquals(1, startIndex);

		final Integer stopIndex = parentheses.getStopIndex();
		assertNotNull(stopIndex);
		assertEquals(7, stopIndex);
	}

	@Test
	void scanTest2() {
		String text = " {test },{}";

		AtomicInteger index = new AtomicInteger();

		Parentheses parent = new Parentheses(null, null, null);
		IrtRestTo.scan(text, index, parent);
		logger.error("\n{}", parent);

		List<Parentheses> internalParentheses = parent.getInternalParentheses();
		assertEquals(2, internalParentheses.size());

		int[] expected = {1,7,9,10};
		extracted(internalParentheses, expected);
	}

	@Test
	void scanTest3() {
		String text = " {test{ test }{ test}, {} },{{}{}}";

		AtomicInteger index = new AtomicInteger();

		Parentheses parent = new Parentheses(null, null, null);
		IrtRestTo.scan(text, index, parent);
		logger.error("\n{}", parent);

		List<Parentheses> internalParentheses = parent.getInternalParentheses();
		assertEquals(2, internalParentheses.size());

		int[] expected = {1,26,28,33};
		extracted(internalParentheses, expected);

		int[][] expected2 = {{6,13,14,20,23,24},{29,30,31,32}};
		int i = 0;
		for(Parentheses parentheses : internalParentheses) {
			extracted(parentheses.getInternalParentheses(), expected2[i++]);
		}
	}

	@Test
	void getParenthesesTest() {
		String text = " [{test{ test }{ test}, {} },{{}[{}]}]";
		final List<Parentheses> parentheses = IrtRestTo.getParentheses(text);
		logger.error("\n{}", parentheses);

		assertEquals(1, parentheses.size());

		final Parentheses parent = parentheses.get(0);
		assertEquals('[', parent.getStartParenthese());
		assertEquals(1, parent.getStartIndex());
		assertEquals(37, parent.getStopIndex());

		List<Parentheses> internalParentheses = parent.getInternalParentheses();
		assertEquals(2, internalParentheses.size());

		int[] expected = {2,27,29,36};
		extracted(internalParentheses, expected);

		int[][] expected2 = {{7,14,15,21,24,25},{30,31,32,35}};
		int i = 0;
		for(Parentheses p : internalParentheses) {
			extracted(p.getInternalParentheses(), expected2[i++]);
		}

		final Parentheses p = internalParentheses.get(1).getInternalParentheses().get(1);
		assertEquals('[', p.getStartParenthese());
		extracted(p.getInternalParentheses(), new int[]{33,34});
	}

	public void extracted(List<Parentheses> internalParentheses, int[] expected) {
		int i = 0;
		for(Parentheses parentheses : internalParentheses) {
			final Integer startIndex = parentheses.getStartIndex();
			assertNotNull(startIndex);
			assertEquals(expected[i++], startIndex);

			final Integer stopIndex = parentheses.getStopIndex();
			assertNotNull(stopIndex);
			assertEquals(expected[i++], stopIndex);
		}
	}
}
