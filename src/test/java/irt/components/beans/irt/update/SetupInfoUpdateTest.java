package irt.components.beans.irt.update;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SetupInfoUpdateTest {

	@Test
	void test() {
		final SetupInfoUpdate setupInfo = new SetupInfoUpdate("IRT-2424024", "package.bin");
		assertEquals("package any.any.any.IRT-2424024 { upgrade  { path { package.bin }}}", setupInfo.toString());
	}

}
