package irt.components.beans;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor @ToString
public class OneCeUrl {

	private static final int SIZE = 5;

	private final String protocol;
	private final String login;
	private final String url;

	public String createUrl(String category) {

		List<NameValuePair> params = new ArrayList<NameValuePair>();
//		params.add(new BasicNameValuePair("$format", "json"));
		params.add(new BasicNameValuePair("$top", "" + SIZE));

		return createUrl(category, params);
	}

	public String createUrl(String category, List<NameValuePair> params) {
		final StringBuilder sb = new StringBuilder(protocol).append(login).append(url).append(category).append('?');

		 final String collectPatams = params.stream().map(pair->pair.getName() + "=" + pair.getValue()).collect(Collectors.joining("&"));
		 sb.append(collectPatams);

		 return sb.toString();
	}
}
