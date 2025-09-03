package irt.components.services;

import static org.springframework.web.reactive.function.client.ExchangeFilterFunctions.basicAuthentication;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class OneCWebClient {

	@Value("${irt.url.protocol}")
	private String protocol;

	@Value("${irt.url.username}")
	private String username;

	@Value("${irt.url.password}")
	private String password;

	@Value("${irt.url}")
	private String url;

	@Value("${irt.url.api}")
	private String api;

	public WebClient build() {
		return build(url);
	}

	public WebClient buildApi() {
		return build(api);
	}

	private WebClient build(String url) {

		String urlTo1c = new StringBuilder(protocol).append(url).toString();

		return WebClient.builder()
				.baseUrl(urlTo1c)
		        .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
		        .filter(basicAuthentication(username, password))
		        .codecs(conf->conf.defaultCodecs().maxInMemorySize(1000000))
		        .build();
	}
}
