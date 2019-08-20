package no.nav.dokdistkanal.nais.checks;

import static no.nav.dokdistkanal.metrics.PrometheusLabels.SIKKERHETSNIVAAV1;

import no.nav.dokdistkanal.nais.selftest.AbstractDependencyCheck;
import no.nav.dokdistkanal.nais.selftest.ApplicationNotReadyException;
import no.nav.dokdistkanal.nais.selftest.DependencyType;
import no.nav.dokdistkanal.nais.selftest.Importance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;

@Component
public class SikkerhetsnivaaV1Check extends AbstractDependencyCheck {

	private final RestTemplate restTemplate;

	@Inject
	public SikkerhetsnivaaV1Check(RestTemplateBuilder restTemplateBuilder,
								  @Value("${hentpaaloggingsnivaa-v1.url}") String sikkerhetsnivaaUrl,
								  HttpComponentsClientHttpRequestFactory requestFactory) {
		super(DependencyType.REST,
				SIKKERHETSNIVAAV1,
				sikkerhetsnivaaUrl,
				Importance.WARNING);
		this.restTemplate = restTemplateBuilder.requestFactory(() -> requestFactory)
				.rootUri(sikkerhetsnivaaUrl)
				.build();
	}

	@Override
	protected void doCheck() {
		try {
			restTemplate.getForEntity("/isReady", String.class);
		} catch (Exception e) {
			throw new ApplicationNotReadyException(String.format("%s ping failed. errorMessage=%s", SIKKERHETSNIVAAV1, getErrorMessage(e)), e);
		}
	}

}
