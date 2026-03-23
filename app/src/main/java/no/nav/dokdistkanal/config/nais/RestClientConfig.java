package no.nav.dokdistkanal.config.nais;

import no.nav.dokdistkanal.consumer.nais.NaisTexasConsumer;
import no.nav.dokdistkanal.consumer.nais.NaisTexasRequestInterceptor;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

	@Bean
	RestClient restClientTexas(RestClient.Builder restClientBuilder, NaisTexasConsumer naisTexasConsumer) {
		return restClientBuilder
				.requestFactory(jdkClientHttpRequestFactory())
				.requestInterceptor(new NaisTexasRequestInterceptor(naisTexasConsumer))
				.build();
	}

	private static JdkClientHttpRequestFactory jdkClientHttpRequestFactory() {
		return ClientHttpRequestFactoryBuilder.jdk()
				.withCustomizer(factory -> factory.setReadTimeout(Duration.ofSeconds(20)))
				.build();
	}
}
