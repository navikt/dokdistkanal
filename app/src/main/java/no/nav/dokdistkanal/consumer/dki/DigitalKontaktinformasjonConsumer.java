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
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.binding.HentSikkerDigitalPostadresseKontaktinformasjonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.binding.HentSikkerDigitalPostadressePersonIkkeFunnet;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.binding.HentSikkerDigitalPostadresseSikkerhetsbegrensing;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.DigitalPostkasse;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.Kontaktinformasjon;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.SikkerDigitalKontaktinformasjon;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.HentSikkerDigitalPostadresseRequest;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.HentSikkerDigitalPostadresseResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.util.Arrays;

@Slf4j
@Service
public class DigitalKontaktinformasjonConsumer {

	private final DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1;
	private Histogram.Timer requestTimer;

	public static final String HENT_SIKKER_DIGITAL_POSTADRESSE = "hentSikkerDigitalPostadresse";


	@Inject
	public DigitalKontaktinformasjonConsumer(DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1) {
		this.digitalKontaktinformasjonV1 = digitalKontaktinformasjonV1;
	}

	@Cacheable(HENT_SIKKER_DIGITAL_POSTADRESSE)
	@Retryable(include = DokDistKanalTechnicalException.class, exclude = {DokDistKanalFunctionalException.class}, maxAttempts = 5, backoff = @Backoff(delay = 200))
	public DigitalKontaktinformasjonTo hentSikkerDigitalPostadresse(final String personidentifikator, final String serviceCode) throws DokDistKanalFunctionalException, DokDistKanalSecurityException {

		requestCounter.labels(HENT_SIKKER_DIGITAL_POSTADRESSE, LABEL_CACHE_COUNTER, getConsumerId(), CACHE_MISS).inc();

		HentSikkerDigitalPostadresseRequest request = mapHentDigitalKontaktinformasjonRequest(personidentifikator);
		HentSikkerDigitalPostadresseResponse response;

		try {
			requestTimer = requestLatency.labels(serviceCode, DIGITALKONTAKTINFORMASJONV1, HENT_SIKKER_DIGITAL_POSTADRESSE).startTimer();
			response = digitalKontaktinformasjonV1.hentSikkerDigitalPostadresse(request);
		} catch (HentSikkerDigitalPostadressePersonIkkeFunnet hentSikkerDigitalPostadressePersonIkkeFunnet) {
			throw new DokDistKanalFunctionalException("DigitalKontaktinformasjonV1.hentDigitakKontaktinformasjon fant ikke person, message=" + hentSikkerDigitalPostadressePersonIkkeFunnet
					.getMessage(), hentSikkerDigitalPostadressePersonIkkeFunnet);
		} catch (HentSikkerDigitalPostadresseKontaktinformasjonIkkeFunnet hentSikkerDigitalPostadresseKontaktinformasjonIkkeFunnet) {
			throw new DokDistKanalFunctionalException("DigitalKontaktinformasjonV1.hentDigitakKontaktinformasjon fant ikke kontaktinformasjon for person, message=" + hentSikkerDigitalPostadresseKontaktinformasjonIkkeFunnet
					.getMessage(), hentSikkerDigitalPostadresseKontaktinformasjonIkkeFunnet);
		} catch (HentSikkerDigitalPostadresseSikkerhetsbegrensing hentSikkerDigitalPostadresseSikkerhetsbegrensing) {
			throw new DokDistKanalSecurityException("DigitalKontaktinformasjonV1.hentDigitakKontaktinformasjon feiler på grunn av sikkerhetsbegresning. message=" + hentSikkerDigitalPostadresseSikkerhetsbegrensing
					.getMessage(), hentSikkerDigitalPostadresseSikkerhetsbegrensing);
		} catch (Exception e) {
			throw new DokDistKanalTechnicalException("Noe gikk galt i kall til DigitalKontaktinformasjonV1.hentDigitakKontaktinformasjon. message=" + e
					.getMessage());
		} finally {
			requestTimer.observeDuration();
		}
		if (response != null && response.getSikkerDigitalKontaktinformasjon() != null) {
			return mapTo(response.getSikkerDigitalKontaktinformasjon());
		}
		return null;
	}


	private HentSikkerDigitalPostadresseRequest mapHentDigitalKontaktinformasjonRequest(final String personidentifikator) {
		HentSikkerDigitalPostadresseRequest request = new HentSikkerDigitalPostadresseRequest();
		request.setPersonident(personidentifikator);
		return request;
	}

	private DigitalKontaktinformasjonTo mapTo(SikkerDigitalKontaktinformasjon sikkerDigitalKontaktinformasjon) {

		DigitalPostkasse digitalPostkasse = null;
		if (sikkerDigitalKontaktinformasjon.getSikkerDigitalPostkasse() != null) {
			digitalPostkasse = sikkerDigitalKontaktinformasjon.getSikkerDigitalPostkasse();
		}

		Kontaktinformasjon kontaktinformasjon = null;
		if (sikkerDigitalKontaktinformasjon.getDigitalKontaktinformasjon() != null) {
			kontaktinformasjon = sikkerDigitalKontaktinformasjon.getDigitalKontaktinformasjon();
		}

		byte[] sertifikat = sikkerDigitalKontaktinformasjon.getSertifikat();


		return DigitalKontaktinformasjonTo.builder()
				.leverandoerAdresse(digitalPostkasse == null ? null : digitalPostkasse.getLeverandoerAdresse())
				.brukerAdresse(digitalPostkasse == null ? null : digitalPostkasse.getBrukerAdresse())
				.epostadresse(kontaktinformasjon == null ? null : kontaktinformasjon.getEpostadresse().getValue())
				.mobiltelefonnummer(kontaktinformasjon == null ? null : kontaktinformasjon.getMobiltelefonnummer().getValue())
				.reservasjon(mapStringToBool(kontaktinformasjon==null ? null: kontaktinformasjon.getReservasjon()))
				.sertifikat(StringUtils.isNotEmpty(Arrays.toString((sertifikat)))).build();
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
}
