package irt.components.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import irt.components.beans.OneCeUrl;

@Configuration
public class WebConfig {

	@Value("${irt.url.protocol}")
	private String protocol;

	@Value("${irt.url.login}")
	private String login;

	@Value("${irt.url}")
	private String url;


    @Bean
    public OneCeUrl oneCeUrl() throws Exception {

		return new OneCeUrl(protocol, login, url);
	}
}
