package no.nav.dokdistkanal.nais.checks;

import static no.nav.dokdistkanal.metrics.PrometheusLabels.SIKKERHETSNIVAAV1;

import no.nav.dokdistkanal.nais.selftest.support.AbstractSelftest;
import no.nav.dokdistkanal.nais.selftest.support.ApplicationNotReadyException;
import no.nav.dokdistkanal.nais.selftest.support.Ping;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;

@Component
public class SikkerhetsnivaaV1Check extends AbstractSelftest {

	private final String url;
	private final RestTemplate restTemplate;

	@Inject
	public SikkerhetsnivaaV1Check(RestTemplateBuilder restTemplateBuilder,
								  @Value("${HENTPAALOGGINGSNIVAA_V1_URL}") String sikkerhetsnivaaUrl,
								  HttpComponentsClientHttpRequestFactory requestFactory) {
		super(Ping.Type.Rest,
				SIKKERHETSNIVAAV1,
				sikkerhetsnivaaUrl,
				SIKKERHETSNIVAAV1);
		this.url = sikkerhetsnivaaUrl;
		this.restTemplate = restTemplateBuilder.requestFactory(requestFactory)
				.rootUri(sikkerhetsnivaaUrl)
				.build();
	}

	@Override
	protected void doCheck() {
		try {
			restTemplate.getForEntity("/isReady", String.class);
		} catch (Exception e) {
			throw new ApplicationNotReadyException("Could not ping "+ SIKKERHETSNIVAAV1, e);
		}
	}

}
