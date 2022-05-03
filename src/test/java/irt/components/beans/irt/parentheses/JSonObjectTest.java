package irt.components.beans.irt.parentheses;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class JSonObjectTest {

	@Test
	void test() {

		assertEquals("{}", new JSonObject("").toString());

		final JSonObject jSonObject = new JSonObject("a: 1, b : m");

		assertEquals("{\"a\":1,\"b\":\"m\"}", jSonObject.toString());
	}

	@Test
	void test2() {

		final JSonObject jSonObject = new JSonObject("name", "a: 1, b : m");

		assertEquals("{\"name\":{\"a\":1,\"b\":\"m\"}}", jSonObject.toString());
		assertEquals("\"name\":{\"a\":1,\"b\":\"m\"}", jSonObject.toJSonPair().toString());
	}
}
