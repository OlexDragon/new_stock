package irt.components.beans.irt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class InfoTest {

	@Test
	void test() {
		final Info info = new Info();
		info.setDeviceId("{250.31.1}");
		assertEquals(250, info.getDeviceType());
		assertEquals(31, info.getTypeVersion());
	}
	@Test
	void test2() {
		String str = "-25  -24,/ -15, -21/25";
		final String[] split = str.split("[ ,/]+");
		assertEquals(5, split.length);
	}

}
