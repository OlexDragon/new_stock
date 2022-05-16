package irt.components.workers;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import irt.components.beans.irt.Info;
import irt.components.beans.irt.calibration.NameIndexPair;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

public class HttpRequestTest {
	private final static Logger logger = LogManager.getLogger();

	@Test
	public void updateTest() {

		try {

			URL url = new URL("http", "OP-2123100", "/update.cgi");

			List<NameValuePair> params = new ArrayList<>();
			params.add(new BasicNameValuePair("exec", "debug_devices"));

			final NameIndexPair[] postForObgect = HttpRequest.postForIrtObgect(url.toString(), NameIndexPair[].class,  params).get(10, TimeUnit.SECONDS);
			logger.info("{} : {}", postForObgect.length, (Object[])postForObgect);

		} catch (MalformedURLException | InterruptedException | ExecutionException | TimeoutException e) {
			logger.catching(e);
		}
	}

	@Test
	public void deviceDebugReadTest() {

		try {
			URL url = new URL("http", "OP-2123100", "/device_debug_read.cgi");

			List<NameValuePair> params = new ArrayList<>();
			params.add(new BasicNameValuePair("devid", "1"));
			params.add(new BasicNameValuePair("command", "info"));

			final Info postForObgect = HttpRequest.postForIrtObgect(url.toString(), Info.class,  params).get(10, TimeUnit.SECONDS);
			logger.info(postForObgect);

		} catch (MalformedURLException | InterruptedException | ExecutionException | TimeoutException e) {
			logger.catching(e);
		}
	}

	@Test
	public void dacTest() throws ScriptException, JsonProcessingException {

		try {

			final URL url = new URL("http", "op-2123100", "/calibration.cgi");
			List<NameValuePair> params = new ArrayList<>();
			params.addAll(Arrays.asList(new BasicNameValuePair[]{new BasicNameValuePair("channel", "fcm_dac"), new BasicNameValuePair("index", "2"), new BasicNameValuePair("value", Integer.toString(1111))}));

			Object o = HttpRequest.postForIrtObgect(url.toString(), Object.class, params).get(5, TimeUnit.SECONDS);
			logger.info(o);

		} catch (MalformedURLException | InterruptedException | ExecutionException | TimeoutException e) {
			logger.catching(e);
		}
	}

	@Test
	public void test2() throws ScriptException, JsonProcessingException {
		final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
		ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("JavaScript");
		scriptEngine.eval("count = ['one', 'two', 'three'];");
		scriptEngine.eval("className = count.constructor.name;");

		final String className =  (String) scriptEngine.get("className");

		switch(className) {

		case "Array":
		    scriptEngine.eval("json= JSON.stringify(count);");
		    final String json =  (String) scriptEngine.get("json");

		    final ObjectMapper mapper = new ObjectMapper();
		    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

		    final String[] readValue = mapper.readValue(json, String[].class);
			logger.error("{} : {}", readValue.length, (Object[])readValue);
		    break;
		}

	}

	@Test
	public void test3() throws ScriptException, JsonMappingException, JsonProcessingException {

		final String js = "[{name:'FieldName', numbers: [1,2,3], strings: ['one','two','three'], innerClass:[{name:'InnerClass1', numbers: [4,5,6]},{name:'InnerClass2', strings: ['test']}]},  {strings: ['asdasda','tsdsdf']}]";
		final ClassToGet[] object = toObject(js, ClassToGet[].class);

		logger.error("{}", (Object[])object);
	}

	public <T> T toObject(String js, Class<T> classToReturn) throws ScriptException, JsonMappingException, JsonProcessingException {

		final ScriptEngineManager scriptEngineManager = new ScriptEngineManager();
		ScriptEngine scriptEngine = scriptEngineManager.getEngineByName("JavaScript");
		scriptEngine.eval("variable = " + js);
	    scriptEngine.eval("json= JSON.stringify(variable);");
	    final String json =  (String) scriptEngine.get("json");

	    final ObjectMapper mapper = new ObjectMapper();
	    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

	    return mapper.readValue(json, classToReturn);
	}

	@Getter @Setter @ToString
	public static class ClassToGet{
		private String name;
		private int[] numbers;
		private List<String> strings;
		private Set<ClassToGet> innerClass;
	}
}
