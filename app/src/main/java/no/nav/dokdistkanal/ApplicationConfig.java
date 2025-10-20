package no.nav.dokdistkanal;

import no.nav.dokdistkanal.azure.AzureProperties;
import no.nav.dokdistkanal.certificate.KeyStoreProperties;
import no.nav.dokdistkanal.config.RestWebMvcConfig;
import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.config.properties.MaskinportenProperties;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.JdkClientHttpRequestFactory;

import java.time.Duration;

@EnableConfigurationProperties({
		AzureProperties.class,
		DokdistkanalProperties.class,
		MaskinportenProperties.class,
		KeyStoreProperties.class
})
@Import(RestWebMvcConfig.class)
@Configuration
public class ApplicationConfig {

	@Bean
	HttpClient httpClient(HttpClientConnectionManager connectionManager) {
		return HttpClientBuilder.create()
				.useSystemProperties()
				.setConnectionManager(connectionManager)
				.build();
	}

	@Bean
	JdkClientHttpRequestFactory jdkClientHttpRequestFactory() {
		return ClientHttpRequestFactoryBuilder.jdk()
				.withCustomizer(jdkClientHttpRequestFactory ->
						jdkClientHttpRequestFactory.setReadTimeout(Duration.ofSeconds(20)))
				.build();
	}

	@Bean
	HttpClientConnectionManager httpClientConnectionManager() {
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		var readTimeout = SocketConfig.custom().setSoTimeout(Timeout.ofSeconds(20)).build();
		connectionManager.setMaxTotal(400);
		connectionManager.setDefaultMaxPerRoute(100);
		connectionManager.setDefaultSocketConfig(readTimeout);
		return connectionManager;
	}
}
