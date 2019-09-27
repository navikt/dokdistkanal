package no.nav.dokdistkanal.consumer.sts;

import static no.nav.dokdistkanal.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistkanal.constants.RetryConstants.MULTIPLIER_SHORT;
import static no.nav.dokdistkanal.nais.NaisContract.STS_CACHE_NAME;

import no.nav.dokdistkanal.config.fasit.ServiceuserAlias;
import no.nav.dokdistkanal.consumer.sts.to.StsResponseTo;
import no.nav.dokdistkanal.exceptions.technical.DokDistKanalTechnicalException;
import no.nav.dokdistkanal.exceptions.technical.StsTechnicalException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.time.Duration;

/**
 * @author Sigurd Midttun, Visma Consulting.
 */
@Component
public class StsRestConsumer {

	private final RestTemplate restTemplate;
	private final String stsUrl;

	@Inject
	public StsRestConsumer(@Value("${security-token-service-token.url}") String stsUrl,
						   RestTemplateBuilder restTemplateBuilder,
						   final ServiceuserAlias serviceuserAlias) {
		this.stsUrl = stsUrl;
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.basicAuthentication(serviceuserAlias.getUsername(), serviceuserAlias.getPassword())
				.build();
	}

	@Retryable(include = DokDistKanalTechnicalException.class, backoff = @Backoff(delay = DELAY_SHORT, multiplier = MULTIPLIER_SHORT))
	@Cacheable(STS_CACHE_NAME)
	public String getOidcToken() {
		try {
			return restTemplate.getForObject(stsUrl + "?grant_type=client_credentials&scope=openid", StsResponseTo.class)
					.getAccessToken();
		} catch (HttpStatusCodeException e) {
			throw new StsTechnicalException(String.format("Kall mot STS feilet med status=%s feilmelding=%s.", e.getStatusCode(), e
					.getMessage()), e);
		}
	}
}
