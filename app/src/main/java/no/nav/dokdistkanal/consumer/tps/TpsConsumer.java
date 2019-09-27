package no.nav.dokdistkanal.consumer.tps;

import static java.lang.String.format;
import static no.nav.dokdistkanal.constants.DomainConstants.BEARER_PREFIX;
import static no.nav.dokdistkanal.constants.MDCConstants.CALL_ID;
import static no.nav.dokdistkanal.constants.MDCConstants.NAV_CALL_ID;
import static no.nav.dokdistkanal.constants.MDCConstants.NAV_CONSUMER_ID;
import static no.nav.dokdistkanal.constants.MDCConstants.NAV_PERSONIDENT;
import static no.nav.dokdistkanal.constants.RetryConstants.DELAY_SHORT;
import static no.nav.dokdistkanal.metrics.MetricLabels.PROCESS_CODE;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.consumer.sts.StsRestConsumer;
import no.nav.dokdistkanal.consumer.tps.to.TpsHentPersoninfoForIdentResponseTo;
import no.nav.dokdistkanal.consumer.tps.to.TpsHentPersoninfoForIdentTo;
import no.nav.dokdistkanal.exceptions.functional.TpsHentNavnFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.TpsHentNavnTechnicalException;
import no.nav.dokdistkanal.metrics.Metrics;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import javax.inject.Inject;
import java.time.Duration;
import java.time.LocalDate;

/**
 * @author Heidi Elisabeth Sando, Visma Consulting.
 */

@Slf4j
@Component
public class TpsConsumer implements Tps {
	private static final String HENT_PERSONINFO_FOR_IDENT = "hentPersoninfoForIdent";

	private final RestTemplate restTemplate;
	private final String tpsProxyUrl;
	private final StsRestConsumer stsRestConsumer;

	@Inject
	public TpsConsumer(RestTemplateBuilder restTemplateBuilder,
					   @Value("${tpsproxy_api_url}") String tpsProxyUrl,
					   StsRestConsumer stsRestConsumer) {
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.build();
		this.tpsProxyUrl = tpsProxyUrl;
		this.stsRestConsumer = stsRestConsumer;
	}

	@Metrics(value = "dok_metric", extraTags = {PROCESS_CODE, HENT_PERSONINFO_FOR_IDENT}, percentiles = {0.5, 0.95}, histogram = true)
	@Retryable(include = TpsHentNavnTechnicalException.class, maxAttempts = 5, backoff = @Backoff(delay = DELAY_SHORT))
	public TpsHentPersoninfoForIdentTo tpsHentPersoninfoForIdent(final String fnr, final String consumerId) {
		final String fnrTrimmed = fnr.trim();
		HttpHeaders headers = createHeaders();
		headers.add(NAV_PERSONIDENT, fnrTrimmed);
		headers.add(NAV_CONSUMER_ID, consumerId);
		TpsHentPersoninfoForIdentResponseTo response;

		try {
			response = restTemplate.exchange(tpsProxyUrl + "/v1/innsyn/person", HttpMethod.GET, new HttpEntity<>(headers), TpsHentPersoninfoForIdentResponseTo.class)
					.getBody();
			return mapTo(response);
		} catch (HttpClientErrorException e) {
			throw new TpsHentNavnFunctionalException(format("Funksjonell feil ved kall mot tpsProxy:hentPersoninfoForIdent. feilmelding=%s", e
					.getMessage()), e);
		} catch (HttpServerErrorException e) {
			throw new TpsHentNavnTechnicalException(format("Teknisk feil ved kall mot tpsProxy:hentPersoninfoForIdent. Feilmelding=%s", e
					.getMessage()), e);
		}
	}

	private TpsHentPersoninfoForIdentTo mapTo(TpsHentPersoninfoForIdentResponseTo response) {
		if (response == null) {
			return null;
		} else {
			return TpsHentPersoninfoForIdentTo.builder()
					.doedsdato((response.getDoedsdato() != null && response.getDoedsdato()
							.getDato() != null) ? LocalDate.parse(response.getDoedsdato().getDato()) : null)
					.foedselsdato(response.getFoedselsdato() != null ? LocalDate.parse(response.getFoedselsdato()) : null)
					.build();
		}
	}

	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + stsRestConsumer.getOidcToken());
		headers.add(NAV_CALL_ID, MDC.get(CALL_ID));
		return headers;
	}
}