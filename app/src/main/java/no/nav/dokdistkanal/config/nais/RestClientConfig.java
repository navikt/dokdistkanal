package no.nav.dokdistkanal.config.nais;

import no.nav.dokdistkanal.consumer.nais.NaisTexasConsumer;
import no.nav.dokdistkanal.consumer.nais.NaisTexasRequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

	@Bean
	RestClient restClientTexas(RestClient.Builder restClientBuilder, NaisTexasConsumer naisTexasConsumer) {
		return restClientBuilder
				.requestInterceptor(new NaisTexasRequestInterceptor(naisTexasConsumer))
				.build();
	}
}
