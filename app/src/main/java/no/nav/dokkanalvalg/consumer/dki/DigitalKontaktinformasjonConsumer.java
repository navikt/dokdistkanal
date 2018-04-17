package no.nav.dokkanalvalg.consumer.dki;

import static no.nav.dokkanalvalg.metrics.PrometheusLabels.CACHE_MISS;
import static no.nav.dokkanalvalg.metrics.PrometheusLabels.DIGITALKONTAKTINFORMASJONV1;
import static no.nav.dokkanalvalg.metrics.PrometheusLabels.LABEL_CACHE_COUNTER;
import static no.nav.dokkanalvalg.metrics.PrometheusMetrics.getConsumerId;
import static no.nav.dokkanalvalg.metrics.PrometheusMetrics.requestCounter;
import static no.nav.dokkanalvalg.metrics.PrometheusMetrics.requestLatency;

import io.prometheus.client.Histogram;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokkanalvalg.consumer.dki.to.DigitalKontaktinformasjonTo;
import no.nav.dokkanalvalg.exceptions.DokKanalvalgFunctionalException;
import no.nav.dokkanalvalg.exceptions.DokKanalvalgSecurityException;
import no.nav.dokkanalvalg.exceptions.DokKanalvalgTechnicalException;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.binding.DigitalKontaktinformasjonV1;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.binding.HentDigitalKontaktinformasjonKontaktinformasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.binding.HentDigitalKontaktinformasjonPersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.binding.HentDigitalKontaktinformasjonSikkerhetsbegrensing;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.Kontaktinformasjon;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.HentDigitalKontaktinformasjonRequest;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.HentDigitalKontaktinformasjonResponse;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.inject.Inject;

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

	@Cacheable(value = HENT_DIGITAL_KONTAKTINFORMASJON, key = "#personidentifikator")
	@Retryable(include = DokKanalvalgTechnicalException.class, exclude = {DokKanalvalgFunctionalException.class}, maxAttempts = 5, backoff = @Backoff(delay = 200))
	public DigitalKontaktinformasjonTo hentDigitalKontaktinformasjon(final String personidentifikator, final String serviceCode) throws DokKanalvalgTechnicalException, DokKanalvalgFunctionalException, DokKanalvalgSecurityException {

		requestCounter.labels(HENT_DIGITAL_KONTAKTINFORMASJON, LABEL_CACHE_COUNTER, getConsumerId(), CACHE_MISS).inc();

		HentDigitalKontaktinformasjonRequest request = mapHentDigitalKontaktinformasjonRequest(personidentifikator);
		HentDigitalKontaktinformasjonResponse response;

		try {
			requestTimer = requestLatency.labels(serviceCode, DIGITALKONTAKTINFORMASJONV1, HENT_DIGITAL_KONTAKTINFORMASJON).startTimer();
			response = digitalKontaktinformasjonV1.hentDigitalKontaktinformasjon(request);
		} catch (HentDigitalKontaktinformasjonPersonIkkeFunnet hentDigitalKontaktinformasjonPersonIkkeFunnet) {
			throw new DokKanalvalgFunctionalException("DigitalKontaktinformasjonV1.hentDigitakKontaktinformasjon fant ikke person med ident:" + personidentifikator + ", message=" + hentDigitalKontaktinformasjonPersonIkkeFunnet
					.getMessage(), hentDigitalKontaktinformasjonPersonIkkeFunnet);
		} catch (HentDigitalKontaktinformasjonKontaktinformasjonIkkeFunnet hentDigitalKontaktinformasjonKontaktinformasjonIkkeFunnet) {
			throw new DokKanalvalgFunctionalException("DigitalKontaktinformasjonV1.hentDigitakKontaktinformasjon fant ikke kontaktinformasjon for person med ident:" + personidentifikator + ", message=" + hentDigitalKontaktinformasjonKontaktinformasjonIkkeFunnet
					.getMessage(), hentDigitalKontaktinformasjonKontaktinformasjonIkkeFunnet);
		} catch (HentDigitalKontaktinformasjonSikkerhetsbegrensing hentDigitalKontaktinformasjonSikkerhetsbegrensing) {
			throw new DokKanalvalgSecurityException("DigitalKontaktinformasjonV1.hentDigitakKontaktinformasjon feiler på grunn av sikkerhetsbegresning. message=" + hentDigitalKontaktinformasjonSikkerhetsbegrensing
					.getMessage(), hentDigitalKontaktinformasjonSikkerhetsbegrensing);
		} catch (Exception e) {
//			if (e.getCause() instanceof SamlTokenInterceptorException){
//				throw new RegOppslagFunctionalException(e.getMessage());
//			}
			throw new DokKanalvalgTechnicalException("Noe gikk galt i kall til DigitalKontaktinformasjonV1.hentDigitakKontaktinformasjon. message=" + e
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
				.mobiltelefon(kontaktinformasjon.getMobiltelefonnummer().getValue())
				.reservasjon(kontaktinformasjon.getReservasjon()).build();
	}

}
