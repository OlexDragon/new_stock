package irt.components.controllers.calibration;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import irt.components.beans.OneCeHeader;
import irt.components.beans.OneCeUrl;
import irt.components.beans.irt.calibration.OneCeSection;
import irt.components.workers.HttpRequest;

@RestController
@RequestMapping("calibration/rest/one-c")
public class OneCeRestController {
	private final static Logger logger = LogManager.getLogger();

	@Autowired private OneCeUrl oneCeApiUrl;

	@GetMapping("profile")
	String getProfileSection(@RequestParam String sn, String section) throws InterruptedException, ExecutionException, TimeoutException {
		logger.traceEntry("sn: {}; section: {}",sn, section);

		final String url = getOneCeUrl(oneCeApiUrl, sn, Optional.ofNullable(section).orElse("header"));
		logger.debug(url);
		return HttpRequest.getForString(url);
	}

	@PostMapping("profile")
	String postProfileSection(@RequestBody OneCeSection oneCeSection) throws InterruptedException, ExecutionException, TimeoutException, UnsupportedEncodingException {
		logger.error("{}",oneCeSection);

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
		final String url = getOneCeUrl(oneCeApiUrl, oneCeSection.getSerialNumber(), oneCeSection.getSection()) + extra;
		logger.debug(url);

		String json = String.format("{ \"%s\": \"%s\"}", oneCeSection.getFieldName(), oneCeSection.getValue());
		logger.debug(json);
		return HttpRequest.postString(url, json).get(5, TimeUnit.SECONDS);
	}

	public static FutureTask<OneCeHeader> getOneCHeader(OneCeUrl oneCeApiUrl, String sn) {
		final String section = "header";
		String url = getOneCeUrl(oneCeApiUrl, sn, section);
		logger.debug(url);
		return  HttpRequest.getForObgect(url, OneCeHeader.class);
	}

	public static String getOneCeUrl(OneCeUrl oneCeApiUrl, String sn, final String section) {
		final List<NameValuePair> params = new ArrayList<>();
		params.add(new BasicNameValuePair("sn", sn.replaceAll("\\D", "")));
		params.add(new BasicNameValuePair("section", section));
		String url = oneCeApiUrl.createUrl("travelers", params);
		return url;
	}
}
