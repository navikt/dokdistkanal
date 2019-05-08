package no.nav.dokdistkanal.service;

import static java.lang.String.format;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.DITT_NAV;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.INGEN_DISTRIBUSJON;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.LOKAL_PRINT;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.PRINT;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.SDP;
import static no.nav.dokdistkanal.common.MottakerTypeCode.PERSON;
import static no.nav.dokdistkanal.consumer.dki.DigitalKontaktinformasjonConsumer.HENT_SIKKER_DIGITAL_POSTADRESSE;
import static no.nav.dokdistkanal.consumer.dokkat.DokumentTypeInfoConsumer.HENT_DOKKAT_INFO;
import static no.nav.dokdistkanal.consumer.personv3.PersonV3Consumer.HENT_PERSON;
import static no.nav.dokdistkanal.consumer.sikkerhetsnivaa.SikkerhetsnivaaRestConsumer.HENT_PAALOGGINGSNIVAA;
import static no.nav.dokdistkanal.metrics.PrometheusLabels.CACHE_COUNTER;
import static no.nav.dokdistkanal.metrics.PrometheusLabels.CACHE_TOTAL;
import static no.nav.dokdistkanal.metrics.PrometheusLabels.LABEL_DOKDIST;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.getConsumerId;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.requestCounter;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.common.DistribusjonKanalCode;
import no.nav.dokdistkanal.common.DokDistKanalRequest;
import no.nav.dokdistkanal.common.DokDistKanalResponse;
import no.nav.dokdistkanal.consumer.dki.DigitalKontaktinformasjonConsumer;
import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinformasjonTo;
import no.nav.dokdistkanal.consumer.dokkat.DokumentTypeInfoConsumer;
import no.nav.dokdistkanal.consumer.dokkat.to.DokumentTypeInfoTo;
import no.nav.dokdistkanal.consumer.personv3.PersonV3Consumer;
import no.nav.dokdistkanal.consumer.personv3.to.PersonV3To;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.SikkerhetsnivaaConsumer;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.to.SikkerhetsnivaaTo;
import no.nav.dokdistkanal.exceptions.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.LocalDate;

/**
 * @author Ketill Fenne, Visma Consulting
 */
@Slf4j
@Service
public class DokDistKanalService {

	public static final Logger LOG = LoggerFactory.getLogger(DokDistKanalService.class);

	private final DokumentTypeInfoConsumer dokumentTypeInfoConsumer;
	private final PersonV3Consumer personV3Consumer;
	private final DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer;
	private final SikkerhetsnivaaConsumer sikkerhetsnivaaConsumer;

	@Inject
	DokDistKanalService(DokumentTypeInfoConsumer dokumentTypeInfoConsumer, PersonV3Consumer personV3Consumer, DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer, SikkerhetsnivaaConsumer sikkerhetsnivaaConsumer) {
		this.dokumentTypeInfoConsumer = dokumentTypeInfoConsumer;
		this.personV3Consumer = personV3Consumer;
		this.digitalKontaktinformasjonConsumer = digitalKontaktinformasjonConsumer;
		this.sikkerhetsnivaaConsumer = sikkerhetsnivaaConsumer;
	}

	public DokDistKanalResponse velgKanal(DokDistKanalRequest dokDistKanalRequest) throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		validateInput(dokDistKanalRequest);

		DokumentTypeInfoTo dokumentTypeInfoTo = dokumentTypeInfoConsumer.hentDokumenttypeInfo(dokDistKanalRequest.getDokumentTypeId());
		requestCounter.labels(HENT_DOKKAT_INFO, CACHE_COUNTER, getConsumerId(), CACHE_TOTAL).inc();

		if ("INGEN".equals(dokumentTypeInfoTo.getArkivsystem())) {
			return logAndReturn(PRINT, "Skal ikke arkiveres");
		}
		if (LOKAL_PRINT.toString().equals(dokumentTypeInfoTo.getPredefinertDistKanal())) {
			return logAndReturn(LOKAL_PRINT, "Predefinert distribusjonskanal er Lokal Print");
		}
		if (INGEN_DISTRIBUSJON.toString().equals(dokumentTypeInfoTo.getPredefinertDistKanal())) {
			return logAndReturn(INGEN_DISTRIBUSJON, "Predefinert distribusjonskanal er Ingen Distribusjon");
		}

