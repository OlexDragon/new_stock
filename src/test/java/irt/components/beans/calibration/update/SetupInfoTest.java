package irt.components.beans.calibration.update;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import irt.components.beans.irt.update.SetupInfo;

public class SetupInfoTest {
	private final static Logger logger = LogManager.getLogger();

	@Test
	public void test() {

		final SetupInfo setupInfo = new SetupInfo("irt-2201001");
		assertEquals("system any.any.any.irt-2201001 { profile { path { irt-2201001.bin }}}", setupInfo.toString());

		setupInfo.setModule(true);
		assertEquals("file any.any.any.irt-2201001 { profile { path { irt-2201001.bin }}}", setupInfo.toString());

//		final Md5 md5 = new Md5("system any.any.any.OP-2123100 {profile { path {OP-2123100.bin}  }}".getBytes());
//		assertEquals("3DE61663E89BB71F08C02217F7932BF4", md5.toString());
	}

	@Test
	public void testIp() throws UnknownHostException {
		final byte[] address = InetAddress.getLocalHost().getAddress();
		logger.error("{}", address);
	}
}
