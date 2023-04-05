package irt.components.beans.irt;

import java.util.Optional;

import javax.script.ScriptException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import irt.components.workers.HttpRequest;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.ToString;

@Getter @ToString @JsonIgnoreProperties(ignoreUnknown = true)
public class HomePageInfo {
	@Getter(AccessLevel.NONE)
	private final Logger logger = LogManager.getLogger();

	private SysInfo  sysInfo;
	private NetInfo  netInfo;

	public HomePageInfo(String homePage) {

		Optional.ofNullable(homePage)
		.ifPresent(str->{
			try {

				sysInfo = getSysInfo(str);

			} catch (JsonProcessingException | ScriptException e) {
				logger.catching(e);
			}

			try {

				netInfo = getNetInfo(str);

			} catch (JsonProcessingException | ScriptException e) {
				logger.catching(e);
			}
		});
	}

	private SysInfo getSysInfo(String str) throws JsonProcessingException, ScriptException {
		return getObject(str, "sysinfo = {", SysInfo.class);
	}

	private NetInfo getNetInfo(String str) throws JsonProcessingException, ScriptException {
		return getObject(str, "netinfo = {", NetInfo.class);
	}

	private <T> T getObject(String str, final String jsStarts, final Class<T> valueType)
			throws ScriptException, JsonProcessingException, JsonMappingException {
		final int start = str.indexOf(jsStarts);
		if(start<0)
			return null;

		final int stop = str.indexOf("}", start) + 1;
		final String js = str.substring(start, stop);
		final String json = HttpRequest.javaScriptToJSon(js);

		final ObjectMapper mapper = new ObjectMapper();
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
	
		return mapper.readValue(json, valueType);
	}
}
