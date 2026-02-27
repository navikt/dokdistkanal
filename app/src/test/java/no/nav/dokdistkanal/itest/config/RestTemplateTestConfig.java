package no.nav.dokdistkanal.itest.config;

import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
@Profile("itest")
public class RestTemplateTestConfig {

	public static final int TIMEOUT = 30_000;

	@Bean
	public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder
				.requestFactory(HttpComponentsClientHttpRequestFactory.class)
				.readTimeout(Duration.ofMillis(TIMEOUT))
				.build();
	}

}
