package no.nav.dokdistkanal.consumer.sikkerhetsnivaa;

import static no.nav.dokdistkanal.common.FunctionalUtils.isNotEmpty;
import static no.nav.dokdistkanal.metrics.MetricLabels.DOK_CONSUMER;
import static no.nav.dokdistkanal.metrics.MetricLabels.PROCESS_CODE;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.config.fasit.ServiceuserAlias;
import no.nav.dokdistkanal.config.fasit.SikkerhetsnivaaV1Alias;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.schema.SikkerhetsnivaaRequest;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.schema.SikkerhetsnivaaResponse;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.to.SikkerhetsnivaaTo;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import no.nav.dokdistkanal.exceptions.functional.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.functional.SikkerhetsnivaaFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.DokDistKanalTechnicalException;
import no.nav.dokdistkanal.exceptions.technical.SikkerhetsnivaaTechnicalException;
import no.nav.dokdistkanal.metrics.Metrics;
import no.nav.dokdistkanal.metrics.MicrometerMetrics;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import org.springframework.beans.factory.annotation.Autowired;
import java.time.Duration;

@Slf4j
public class SikkerhetsnivaaConsumer {

	private final RestTemplate restTemplate;
	public static final String HENT_PAALOGGINGSNIVAA = "hentPaaloggingsnivaa";
	private static final String FEILMELDING = "Sikkerhetsnivaa.hentPaaloggingsnivaa feilet (HttpStatus=%s)";
	private MicrometerMetrics metrics;

	public SikkerhetsnivaaConsumer(RestTemplate restTemplate,
								   MicrometerMetrics metrics) {
		this.restTemplate = restTemplate;
		this.metrics = metrics;
	}

	@Autowired
	public SikkerhetsnivaaConsumer(RestTemplateBuilder restTemplateBuilder,
								   SikkerhetsnivaaV1Alias sikkerhetsnivaaV1Alias,
								   MicrometerMetrics metrics,
								   ServiceuserAlias serviceuserAlias) {
		this.restTemplate = restTemplateBuilder
				.rootUri(sikkerhetsnivaaV1Alias.getUrl())
				.basicAuthentication(serviceuserAlias.getUsername(), serviceuserAlias.getPassword())
				.setConnectTimeout(Duration.ofMillis(sikkerhetsnivaaV1Alias.getConnecttimeoutms()))
				.setReadTimeout(Duration.ofMillis(sikkerhetsnivaaV1Alias.getReadtimeoutms()))
				.build();
		this.metrics = metrics;
	}

	@Metrics(value = DOK_CONSUMER, extraTags = {PROCESS_CODE, HENT_PAALOGGINGSNIVAA}, percentiles = {0.5, 0.95}, histogram = true)
	@Retryable(include = DokDistKanalTechnicalException.class, exclude = {DokDistKanalFunctionalException.class}, maxAttempts = 5, backoff = @Backoff(delay = 200))
	@Cacheable(value = HENT_PAALOGGINGSNIVAA, key = "#fnr+'-sikkerhetsnivaa'")
	public SikkerhetsnivaaTo hentPaaloggingsnivaa(String fnr) {
		metrics.cacheMiss(HENT_PAALOGGINGSNIVAA);
		SikkerhetsnivaaRequest request = SikkerhetsnivaaRequest.builder().personidentifikator(fnr).build();
		try {
			SikkerhetsnivaaResponse response = restTemplate.postForObject("/", request, SikkerhetsnivaaResponse.class);
			return mapTo(response);
		} catch (HttpClientErrorException e) {
			if (HttpStatus.UNAUTHORIZED.equals(e.getStatusCode()) || HttpStatus.FORBIDDEN.equals(e.getStatusCode())) {
				throw new DokDistKanalSecurityException(String.format(FEILMELDING, e.getStatusCode()), e);
			}
			if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
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

	public void ping() {
		String ping = restTemplate.getForObject("isReady", String.class);
		Assert.isTrue(isNotEmpty(ping), "Sikkerhetsnivaa ping failed " + ping);
	}

	private SikkerhetsnivaaTo mapTo(SikkerhetsnivaaResponse response) {
		return SikkerhetsnivaaTo.builder()
				.personIdent(response.getPersonidentifikator())
				.harLoggetPaaNivaa4(response.isHarbruktnivaa4())
				.build();
	}
}