		if (!PERSON.equals(dokDistKanalRequest.getMottakerType())) {
			return logAndReturn(PRINT, String.format("Mottaker er av typen %s", dokDistKanalRequest.getMottakerType().name()));
		} else {
			PersonV3To personTo = personV3Consumer.hentPerson(dokDistKanalRequest.getMottakerId(), getConsumerId());
			requestCounter.labels(HENT_PERSON, CACHE_COUNTER, getConsumerId(), CACHE_TOTAL).inc();

			if (personTo == null) {
				return logAndReturn(PRINT, "Finner ikke personen i TPS");
			}

			if (personTo.getDoedsdato() != null) {
				return logAndReturn(PRINT, "Personen er død");
			}

			if (personTo.getFoedselsdato() == null) {
				return logAndReturn(PRINT, "Personens alder er ukjent");
			}

			if (LocalDate.now().minusYears(18).isBefore(personTo.getFoedselsdato())) {
				return logAndReturn(PRINT, "Personen må være minst 18 år gammel");
			}

			DigitalKontaktinformasjonTo dki = digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(dokDistKanalRequest
					.getMottakerId());
			requestCounter.labels(HENT_SIKKER_DIGITAL_POSTADRESSE, CACHE_COUNTER, getConsumerId(), CACHE_TOTAL).inc();
			if (dki == null) {
				return logAndReturn(PRINT, "Finner ikke Digital kontaktinformasjon");
			}

			if (dki.isReservasjon()) {
				return logAndReturn(PRINT, "Bruker har reservert seg");
			}
			if (dokumentTypeInfoTo.isVarslingSdp() && StringUtils.isBlank(dki.getEpostadresse()) && StringUtils.isBlank(dki.getMobiltelefonnummer())) {
				return logAndReturn(PRINT, "Bruker skal varsles, men verken mobiltelefonnummer eller epostadresse har verdi");
			}
			if (dki.verifyAddress()) {
				return logAndReturn(SDP, "Sertifikat, LeverandørAddresse og BrukerAdresse har verdi.");
			}
			if (StringUtils.isBlank(dki.getEpostadresse()) && StringUtils.isBlank(dki.getMobiltelefonnummer())) {
				return logAndReturn(PRINT, "Epostadresse og mobiltelefon - feltene er tomme");
			}

			SikkerhetsnivaaTo sikkerhetsnivaaTo = sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(dokDistKanalRequest.getMottakerId());
			requestCounter.labels(HENT_PAALOGGINGSNIVAA, CACHE_COUNTER, getConsumerId(), CACHE_TOTAL).inc();
			if (sikkerhetsnivaaTo == null) {
				return logAndReturn(PRINT, "Paaloggingsnivaa ikke tilgjengelig");
			}

			if (!dokDistKanalRequest.getMottakerId().equals(dokDistKanalRequest.getBrukerId())) {
				return logAndReturn(PRINT, "Bruker og mottaker er forskjellige");
			}

			if (!dokDistKanalRequest.getErArkivert()) {
				return logAndReturn(PRINT, "Dokumentet er ikke arkivert");
			}

			if (sikkerhetsnivaaTo.isHarLoggetPaaNivaa4()) {
				return logAndReturn(DITT_NAV, "Bruker har logget på med nivaa4 de siste 18 mnd");
			}

			return logAndReturn(PRINT, "Bruker har ikke logget på med nivaa4 de siste 18 mnd");
		}
	}

	private DokDistKanalResponse logAndReturn(DistribusjonKanalCode code, String reason) {
		LOG.info("BestemKanal: Sender melding til " + code.name() + ": " + reason);
		requestCounter.labels(LABEL_DOKDIST, "velgKanal", getConsumerId(), code.name()).inc();
		return DokDistKanalResponse.builder().distribusjonsKanal(code).build();
	}

	private void validateInput(DokDistKanalRequest dokDistKanalRequest) throws DokDistKanalFunctionalException {
		assertNotNullOrEmpty("dokumentTypeId", dokDistKanalRequest.getDokumentTypeId());
		assertNotNullOrEmpty("mottakerId", dokDistKanalRequest.getMottakerId());
		assertNotNullOrEmpty("mottakerType", dokDistKanalRequest.getMottakerType() == null ?
				null : dokDistKanalRequest.getMottakerType().name());
		assertNotNullOrEmpty("brukerId", dokDistKanalRequest.getBrukerId());
		assertNotNull("erArkivert", dokDistKanalRequest.getErArkivert());
	}

	private static void assertNotNullOrEmpty(String fieldName, String value) throws DokDistKanalFunctionalException {
		if (StringUtils.isEmpty(value)) {
			throw new DokDistKanalFunctionalException(format("Ugyldig input: Feltet %s kan ikke være null eller tomt. Fikk %s=%s", fieldName, fieldName, value));
		}
	}

	private static void assertNotNull(String fieldName, Boolean value) throws DokDistKanalFunctionalException {
		if (value == null) {
			throw new DokDistKanalFunctionalException(format("Ugyldig input: Feltet %s kan ikke være null. Fikk %s=%s", fieldName, fieldName, value));
		}
	}

}