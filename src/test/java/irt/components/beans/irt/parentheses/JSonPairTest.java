package irt.components.beans.irt.parentheses;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class JSonPairTest {

	@Test
	void test() {
		final JSonPair jSonPair = new JSonPair(" name : value ");
		assertEquals("\"name\":\"value\"", jSonPair.toString());
	}

	@Test
	void test2() {
		final JSonPair jSonPair = new JSonPair(" name : 1 2 ");
		assertEquals("\"name\":\"1 2\"", jSonPair.toString());
	}

	@Test
	void test3() {
		final JSonPair jSonPair = new JSonPair(" name : 123 ");
		assertEquals("\"name\":123", jSonPair.toString());
	}
}
