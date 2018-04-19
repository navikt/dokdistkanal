package no.nav.dokdistkanal.consumer.dki;

import static no.nav.dokdistkanal.metrics.PrometheusLabels.CACHE_MISS;
import static no.nav.dokdistkanal.metrics.PrometheusLabels.DIGITALKONTAKTINFORMASJONV1;
import static no.nav.dokdistkanal.metrics.PrometheusLabels.LABEL_CACHE_COUNTER;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.getConsumerId;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.requestCounter;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.requestLatency;

import io.prometheus.client.Histogram;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinformasjonTo;
import no.nav.dokdistkanal.exceptions.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import no.nav.dokdistkanal.exceptions.DokDistKanalTechnicalException;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.binding.DigitalKontaktinformasjonV1;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.binding.HentDigitalKontaktinformasjonKontaktinformasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.binding.HentDigitalKontaktinformasjonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.binding.HentDigitalKontaktinformasjonSikkerhetsbegrensing;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.Kontaktinformasjon;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.HentDigitalKontaktinformasjonRequest;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.HentDigitalKontaktinformasjonResponse;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.LocalDateTime;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.xml.datatype.XMLGregorianCalendar;

@Slf4j
@Service
public class DigitalKontaktinformasjonConsumer {

	private final DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1;
	private Histogram.Timer requestTimer;

	public static final String HENT_DIGITAL_KONTAKTINFORMASJON = "hentDigitalKontaktinformasjon";


	@Inject
	public DigitalKontaktinformasjonConsumer(DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1) {
		this.digitalKontaktinformasjonV1 = digitalKontaktinformasjonV1;
	}

//	@Cacheable(value = HENT_DIGITAL_KONTAKTINFORMASJON, key = "#personidentifikator")
	@Retryable(include = DokDistKanalTechnicalException.class, exclude = {DokDistKanalFunctionalException.class}, maxAttempts = 5, backoff = @Backoff(delay = 200))
	public DigitalKontaktinformasjonTo hentDigitalKontaktinformasjon(final String personidentifikator, final String serviceCode) throws DokDistKanalTechnicalException, DokDistKanalFunctionalException, DokDistKanalSecurityException {

		requestCounter.labels(HENT_DIGITAL_KONTAKTINFORMASJON, LABEL_CACHE_COUNTER, getConsumerId(), CACHE_MISS).inc();

		HentDigitalKontaktinformasjonRequest request = mapHentDigitalKontaktinformasjonRequest(personidentifikator);
		HentDigitalKontaktinformasjonResponse response;

		try {
			requestTimer = requestLatency.labels(serviceCode, DIGITALKONTAKTINFORMASJONV1, HENT_DIGITAL_KONTAKTINFORMASJON).startTimer();
			response = digitalKontaktinformasjonV1.hentDigitalKontaktinformasjon(request);
		} catch (HentDigitalKontaktinformasjonPersonIkkeFunnet hentDigitalKontaktinformasjonPersonIkkeFunnet) {
			throw new DokDistKanalFunctionalException("DigitalKontaktinformasjonV1.hentDigitakKontaktinformasjon fant ikke person med ident:" + personidentifikator + ", message=" + hentDigitalKontaktinformasjonPersonIkkeFunnet
					.getMessage(), hentDigitalKontaktinformasjonPersonIkkeFunnet);
		} catch (HentDigitalKontaktinformasjonKontaktinformasjonIkkeFunnet hentDigitalKontaktinformasjonKontaktinformasjonIkkeFunnet) {
			throw new DokDistKanalFunctionalException("DigitalKontaktinformasjonV1.hentDigitakKontaktinformasjon fant ikke kontaktinformasjon for person med ident:" + personidentifikator + ", message=" + hentDigitalKontaktinformasjonKontaktinformasjonIkkeFunnet
					.getMessage(), hentDigitalKontaktinformasjonKontaktinformasjonIkkeFunnet);
		} catch (HentDigitalKontaktinformasjonSikkerhetsbegrensing hentDigitalKontaktinformasjonSikkerhetsbegrensing) {
			throw new DokDistKanalSecurityException("DigitalKontaktinformasjonV1.hentDigitakKontaktinformasjon feiler på grunn av sikkerhetsbegresning. message=" + hentDigitalKontaktinformasjonSikkerhetsbegrensing
					.getMessage(), hentDigitalKontaktinformasjonSikkerhetsbegrensing);
		} catch (Exception e) {
//			if (e.getCause() instanceof SamlTokenInterceptorException){
//				throw new RegOppslagFunctionalException(e.getMessage());
//			}
			throw new DokDistKanalTechnicalException("Noe gikk galt i kall til DigitalKontaktinformasjonV1.hentDigitakKontaktinformasjon. message=" + e
					.getMessage());
		} finally {
			requestTimer.observeDuration();
		}
		if (response != null && response.getDigitalKontaktinformasjon() != null) {
			return mapTo(response.getDigitalKontaktinformasjon());
		}
		return null;
	}

	private HentDigitalKontaktinformasjonRequest mapHentDigitalKontaktinformasjonRequest(final String personidentifikator) {
		HentDigitalKontaktinformasjonRequest request = new HentDigitalKontaktinformasjonRequest();
		request.setPersonident(personidentifikator);
		return request;
	}

	private DigitalKontaktinformasjonTo mapTo(Kontaktinformasjon kontaktinformasjon) {

		return DigitalKontaktinformasjonTo.builder()
				.epostadresse(kontaktinformasjon.getEpostadresse().getValue())
				.mobiltelefonnummer(kontaktinformasjon.getMobiltelefonnummer().getValue())
				.reservasjon(mapStringToBool(kontaktinformasjon.getReservasjon())).build();


	}

	private boolean mapStringToBool(String bool) {
		if (StringUtils.isBlank(bool)) {
			return true;
		}
		switch (bool.toLowerCase()) {
			case "ja":
			case "true":
				return true;
			default:
				return false;
		}
	}

	private LocalDateTime map(XMLGregorianCalendar calendar) {
		return calendar == null ? null : LocalDateTime.fromCalendarFields(calendar.toGregorianCalendar());
	}

}
