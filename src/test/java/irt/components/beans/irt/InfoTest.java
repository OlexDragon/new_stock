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

}
