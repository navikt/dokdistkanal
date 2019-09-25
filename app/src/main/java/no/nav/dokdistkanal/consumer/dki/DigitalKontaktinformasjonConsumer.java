package no.nav.dokdistkanal.consumer.dki;

import static java.lang.String.format;
import static no.nav.dokdistkanal.constants.DomainConstants.APP_NAME;
import static no.nav.dokdistkanal.constants.DomainConstants.BEARER_PREFIX;
import static no.nav.dokdistkanal.constants.MDCConstants.NAV_CALL_ID;
import static no.nav.dokdistkanal.constants.MDCConstants.NAV_CONSUMER_ID;
import static no.nav.dokdistkanal.constants.MDCConstants.NAV_PERSONIDENT;
import static no.nav.dokdistkanal.metrics.MetricLabels.DOK_CONSUMER;
import static no.nav.dokdistkanal.metrics.MetricLabels.PROCESS_CODE;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.constants.MDCConstants;
import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinformasjonTo;
import no.nav.dokdistkanal.consumer.dki.to.KontaktInfo;
import no.nav.dokdistkanal.consumer.sts.StsRestConsumer;
import no.nav.dokdistkanal.exceptions.functional.DigitalKontaktinformasjonV2FunctionalException;
import no.nav.dokdistkanal.exceptions.functional.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.DigitalKontaktinformasjonV2TechnicalException;
import no.nav.dokdistkanal.exceptions.technical.DokDistKanalTechnicalException;
import no.nav.dokdistkanal.metrics.Metrics;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
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
import java.util.Map;

@Slf4j
@Component
public class DigitalKontaktinformasjonConsumer implements DigitalKontaktinformasjon {

	private final RestTemplate restTemplate;
	private final String dkiUrl;
	private final StsRestConsumer stsRestConsumer;

	public static final String HENT_SIKKER_DIGITAL_POSTADRESSE = "hentSikkerDigitalPostadresse";

	@Inject
	public DigitalKontaktinformasjonConsumer(RestTemplateBuilder restTemplateBuilder,
											 @Value("${dki_api_url}") String dkiUrl,
											 StsRestConsumer stsRestConsumer) {
		this.restTemplate = restTemplateBuilder
				.setReadTimeout(Duration.ofSeconds(20))
				.setConnectTimeout(Duration.ofSeconds(5))
				.build();
		this.dkiUrl = dkiUrl;
		this.stsRestConsumer = stsRestConsumer;
	}

	@Retryable(include = DokDistKanalTechnicalException.class, exclude = {DokDistKanalFunctionalException.class}, maxAttempts = 5, backoff = @Backoff(delay = 200))
	@Metrics(value = DOK_CONSUMER, extraTags = {PROCESS_CODE, HENT_SIKKER_DIGITAL_POSTADRESSE}, percentiles = {0.5, 0.95}, histogram = true)
	public DigitalKontaktinformasjonTo hentSikkerDigitalPostadresse(final String personidentifikator, final boolean inkluderSikkerDigitalPost) {
		HttpHeaders headers = createHeaders();
		final String fnrTrimmed = personidentifikator.trim();
		headers.add(NAV_PERSONIDENT, fnrTrimmed);

		try {
			Map<String, KontaktInfo> response = restTemplate.exchange(dkiUrl + "/api/v1/personer/kontaktinformasjon?" + inkluderSikkerDigitalPost,
					HttpMethod.GET, new HttpEntity<>(headers), new ParameterizedTypeReference<Map<String, KontaktInfo>>() {
					}).getBody();

			return mapDigitalKontaktinformasjon(response.get(fnrTrimmed));
		} catch (
				HttpClientErrorException e) {
			throw new DigitalKontaktinformasjonV2FunctionalException(format("Funksjonell feil ved kall mot DigitalKontaktinformasjonV2.digitalKontaktinformasjon feilmelding=%s", e
					.getMessage()), e);
		} catch (
				HttpServerErrorException e) {
			throw new DigitalKontaktinformasjonV2TechnicalException(format("Teknisk feil ved kall mot DigitalKontaktinformasjonV2.digitalKontaktinformasjon. Feilmelding=%s", e
					.getMessage()), e);
		}
	}

	private DigitalKontaktinformasjonTo mapDigitalKontaktinformasjon(KontaktInfo kontaktInfo) {

		if (kontaktInfo == null || kontaktInfo.getDigitalKontaktinfo() == null) {
			return null;
		} else {
			KontaktInfo.DigitalKontaktinfo digitalKontaktinfo = kontaktInfo.getDigitalKontaktinfo();

			return DigitalKontaktinformasjonTo.builder()
					.brukerAdresse(digitalKontaktinfo.getSikkerDigitalPostkasse() != null ? digitalKontaktinfo.getSikkerDigitalPostkasse()
							.getAdresse() : null)
					.epostadresse(digitalKontaktinfo.getEpostadresse())
					.leverandoerAdresse(digitalKontaktinfo.getSikkerDigitalPostkasse() != null ? digitalKontaktinfo.getSikkerDigitalPostkasse()
							.getLeverandoerAdresse() : null)
					.mobiltelefonnummer(digitalKontaktinfo.getMobiltelefonnummer())
					.reservasjon(digitalKontaktinfo.isReservert())
					.sertifikat(digitalKontaktinfo.getSikkerDigitalPostkasse() != null && isSertifikat(digitalKontaktinfo.getSikkerDigitalPostkasse()
							.getLeverandoerSertifikat()))
					.build();
		}
	}

	private boolean isSertifikat(String leverandoerSertifikat) {
		return leverandoerSertifikat != null && !leverandoerSertifikat.isEmpty();
	}

	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.set(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + stsRestConsumer.getOidcToken());
		headers.add(NAV_CONSUMER_ID, APP_NAME);
		headers.add(NAV_CALL_ID, MDC.get(MDCConstants.CALL_ID));
		return headers;
	}
}
