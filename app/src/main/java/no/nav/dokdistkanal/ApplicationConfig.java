package no.nav.dokdistkanal;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import no.nav.dokdistkanal.azure.AzureProperties;
import no.nav.dokdistkanal.config.fasit.ServiceuserAlias;
import no.nav.dokdistkanal.config.fasit.SikkerhetsnivaaV1Alias;
import no.nav.dokdistkanal.metrics.DokTimedAspect;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.util.Timeout;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableConfigurationProperties({
		ServiceuserAlias.class,
		SikkerhetsnivaaV1Alias.class,
		AzureProperties.class,
		DokdistkanalProperties.class
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

		var readTimeout = SocketConfig.custom().setSoTimeout(Timeout.ofSeconds(20)).build();
		connectionManager.setMaxTotal(400);
		connectionManager.setDefaultMaxPerRoute(100);
		connectionManager.setDefaultSocketConfig(readTimeout);
		return connectionManager;
	}
}
