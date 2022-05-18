package no.nav.dokdistkanal.consumer.dki;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.azure.TokenConsumer;
import no.nav.dokdistkanal.azure.TokenResponse;
import no.nav.dokdistkanal.constants.MDCConstants;
import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinfoMapper;
import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinformasjonTo;
import no.nav.dokdistkanal.consumer.dki.to.DkifResponseTo;
import no.nav.dokdistkanal.consumer.dki.to.PostPersonerRequest;
import no.nav.dokdistkanal.exceptions.functional.DigitalKontaktinformasjonV2FunctionalException;
import no.nav.dokdistkanal.exceptions.functional.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.DigitalKontaktinformasjonV2TechnicalException;
import no.nav.dokdistkanal.exceptions.technical.DokDistKanalTechnicalException;
import no.nav.dokdistkanal.metrics.Metrics;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.time.Duration;
import java.util.Arrays;

import static java.lang.String.format;
import static no.nav.dokdistkanal.constants.DomainConstants.APP_NAME;
import static no.nav.dokdistkanal.constants.DomainConstants.BEARER_PREFIX;
import static no.nav.dokdistkanal.constants.MDCConstants.NAV_CALL_ID;
import static no.nav.dokdistkanal.constants.MDCConstants.NAV_CONSUMER_ID;
import static no.nav.dokdistkanal.constants.MDCConstants.NAV_PERSONIDENT;
import static no.nav.dokdistkanal.metrics.MetricLabels.DOK_CONSUMER;
import static no.nav.dokdistkanal.metrics.MetricLabels.PROCESS_CODE;

@Slf4j
@Component
public class DigitalKontaktinformasjonConsumer implements DigitalKontaktinformasjon {

	private final RestTemplate restTemplate;
	private final String dkiUrl;
	private final TokenConsumer tokenConsumer;
	private final DigitalKontaktinfoMapper digitalKontaktinfoMapper = new DigitalKontaktinfoMapper();

	public static final String HENT_SIKKER_DIGITAL_POSTADRESSE = "hentSikkerDigitalPostadresse";
	public static final String INGEN_KONTAKTINFORMASJON_FEILMELDING = "Ingen kontaktinformasjon er registrert på personen";

	@Autowired
	public DigitalKontaktinformasjonConsumer(RestTemplateBuilder restTemplateBuilder,
											 @Value("${digdir_krr_proxy_url}") String dkiUrl,
											 TokenConsumer tokenConsumer) {
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.build();
		this.dkiUrl = dkiUrl;
		this.tokenConsumer = tokenConsumer;
	}

	public void pingDkif() {
		HttpHeaders headers =  null;
		try {
			headers = createHeaders();
			String response = restTemplate.exchange(dkiUrl + "/rest/ping",
					HttpMethod.GET, new HttpEntity<>(headers), String.class).getBody();
			log.info("Pinget Dkif: " + response);
		} catch (Exception e) {
			log.error("Klarte ikke pinge Digdir KRR: " + e.getMessage());


		}
	}

	@Retryable(include = DokDistKanalTechnicalException.class, exclude = {DokDistKanalFunctionalException.class}, maxAttempts = 5, backoff = @Backoff(delay = 200))
	@Metrics(value = DOK_CONSUMER, extraTags = {PROCESS_CODE, HENT_SIKKER_DIGITAL_POSTADRESSE}, percentiles = {0.5, 0.95}, histogram = true)
	public DigitalKontaktinformasjonTo hentSikkerDigitalPostadresse(final String personidentifikator, final boolean inkluderSikkerDigitalPost) {
		HttpHeaders headers = createHeaders();
		final String fnrTrimmed = personidentifikator.trim();
		PostPersonerRequest postPersonRequest = PostPersonerRequest.builder().personidenter(Arrays.asList(fnrTrimmed)).build();
		HttpEntity<String> request = new HttpEntity(postPersonRequest, headers);
		try {
			String test = restTemplate.postForEntity(dkiUrl + "/rest/v1/personer?inkluderSikkerDigitalPost=" + inkluderSikkerDigitalPost, request, String.class).getBody();
			DkifResponseTo response = restTemplate.postForEntity(dkiUrl + "/rest/v1/personer?inkluderSikkerDigitalPost=" + inkluderSikkerDigitalPost, request, DkifResponseTo.class).getBody();
			if (isValidRespons(response, fnrTrimmed)) {
				return digitalKontaktinfoMapper.mapDigitalKontaktinformasjon(response.getPersoner().get(fnrTrimmed));
			} else {
				String errorMsg = getErrorMsg(response, fnrTrimmed);

				if (errorMsg != null && errorMsg.contains(INGEN_KONTAKTINFORMASJON_FEILMELDING)) {
					return null;
				} else {
					throw new DigitalKontaktinformasjonV2FunctionalException(format("Funksjonell feil ved kall mot DigitalKontaktinformasjonV1.kontaktinformasjon. Feilmelding=%s",
							errorMsg));
				}
			}
		} catch (HttpClientErrorException e) {
			throw new DigitalKontaktinformasjonV2FunctionalException(format("Funksjonell feil ved kall mot DigitalKontaktinformasjonV1.kontaktinformasjon. Feilmelding=%s", e
					.getMessage()), e);
		} catch (HttpServerErrorException e) {
			throw new DigitalKontaktinformasjonV2TechnicalException(format("Teknisk feil ved kall mot DigitalKontaktinformasjonV1.kontaktinformasjon. Feilmelding=%s", e
					.getMessage()), e);
		}
	}

