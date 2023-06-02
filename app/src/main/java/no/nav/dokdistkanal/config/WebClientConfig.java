package no.nav.dokdistkanal.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

	@Bean
	public WebClient webClient(WebClient.Builder webClientBuilder) {
		HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofSeconds(60))
				.proxyWithSystemProperties();
		return webClientBuilder.clone()
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.build();
	}
}
