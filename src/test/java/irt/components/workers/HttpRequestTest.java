package irt.components.workers;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Stream;

import javax.script.ScriptException;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.JsonProcessingException;

import irt.components.beans.OneCeUrl;
import irt.components.beans.irt.Info;
import irt.components.beans.irt.calibration.NameIndexPair;
import irt.components.controllers.calibration.CalibrationController;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

public class HttpRequestTest {
	private final static Logger logger = LogManager.getLogger();

	OneCeUrl oneCeUrl;
	OneCeUrl oneCeApiUrl;

	@Before
	public void before() throws IOException {
		try(final InputStream is = getClass().getClassLoader().getResourceAsStream("application-w.properties");){
			Optional.ofNullable(is)
			.ifPresent(
					s->{
						try {

							Properties properties = new Properties();
							properties.load(s);
							String scheme = properties.getProperty("irt.url.scheme");
							String userInfo = properties.getProperty("irt.url.userInfo");
							String url = properties.getProperty("irt.url");
							String urlApi = properties.getProperty("irt.url.api");

							oneCeUrl = new OneCeUrl(scheme, userInfo, url);
							oneCeApiUrl = new OneCeUrl(scheme, userInfo, urlApi);

						} catch (IOException e) {
							logger.catching(e);
						}
					});
		}
	}

	@Test
	public void javaScriptToJSonTest() throws JsonProcessingException, ScriptException{
		final String jSon = IrtHttpRequest.javaScriptToJSon("var test = {test1:1, test2:2, test3: 'test3'}");
		logger.error(jSon);
	}

	@Test
	public void postForIrtObgectTest() throws InterruptedException, ExecutionException, TimeoutException {

			String url = UriComponentsBuilder.newInstance()
					.scheme("http")
					.host("IRT-BUC-EMU3")
					.path("/update.cgi")
					.toUriString();

			List<NameValuePair> params = new ArrayList<>();
			params.add(new BasicNameValuePair("exec", "debug_devices"));

			final NameIndexPair[] postForObgect = IrtHttpRequest.postForIrtObgect(url, NameIndexPair[].class,  params).get(10, TimeUnit.SECONDS);
			logger.info("{} : {}", postForObgect.length, (Object[])postForObgect);
	}

	@Test
	public void deviceDebugReadTest() throws InterruptedException, ExecutionException, TimeoutException {

			String url = UriComponentsBuilder.newInstance()
					.scheme("http")
					.host("IRT-BUC-EMU3")
					.path("/device_debug_read.cgi")
					.toUriString();

			List<NameValuePair> params = new ArrayList<>();
			params.add(new BasicNameValuePair("devid", "1"));
			params.add(new BasicNameValuePair("command", "info"));

			final Info postForObgect = IrtHttpRequest.postForIrtObgect(url, Info.class,  params).get(10, TimeUnit.SECONDS);
			logger.info(postForObgect);
	}

	@Test
	public void dacTest() throws ScriptException, JsonProcessingException, InterruptedException, ExecutionException, TimeoutException {

			String url = UriComponentsBuilder.newInstance()
					.scheme("http")
					.host("IRT-BUC-EMU3")
					.path("/calibration.cgi")
					.toUriString();

			List<NameValuePair> params = new ArrayList<>();
			params.addAll(Arrays.asList(new BasicNameValuePair[]{new BasicNameValuePair("channel", "fcm_dac"), new BasicNameValuePair("index", "2"), new BasicNameValuePair("value", Integer.toString(1111))}));

			Object o = IrtHttpRequest.postForIrtObgect(url, Object.class, params).get(5, TimeUnit.SECONDS);
			logger.info(o);
	}

	@Test
	public void systemConfiguration() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException{

		final String serialNumber = "IRT-BUC-EMU3";
		final NameIndexPair[] allIndex = CalibrationController.getAllIndex(serialNumber);
		logger.error("{} : {}", allIndex.length, allIndex);

		Optional.ofNullable(allIndex).map(Arrays::stream).orElse(Stream.empty())
		.filter(
				p->{
					final String name = p.getName();
					return name!=null && name.equals("System");
				})
		.findAny().map(NameIndexPair::getIndex)
		.ifPresent(
				i->{
					try {

						final Object string = CalibrationController.getHttpDeviceDebug(serialNumber, Object.class, new BasicNameValuePair("devid", i.toString()), new BasicNameValuePair("command", "config")).get(5, TimeUnit.SECONDS);
						logger.error(string);

					} catch (MalformedURLException | InterruptedException | ExecutionException | TimeoutException e) {
						logger.catching(e);
					}
					logger.error(i);
				});
	}

	@Test
	public void postForSystemConfigTest() throws IOException{
		IrtHttpRequest.postForSystemConfig("IRT-BUC-EMU3");
	}

	@Test
	public void getAllModulesTest() throws IOException, InterruptedException, ExecutionException, TimeoutException, ScriptException {
		final Map<String, Integer> allModules = IrtHttpRequest.getAllModules("IRT-BUC-EMU3");
		logger.info(allModules);
	}

	@Test
	public void getForStringFTTest() throws MalformedURLException, InterruptedException, ExecutionException, TimeoutException {

		final List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("sn", "IRT-2518040".replaceAll("\\D", "")));
		params.add(new BasicNameValuePair("section", "header"));
		URI uri = oneCeApiUrl.createUrl("travelers", params);
		final FutureTask<String> forStringFT = IrtHttpRequest.getForStringFT(uri);
		logger.info(forStringFT.get(1000, TimeUnit.MILLISECONDS));
	}

	@Test
	void deviceDebugWriteTest() throws IOException{
		List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("devid", "1"));
		params.add(new BasicNameValuePair("command", "info"));
		final String postForString = IrtHttpRequest.postForString("http://N2515003/device_debug_read.cgi", params);
		logger.info(postForString);
	}

	@Getter @Setter @ToString
	public static class ClassToGet{
		private String name;
		private int[] numbers;
		private List<String> strings;
		private Set<ClassToGet> innerClass;
	}
}
