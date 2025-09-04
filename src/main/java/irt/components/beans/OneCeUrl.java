package irt.components.beans;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.LogManager;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor @ToString
public class OneCeUrl {

	private static final int SIZE = 5;

	private final String protocol;
	private final String login;
	private final String url;

	public URI createUrl(String category) throws MalformedURLException {

		List<NameValuePair> params = new ArrayList<NameValuePair>();
//		params.add(new BasicNameValuePair("$format", "json"));
		params.add(new BasicNameValuePair("$top", "" + SIZE));

		return createUrl(category, params);
	}

	public URI createUrl(String category, List<NameValuePair> params) throws MalformedURLException {
		LogManager.getLogger().error("login: {}", login);

		final UriComponentsBuilder builder = UriComponentsBuilder.newInstance()
				.scheme(protocol)
				.userInfo(login)
				.host(url)
				.path(category);

		params.forEach(p->builder.queryParam(p.getName(), p.getValue()));

		return builder.build().toUri();
	}
}
