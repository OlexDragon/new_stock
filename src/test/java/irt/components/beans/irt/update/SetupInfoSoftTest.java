package irt.components.beans.irt.update;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SetupInfoSoftTest {

	@Test
	void test() {

		final SetupInfoSoft si = new SetupInfoSoft(null, "image.bin");
		assertEquals("system any.any.any { image { path { image.bin }}}", si.toString());

		final SetupInfoSoft setupInfoSoft = new SetupInfoSoft("1002.31", "image.bin");
		assertEquals("system 1002.31.any { image { path { image.bin }}}", setupInfoSoft.toString());

		setupInfoSoft.setModule(true);
		assertEquals("package 1002.31.any { upgrade { path { image.bin }}}", setupInfoSoft.toString());
	}

}
