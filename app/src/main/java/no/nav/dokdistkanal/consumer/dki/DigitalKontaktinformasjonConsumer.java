package no.nav.dokdistkanal.consumer.dki;

import static no.nav.dokdistkanal.metrics.PrometheusLabels.CACHE_MISS;
import static no.nav.dokdistkanal.metrics.PrometheusLabels.DIGITALKONTAKTINFORMASJONV1;
import static no.nav.dokdistkanal.metrics.PrometheusLabels.LABEL_CACHE_COUNTER;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.getConsumerId;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.requestCounter;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.requestLatency;
import static no.nav.dokdistkanal.service.DokDistKanalService.DOKDISTKANAL_SERVICE;

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
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.Epostadresse;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.Kontaktinformasjon;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.Mobiltelefonnummer;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.informasjon.SikkerDigitalKontaktinformasjon;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.HentSikkerDigitalPostadresseRequest;
import no.nav.tjeneste.virksomhet.digitalkontaktinformasjon.v1.meldinger.HentSikkerDigitalPostadresseResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

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

	@Cacheable(value = HENT_SIKKER_DIGITAL_POSTADRESSE, key = "#personidentifikator+'-dki'")
	@Retryable(include = DokDistKanalTechnicalException.class, exclude = {DokDistKanalFunctionalException.class}, maxAttempts = 5, backoff = @Backoff(delay = 200))
	public DigitalKontaktinformasjonTo hentSikkerDigitalPostadresse(final String personidentifikator) throws DokDistKanalFunctionalException, DokDistKanalSecurityException {

		requestCounter.labels(HENT_SIKKER_DIGITAL_POSTADRESSE, LABEL_CACHE_COUNTER, getConsumerId(), CACHE_MISS).inc();

		HentSikkerDigitalPostadresseRequest request = mapHentDigitalKontaktinformasjonRequest(personidentifikator);
		HentSikkerDigitalPostadresseResponse response;

		try {
			requestTimer = requestLatency.labels(DOKDISTKANAL_SERVICE, DIGITALKONTAKTINFORMASJONV1, HENT_SIKKER_DIGITAL_POSTADRESSE).startTimer();
			response = digitalKontaktinformasjonV1.hentSikkerDigitalPostadresse(request);
		} catch (HentSikkerDigitalPostadresseKontaktinformasjonIkkeFunnet hentSikkerDigitalPostadresseKontaktinformasjonIkkeFunnet) {
			throw new DokDistKanalFunctionalException("DigitalKontaktinformasjonV1.hentDigitakKontaktinformasjon fant ikke kontaktinformasjon for person, message=" + hentSikkerDigitalPostadresseKontaktinformasjonIkkeFunnet
					.getMessage(), hentSikkerDigitalPostadresseKontaktinformasjonIkkeFunnet);
		} catch (HentSikkerDigitalPostadressePersonIkkeFunnet hentSikkerDigitalPostadressePersonIkkeFunnet) {
			throw new DokDistKanalFunctionalException("DigitalKontaktinformasjonV1.hentDigitakKontaktinformasjon fant ikke person, message=" + hentSikkerDigitalPostadressePersonIkkeFunnet
					.getMessage(), hentSikkerDigitalPostadressePersonIkkeFunnet);
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

		LocalDate monthsAgo18 = LocalDate.now().minusMonths(18);

		String mobiltelefonummer = null;
		String epostadresse = null;


		if (kontaktinformasjon != null) {

			//Dersom mobiltelefonnummeret er sist oppdatert for mer enn 18 måneder siden skal feltet blankes
			if (kontaktinformasjon.getMobiltelefonnummer().getSistOppdatert() != null) {
				LocalDate sistOppdatert = kontaktinformasjon.getMobiltelefonnummer().getSistOppdatert().toGregorianCalendar().toZonedDateTime().toLocalDate();
				if (sistOppdatert.isBefore(monthsAgo18)) {
					Mobiltelefonnummer mobiltelefonnummer = new Mobiltelefonnummer();
					kontaktinformasjon.setMobiltelefonnummer(mobiltelefonnummer);
				} else {
					mobiltelefonummer = kontaktinformasjon.getMobiltelefonnummer().getValue();
				}
			}

			//Dersom epostadresse er sist oppdatert for mer enn 18 måneder siden skal feltet blankes
			if (kontaktinformasjon.getEpostadresse().getSistOppdatert() != null) {
				LocalDate sistOppdatert = kontaktinformasjon.getMobiltelefonnummer().getSistOppdatert().toGregorianCalendar().toZonedDateTime().toLocalDate();
				if (sistOppdatert.isBefore(monthsAgo18)) {
					Epostadresse epostAdresse = new Epostadresse();
					kontaktinformasjon.setEpostadresse(epostAdresse);
				} else {
					epostadresse = kontaktinformasjon.getEpostadresse().getValue();
				}
			}
		}

		return DigitalKontaktinformasjonTo.builder()
				.leverandoerAdresse(digitalPostkasse == null ? null : digitalPostkasse.getLeverandoerAdresse())
				.brukerAdresse(digitalPostkasse == null ? null : digitalPostkasse.getBrukerAdresse())
				.epostadresse(epostadresse)
				.mobiltelefonnummer(mobiltelefonummer)
				.reservasjon(mapStringToBool(kontaktinformasjon == null ? null : kontaktinformasjon.getReservasjon()))
				.sertifikat(StringUtils.isNotEmpty(Arrays.toString((sertifikat)))).build();
	}

	private boolean mapStringToBool(String bool) {
		if (StringUtils.isBlank(bool)) {
			return true;
		}
		switch (bool.toLowerCase()) {
			case "ja":
				return true;
			case "true":
				return true;
			default:
				return false;
		}
	}
}
