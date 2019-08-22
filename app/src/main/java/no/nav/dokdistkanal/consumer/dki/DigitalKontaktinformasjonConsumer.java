package no.nav.dokdistkanal.consumer.dki;

import static no.nav.dokdistkanal.metrics.MetricLabels.DOK_CONSUMER;
import static no.nav.dokdistkanal.metrics.MetricLabels.PROCESS_CODE;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinformasjonTo;
import no.nav.dokdistkanal.exceptions.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import no.nav.dokdistkanal.exceptions.DokDistKanalTechnicalException;
import no.nav.dokdistkanal.metrics.Metrics;
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
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;
import java.util.Arrays;

@Slf4j
@Service
public class DigitalKontaktinformasjonConsumer {

	private final DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1;

	public static final String HENT_SIKKER_DIGITAL_POSTADRESSE = "hentSikkerDigitalPostadresse";


	@Inject
	public DigitalKontaktinformasjonConsumer(DigitalKontaktinformasjonV1 digitalKontaktinformasjonV1) {
		this.digitalKontaktinformasjonV1 = digitalKontaktinformasjonV1;
	}

	@Cacheable(value = HENT_SIKKER_DIGITAL_POSTADRESSE, key = "#personidentifikator+'-dki'")
	@Retryable(include = DokDistKanalTechnicalException.class, exclude = {DokDistKanalFunctionalException.class}, maxAttempts = 5, backoff = @Backoff(delay = 200))
	@Metrics(value = DOK_CONSUMER, extraTags = {PROCESS_CODE, HENT_SIKKER_DIGITAL_POSTADRESSE}, percentiles = {0.5, 0.95}, histogram = true)
	public DigitalKontaktinformasjonTo hentSikkerDigitalPostadresse(final String personidentifikator) {
		HentSikkerDigitalPostadresseRequest request = mapHentDigitalKontaktinformasjonRequest(personidentifikator);
		HentSikkerDigitalPostadresseResponse response;

		try {
			response = digitalKontaktinformasjonV1.hentSikkerDigitalPostadresse(request);
		} catch (HentSikkerDigitalPostadresseKontaktinformasjonIkkeFunnet | HentSikkerDigitalPostadressePersonIkkeFunnet e) {
			return null;
		} catch (HentSikkerDigitalPostadresseSikkerhetsbegrensing e) {
			throw new DokDistKanalSecurityException("DigitalKontaktinformasjonV1.hentDigitakKontaktinformasjon feiler på grunn av sikkerhetsbegresning. " +
					"message=" + e.getMessage(), e);
		} catch (Exception e) {
			throw new DkifTechnicalException("Noe gikk galt i kall til DigitalKontaktinformasjonV1.hentDigitakKontaktinformasjon. " +
					"message=" + e.getMessage(), e);
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

		String mobiltelefonummer = null;
		String epostadresse = null;

		if (kontaktinformasjon != null) {
			if (isEpostadresseValid(kontaktinformasjon.getEpostadresse())) {
				epostadresse = kontaktinformasjon.getEpostadresse().getValue();
			}
			if (isMobilnummerValid(kontaktinformasjon.getMobiltelefonnummer())) {
				mobiltelefonummer = kontaktinformasjon.getMobiltelefonnummer().getValue();
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

	private boolean isEpostadresseValid(Epostadresse epostadresse) {
		if (epostadresse == null) {
			return false;
		} else if (epostadresse.getSistOppdatert() == null && epostadresse.getSistVerifisert() == null) {
			return false;
		} else if (epostadresse.getSistOppdatert() != null && isValidDate(epostadresse.getSistOppdatert())) {
			return true;
		} else if (epostadresse.getSistVerifisert() != null && isValidDate(epostadresse.getSistVerifisert())) {
			return true;
		} else {
			log.info("Epostadresse sist oppdatert {}, sist verifisert {}", epostadresse.getSistOppdatert(), epostadresse.getSistVerifisert());
			return false;
		}
	}

	private boolean isMobilnummerValid(Mobiltelefonnummer mobiltelefonnummer) {
		if (mobiltelefonnummer == null) {
			return false;
		} else if (mobiltelefonnummer.getSistOppdatert() == null && mobiltelefonnummer.getSistVerifisert() == null) {
			return false;
		} else if (mobiltelefonnummer.getSistOppdatert() != null && isValidDate(mobiltelefonnummer.getSistOppdatert())) {
			return true;
		} else if (mobiltelefonnummer.getSistVerifisert() != null && isValidDate(mobiltelefonnummer.getSistVerifisert())) {
			return true;
		} else {
			log.info("Mobilnummer sist oppdatert {}, sist verifisert {}", mobiltelefonnummer.getSistOppdatert(), mobiltelefonnummer.getSistVerifisert());
			return false;
		}
	}

	private boolean isValidDate(XMLGregorianCalendar dateTime) {
		return dateTime.toGregorianCalendar().toZonedDateTime().toLocalDate().plusMonths(18).isAfter(LocalDate.now());
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
