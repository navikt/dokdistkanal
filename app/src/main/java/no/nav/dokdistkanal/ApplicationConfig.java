package no.nav.dokdistkanal;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import no.nav.dokdistkanal.azure.AzureProperties;
import no.nav.dokdistkanal.config.fasit.DokumenttypeInfoV4Alias;
import no.nav.dokdistkanal.config.fasit.ServiceuserAlias;
import no.nav.dokdistkanal.config.fasit.SikkerhetsnivaaV1Alias;
import no.nav.dokdistkanal.consumer.dki.DigitalKontaktinformasjonConsumer;
import no.nav.dokdistkanal.consumer.dokkat.DokumentTypeInfoConsumer;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.SikkerhetsnivaaConsumer;
import no.nav.dokdistkanal.metrics.DokTimedAspect;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;


@EnableConfigurationProperties({
		DokumenttypeInfoV4Alias.class,
		ServiceuserAlias.class,
		SikkerhetsnivaaV1Alias.class,
		AzureProperties.class
})

@Import({
		DigitalKontaktinformasjonConsumer.class,
		SikkerhetsnivaaConsumer.class,
		DokumentTypeInfoConsumer.class,
})
@Configuration
@EnableAspectJAutoProxy
public class ApplicationConfig {

	@Bean
	public DokTimedAspect timedAspect(MeterRegistry meterRegistry) {
		return new DokTimedAspect(meterRegistry);
	}

	@Bean
	JvmThreadMetrics threadMetrics() {
		return new JvmThreadMetrics();
	}

	@Bean
	HttpClient httpClient(HttpClientConnectionManager connectionManager) {
		return HttpClients.custom()
				.setConnectionManager(connectionManager)
				.build();
	}

	@Bean
	HttpClientConnectionManager httpClientConnectionManager() {
		PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
		connectionManager.setMaxTotal(400);
		connectionManager.setDefaultMaxPerRoute(100);
		return connectionManager;
	}
}
