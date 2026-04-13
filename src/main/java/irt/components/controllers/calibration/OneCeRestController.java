package irt.components.controllers.calibration;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import irt.components.beans.OneCeHeader;
import irt.components.beans.OneCeUrl;
import irt.components.beans.irt.calibration.OneCeSection;
import irt.components.beans.irt.calibration.OneCeSectionTosave;
import irt.components.workers.IrtHttpRequest;

@RestController
@RequestMapping("calibration/rest/one-c")
public class OneCeRestController {
	private final static Logger logger = LogManager.getLogger();


	@Autowired private OneCeUrl oneCeApiUrl;

	@Value("${irt.url.scheme}")
	private String scheme;

	@Value("${irt.host}")
	private String host;

	@Value("${irt.url.travelers}")
	private String travelers;

	@Value("${irt.url.username}")
	private String username;

	@Value("${irt.url.password}")
	private String password;

	@GetMapping("profile")
	String getProfileSection(@RequestParam String sn, @RequestParam String section, Boolean byProfile){
		logger.traceEntry("sn: {}; section: {}; byProfile: {}",sn, section, byProfile);

		try {

			final URI uri = getOneCeUrl(oneCeApiUrl, sn, Optional.ofNullable(section).orElse("header"), byProfile);
			logger.debug(uri);
			return IrtHttpRequest.getForString(uri, 5000, TimeUnit.MILLISECONDS);

		} catch (SocketTimeoutException e) {
			logger.error("{} - Request to One-C API timed out; section: {}; byProfile: {};", sn, e.getMessage(), section, byProfile);
			 return "error: Request timed out";
		} catch (IOException e) {
			logger.catching(e);
			 return "error: " + e.getMessage();
		}
	}

	@PostMapping(path="profile/save", produces = "application/json;charset=utf-8")
	String saveProfileSection(@RequestBody OneCeSectionTosave toSave){
		logger.traceEntry("{}",toSave);

		try {
			String encodedAuth = Base64.getEncoder() .encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));

			final URI uri = new URI(scheme, host, travelers, "section=" + toSave.getSection() + "&sn=" + toSave.getSerialNumber(), null);
			logger.debug(uri);
			final HttpResponse<String> response = IrtHttpRequest.postJson(uri, encodedAuth, toSave.getBody());
			logger.debug("Response status code: {}", response.statusCode());
			
			return response.body();

		} catch (IOException | InterruptedException | URISyntaxException e) {
			logger.catching(e);
		}
		return null;
	}

	@PostMapping("profile")
	String postProfileSection(@RequestBody OneCeSection oneCeSection) throws InterruptedException, ExecutionException, TimeoutException, UnsupportedEncodingException, MalformedURLException {
		logger.traceEntry("{}",oneCeSection);

		final String extra = Optional.of(oneCeSection.getSetting())

				.filter(s->!s.isEmpty()).map(setting->{
					try {
						return String.format("&component=%s&setting=%s", oneCeSection.getComponent(), URLEncoder.encode(setting, StandardCharsets.UTF_8.displayName()).replace("+", "%20"));
					} catch (UnsupportedEncodingException e) {
						logger.catching(e);
					}
					return "";
				})
				.orElse("");
		final String url = getOneCeUrl(oneCeApiUrl, oneCeSection.getSerialNumber(), oneCeSection.getSection(), null) + extra;
		logger.debug(url);

		String json = String.format("{ \"%s\": \"%s\"}", oneCeSection.getFieldName(), oneCeSection.getValue());
		logger.debug(json);
		return IrtHttpRequest.postString(url, json).get(5, TimeUnit.SECONDS);
	}

	public static FutureTask<OneCeHeader> getOneCHeader(OneCeUrl oneCeApiUrl, String sn) throws MalformedURLException {
		final String section = "header";
		String url = getOneCeUrl(oneCeApiUrl, sn, section, null).toString();
		logger.debug(url);
		return  IrtHttpRequest.getForObgect(url, OneCeHeader.class);
	}

	public static URI getOneCeUrl(OneCeUrl oneCeApiUrl, String sn, final String section, Boolean byProfile) throws MalformedURLException {

		final List<NameValuePair> params = new ArrayList<>();
		
		final String name = Optional.ofNullable(byProfile).filter(b->b).map(p->"profile").orElse("sn");
		params.add(new BasicNameValuePair(name, sn));
		params.add(new BasicNameValuePair("section", section));
		return oneCeApiUrl.createUrl("travelers", params);
	}
}
