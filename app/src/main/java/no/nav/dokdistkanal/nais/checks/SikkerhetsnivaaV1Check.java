package no.nav.dokdistkanal.nais.checks;

import no.nav.dokdistkanal.nais.selftest.AbstractDependencyCheck;
import no.nav.dokdistkanal.nais.selftest.ApplicationNotReadyException;
import no.nav.dokdistkanal.nais.selftest.DependencyType;
import no.nav.dokdistkanal.nais.selftest.Importance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import org.springframework.beans.factory.annotation.Autowired;

@Component
public class SikkerhetsnivaaV1Check extends AbstractDependencyCheck {

	private final RestTemplate restTemplate;

	private static final String SIKKERHETSNIVAAV1 = "SikkerhetsnivaaV1";

	@Autowired
	public SikkerhetsnivaaV1Check(RestTemplateBuilder restTemplateBuilder,
								  @Value("${hentpaaloggingsnivaa-v1.url}") String sikkerhetsnivaaUrl
	) {
		super(DependencyType.REST,
				SIKKERHETSNIVAAV1,
				sikkerhetsnivaaUrl,
				Importance.WARNING);
		this.restTemplate = restTemplateBuilder
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