	private boolean isValidRespons(DkifResponseTo response, String fnr) {
		return response != null && response.getPersoner() != null && response.getPersoner().get(fnr) != null;
	}

	private String getErrorMsg(DkifResponseTo response, String fnr) {
		if (response == null || response.getFeil() == null) {
			return null;
		} else {
			return response.getFeil().get(fnr).getMelding();
		}
	}

	private HttpHeaders createHeaders() {
		//TokenResponse clientCredentialToken = tokenConsumer.getClientCredentialToken();
		HttpHeaders headers = new HttpHeaders();
		//headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(HttpHeaders.AUTHORIZATION, "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsImtpZCI6ImpTMVhvMU9XRGpfNTJ2YndHTmd2UU8yVnpNYyJ9.eyJhdWQiOiIxOGUyYzdiYy1lNDdjLTQ5MTgtOTZmYy05N2Q4ZTBjNTJhNzYiLCJpc3MiOiJodHRwczovL2xvZ2luLm1pY3Jvc29mdG9ubGluZS5jb20vOTY2YWM1NzItZjViNy00YmJlLWFhODgtYzc2NDE5YzBmODUxL3YyLjAiLCJpYXQiOjE2NTI5MDc2OTgsIm5iZiI6MTY1MjkwNzY5OCwiZXhwIjoxNjUyOTExNTk4LCJhaW8iOiJFMlpnWUJCT1ZzcStFczgwZ2NWaDFydnNkTnMvQUE9PSIsImF6cCI6IjY3NzRkY2RlLTU1YTAtNDRmMy05MDNhLTQ3NWFjODU3ZjdlYiIsImF6cGFjciI6IjEiLCJvaWQiOiI3NDUwNmJlNi01ODk4LTQ4YWMtYjdjZi1iM2Q2YjhhMmJkNzkiLCJyaCI6IjAuQVVjQWNzVnFscmYxdmt1cWlNZGtHY0Q0VWJ6SDRoaDg1QmhKbHZ5WDJPREZLblpIQUFBLiIsInJvbGVzIjpbImFjY2Vzc19hc19hcHBsaWNhdGlvbiJdLCJzdWIiOiI3NDUwNmJlNi01ODk4LTQ4YWMtYjdjZi1iM2Q2YjhhMmJkNzkiLCJ0aWQiOiI5NjZhYzU3Mi1mNWI3LTRiYmUtYWE4OC1jNzY0MTljMGY4NTEiLCJ1dGkiOiIydzU3em1wdE1VV3l0WGhTQ2tvU0FBIiwidmVyIjoiMi4wIiwiYXpwX25hbWUiOiJkZXYtZnNzOnRlYW1kb2t1bWVudGhhbmR0ZXJpbmc6ZG9rZGlzdGthbmFsIn0.Bo3eKh809A6E8DVAIIDlp3U8GEJnnSMZvRUhy0gT3SVrDGutuzFzw179rCt8agnr5-K67SBEJR-X3D67sXk5JhJ6YpT3g3RU46GP6rYMeSATr6_M0e8CiE4pWV4DTcwcwWcBY_AUmZgxGCg_xbdvQ6Kq3Y4kzuz3Dr_GyWuurt5cQetFnRXccyiWu7kXsZx8TstBgijlOGo5co1dajyJMbTsz6iWtl9-z6QGYWBSaYlc4n_OlhqTu24h0_pjXE6ww16F22p_X5AeSTFzBFrwce2_hfHPdq8OXs9uXGXjIWaaow98a2g-4rzyUXCL3frghBIjYar87Yp4eNKw0GWyng");
		//headers.add(NAV_CONSUMER_ID, APP_NAME);
		headers.add(NAV_CALL_ID, "HS-TEST");
		return headers;
	}
}
