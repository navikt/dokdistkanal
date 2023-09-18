package no.nav.dokdistkanal.consumer.sikkerhetsnivaa;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.config.properties.DokdistkanalProperties;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.schema.SikkerhetsnivaaRequest;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.schema.SikkerhetsnivaaResponse;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.to.SikkerhetsnivaaTo;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import no.nav.dokdistkanal.exceptions.functional.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.functional.SikkerhetsnivaaFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.DokDistKanalTechnicalException;
import no.nav.dokdistkanal.exceptions.technical.SikkerhetsnivaaTechnicalException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Slf4j
@Component
public class SikkerhetsnivaaConsumer {

	public static final String HENT_PAALOGGINGSNIVAA = "hentPaaloggingsnivaa";
	private static final String FEILMELDING = "Sikkerhetsnivaa.hentPaaloggingsnivaa feilet (HttpStatus=%s)";

	private final RestTemplate restTemplate;

	public SikkerhetsnivaaConsumer(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@Autowired
	public SikkerhetsnivaaConsumer(RestTemplateBuilder restTemplateBuilder,
								   DokdistkanalProperties dokdistkanalProperties) {
		this.restTemplate = restTemplateBuilder
				.rootUri(dokdistkanalProperties.getSikkerhetsnivaa().getUrl())
				.basicAuthentication(dokdistkanalProperties.getServiceuser().getUsername(), dokdistkanalProperties.getServiceuser().getPassword())
				.setConnectTimeout(Duration.ofMillis(dokdistkanalProperties.getSikkerhetsnivaa().getConnecttimeoutms()))
				.setReadTimeout(Duration.ofMillis(dokdistkanalProperties.getSikkerhetsnivaa().getReadtimeoutms()))
				.build();
	}

	@Retryable(retryFor = DokDistKanalTechnicalException.class, noRetryFor = {DokDistKanalFunctionalException.class}, maxAttempts = 5, backoff = @Backoff(delay = 200))
	@Cacheable(value = HENT_PAALOGGINGSNIVAA, key = "#fnr+'-sikkerhetsnivaa'")
	public SikkerhetsnivaaTo hentPaaloggingsnivaa(String fnr) {
		SikkerhetsnivaaRequest request = SikkerhetsnivaaRequest.builder().personidentifikator(fnr).build();

		try {
			SikkerhetsnivaaResponse response = restTemplate.postForObject("/", request, SikkerhetsnivaaResponse.class);
			return mapTo(response);
		} catch (HttpClientErrorException e) {
			if (UNAUTHORIZED.equals(e.getStatusCode()) || FORBIDDEN.equals(e.getStatusCode())) {
				throw new DokDistKanalSecurityException(String.format(FEILMELDING, e.getStatusCode()), e);
			}
			if (NOT_FOUND.equals(e.getStatusCode())) {
				//Personen finnes ikke, returnerer false
				return SikkerhetsnivaaTo.builder().harLoggetPaaNivaa4(false).personIdent(fnr).build();
			}
			throw new SikkerhetsnivaaFunctionalException(String.format(FEILMELDING, e.getStatusCode()), e);
		} catch (HttpServerErrorException e) {
			throw new SikkerhetsnivaaTechnicalException(String.format(FEILMELDING, e.getStatusCode()), e);
		} catch (Exception e) {
			throw new SikkerhetsnivaaTechnicalException("Sikkerhetsnivaa.hentPaaloggingsnivaa feilet", e);
		}
	}

	private SikkerhetsnivaaTo mapTo(SikkerhetsnivaaResponse response) {
		return SikkerhetsnivaaTo.builder()
				.personIdent(response.getPersonidentifikator())
				.harLoggetPaaNivaa4(response.isHarbruktnivaa4())
				.build();
	}
}
