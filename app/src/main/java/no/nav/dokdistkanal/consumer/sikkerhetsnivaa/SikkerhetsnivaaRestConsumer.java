package no.nav.dokdistkanal.consumer.sikkerhetsnivaa;

import static no.nav.dokdistkanal.metrics.PrometheusLabels.CACHE_COUNTER;
import static no.nav.dokdistkanal.metrics.PrometheusLabels.CACHE_MISS;
import static no.nav.dokdistkanal.metrics.PrometheusLabels.LABEL_DOKDIST;
import static no.nav.dokdistkanal.metrics.PrometheusLabels.SIKKERHETSNIVAAV1;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.getConsumerId;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.requestCounter;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.requestLatency;

import io.prometheus.client.Histogram;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.config.fasit.ServiceuserAlias;
import no.nav.dokdistkanal.config.fasit.SikkerhetsnivaaV1Alias;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.schema.SikkerhetsnivaaRequest;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.schema.SikkerhetsnivaaResponse;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.to.SikkerhetsnivaaTo;
import no.nav.dokdistkanal.exceptions.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import no.nav.dokdistkanal.exceptions.DokDistKanalTechnicalException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.util.Assert;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;

@Slf4j
public class SikkerhetsnivaaRestConsumer implements SikkerhetsnivaaConsumer {

	private final RestTemplate restTemplate;
	public static final String HENT_PAALOGGINGSNIVAA = "hentPaaloggingsnivaa";
	private Histogram.Timer requestTimer;

	public SikkerhetsnivaaRestConsumer(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	@Inject
	public SikkerhetsnivaaRestConsumer(RestTemplateBuilder restTemplateBuilder,
									   HttpComponentsClientHttpRequestFactory requestFactory,
									   SikkerhetsnivaaV1Alias sikkerhetsnivaaV1Alias,
									   ServiceuserAlias serviceuserAlias) {
		this.restTemplate = restTemplateBuilder
				.requestFactory(requestFactory)
				.rootUri(sikkerhetsnivaaV1Alias.getUrl())
				.basicAuthorization(serviceuserAlias.getUsername(), serviceuserAlias.getPassword())
				.setConnectTimeout(sikkerhetsnivaaV1Alias.getConnecttimeoutms())
				.setReadTimeout(sikkerhetsnivaaV1Alias.getReadtimeoutms())
				.build();
	}


	@Override
	@Retryable(include = DokDistKanalTechnicalException.class, exclude = {DokDistKanalFunctionalException.class}, maxAttempts = 5, backoff = @Backoff(delay = 200))
	@Cacheable(value = HENT_PAALOGGINGSNIVAA, key = "#fnr+'-sikkerhetsnivaa'")
	public SikkerhetsnivaaTo hentPaaloggingsnivaa(String fnr) throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		SikkerhetsnivaaRequest request = SikkerhetsnivaaRequest.builder().personidentifikator(fnr).build();
		requestCounter.labels(HENT_PAALOGGINGSNIVAA, CACHE_COUNTER, getConsumerId(), CACHE_MISS).inc();
		try {
			requestTimer = requestLatency.labels(LABEL_DOKDIST, SIKKERHETSNIVAAV1, HENT_PAALOGGINGSNIVAA).startTimer();
			SikkerhetsnivaaResponse response = restTemplate.postForObject("/", request, SikkerhetsnivaaResponse.class);
			return mapTo(response);
		} catch (HttpClientErrorException e) {
			if (HttpStatus.UNAUTHORIZED.equals(e.getStatusCode()) || HttpStatus.FORBIDDEN.equals(e.getStatusCode())) {
				throw new DokDistKanalSecurityException("Sikkerhetsnivaa.hentPaaloggingsnivaa feilet (HttpStatus=" + e.getStatusCode() + ")", e);
			}
			if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
				//Personen finnes ikke, returnerer false
				return SikkerhetsnivaaTo.builder().harLoggetPaaNivaa4(false).personIdent(fnr).build();
			}
			throw new DokDistKanalFunctionalException("Sikkerhetsnivaa.hentPaaloggingsnivaa feilet (HttpStatus=" + e.getStatusCode() + ")", e);
		} catch (HttpServerErrorException e) {
			throw new DokDistKanalTechnicalException("Sikkerhetsnivaa.hentPaaloggingsnivaa feilet (HttpStatus=" + e.getStatusCode() + ")", e);
		} catch (Exception e) {
			throw new DokDistKanalTechnicalException("Sikkerhetsnivaa.hentPaaloggingsnivaa feilet", e);
		} finally {
			requestTimer.observeDuration();
		}
	}

	@Override
	public void ping() {
		String ping = restTemplate.getForObject("isReady", String.class);
		Assert.isTrue(StringUtils.isNotBlank(ping), "Sikkerhetsnivaa ping failed " + ping);
	}

	private SikkerhetsnivaaTo mapTo(SikkerhetsnivaaResponse response) {
		return SikkerhetsnivaaTo.builder().personIdent(response.getPersonidentifikator()).harLoggetPaaNivaa4(response.isHarbruktnivaa4()).build();
	}
}
