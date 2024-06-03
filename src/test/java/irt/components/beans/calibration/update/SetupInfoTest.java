package irt.components.beans.calibration.update;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import irt.components.beans.DateContainer;
import irt.components.beans.irt.update.SetupInfo;
import irt.components.services.SerialNumberScaner;

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

	@Test
	public void testInetAddress() throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL("http://chamber-tps:8088/ping").openConnection();
		connection.setRequestMethod("POST");
		connection.setReadTimeout(100);
		int responseCode = connection.getResponseCode();
		assertEquals(responseCode, 200);
	}

	@Test
	void accumulatedDaysTest(){

		final DateContainer dateContainer = new DateContainer() {
			
			@Override
			public Date getDate() {
				Calendar cal = Calendar.getInstance();
				cal.set(Calendar.YEAR, 2023);
				cal.set(Calendar.MONTH, Calendar.SEPTEMBER);
				cal.set(Calendar.DAY_OF_MONTH, 5);
				return cal.getTime();
			}
		};
		final long accumulatedDays = SerialNumberScaner.accumulatedDays(dateContainer);
		logger.error(accumulatedDays);
	}
}
