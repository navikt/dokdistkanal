package no.nav.dokdistkanal.service;

import static java.lang.String.format;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.DITT_NAV;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.INGEN_DISTRIBUSJON;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.LOKAL_PRINT;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.PRINT;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.SDP;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.TRYGDERETTEN;
import static no.nav.dokdistkanal.common.MottakerTypeCode.PERSON;
import static no.nav.dokdistkanal.metrics.PrometheusMetrics.getConsumerId;

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
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.LocalDate;

/**
 * @author Ketill Fenne, Visma Consulting
 */
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

	public DokDistKanalResponse velgKanal(DokDistKanalRequest dokDistKanalRequest) {
		validateInput(dokDistKanalRequest);

		DokumentTypeInfoTo dokumentTypeInfoTo = dokumentTypeInfoConsumer.hentDokumenttypeInfo(dokDistKanalRequest.getDokumentTypeId());

		if ("INGEN".equals(dokumentTypeInfoTo.getArkivsystem())) {
			return logAndReturn(PRINT, "Skal ikke arkiveres");
		}
		if (LOKAL_PRINT.toString().equals(dokumentTypeInfoTo.getPredefinertDistKanal())) {
			return logAndReturn(LOKAL_PRINT, "Predefinert distribusjonskanal er Lokal Print");
		}
		if (INGEN_DISTRIBUSJON.toString().equals(dokumentTypeInfoTo.getPredefinertDistKanal())) {
			return logAndReturn(INGEN_DISTRIBUSJON, "Predefinert distribusjonskanal er Ingen Distribusjon");
		}
		if (TRYGDERETTEN.toString().equals(dokumentTypeInfoTo.getPredefinertDistKanal())) {
			return logAndReturn(TRYGDERETTEN, "Predefinert distribusjonskanal er Trygderetten");
		}

		if (!PERSON.equals(dokDistKanalRequest.getMottakerType())) {
			return logAndReturn(PRINT, String.format("Mottaker er av typen %s", dokDistKanalRequest.getMottakerType().name()));
		} else {
			PersonV3To personTo = personV3Consumer.hentPerson(dokDistKanalRequest.getMottakerId(), getConsumerId());

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

	private DokDistKanalResponse logAndReturn(DistribusjonKanalCode kanalKode, String reason) {
		LOG.info(String.format("BestemKanal: Sender melding til %s: %s", kanalKode.name(), reason));
		return DokDistKanalResponse.builder().distribusjonsKanal(kanalKode).build();
	}

	private void validateInput(DokDistKanalRequest dokDistKanalRequest) {
		assertNotNullOrEmpty("dokumentTypeId", dokDistKanalRequest.getDokumentTypeId());
		assertNotNullOrEmpty("mottakerId", dokDistKanalRequest.getMottakerId());
		assertNotNullOrEmpty("mottakerType", dokDistKanalRequest.getMottakerType() == null ?
				null : dokDistKanalRequest.getMottakerType().name());
		assertNotNullOrEmpty("brukerId", dokDistKanalRequest.getBrukerId());
		assertNotNull("erArkivert", dokDistKanalRequest.getErArkivert());
	}

	private static void assertNotNullOrEmpty(String fieldName, String value) {
		if (StringUtils.isEmpty(value)) {
			throw new UgyldingInputException(format("Ugyldig input: Feltet %s kan ikke være null eller tomt. Fikk %s=%s", fieldName, fieldName, value));
		}
	}

	private static void assertNotNull(String fieldName, Boolean value) {
		if (value == null) {
			throw new UgyldingInputException(format("Ugyldig input: Feltet %s kan ikke være null. Fikk %s=%s", fieldName, fieldName, value));
		}
	}

}