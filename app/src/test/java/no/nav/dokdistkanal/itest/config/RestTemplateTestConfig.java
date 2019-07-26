package no.nav.dokdistkanal.itest.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * @author Ugur Alpay Cenar, Visma Consulting.
 */
@Configuration
@Profile("itest")
public class RestTemplateTestConfig {
	
	public static final int TIMEOUT = 30_000;
	
	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder
				.requestFactory(new HttpComponentsClientHttpRequestFactory())
				.setReadTimeout(TIMEOUT)
				.setConnectTimeout(TIMEOUT).build();
	}
	
	@Bean
	public RestTemplate restTemplateNoHeader(RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder
				.requestFactory(new HttpComponentsClientHttpRequestFactory())
				.setReadTimeout(TIMEOUT)
				.setConnectTimeout(TIMEOUT).build();
	}
}
