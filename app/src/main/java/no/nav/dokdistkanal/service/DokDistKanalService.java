package no.nav.dokdistkanal.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import no.nav.dokdistkanal.common.DistribusjonKanalCode;
import no.nav.dokdistkanal.common.DokDistKanalRequest;
import no.nav.dokdistkanal.common.DokDistKanalResponse;
import no.nav.dokdistkanal.consumer.dki.DigitalKontaktinformasjon;
import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinformasjonTo;
import no.nav.dokdistkanal.consumer.dokkat.DokumentTypeInfoConsumer;
import no.nav.dokdistkanal.consumer.dokkat.DokumentTypeInfoTo;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.SikkerhetsnivaaConsumer;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.to.SikkerhetsnivaaTo;
import no.nav.dokdistkanal.consumer.tps.Tps;
import no.nav.dokdistkanal.consumer.tps.to.TpsHentPersoninfoForIdentTo;
import no.nav.dokdistkanal.exceptions.functional.UgyldingInputFunctionalException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.inject.Inject;
import java.time.LocalDate;

import static java.lang.String.format;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.DITT_NAV;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.INGEN_DISTRIBUSJON;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.LOKAL_PRINT;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.PRINT;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.SDP;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.TRYGDERETTEN;
import static no.nav.dokdistkanal.common.FunctionalUtils.isEmpty;
import static no.nav.dokdistkanal.common.MottakerTypeCode.PERSON;
import static no.nav.dokdistkanal.rest.DokDistKanalRestController.BESTEM_DISTRIBUSJON_KANAL;

/**
 * @author Ketill Fenne, Visma Consulting
 */
@Service
@Component
public class DokDistKanalService {
	public static final Logger LOG = LoggerFactory.getLogger(DokDistKanalService.class);

	private static final String AARSOPPGAVE_DOKUMENTTYPEID_1 = "000053";
	private static final String AARSOPPGAVE_DOKUMENTTYPEID_2 = "000077";

	private final DokumentTypeInfoConsumer dokumentTypeInfoConsumer;
	private final DigitalKontaktinformasjon digitalKontaktinformasjon;
	private final SikkerhetsnivaaConsumer sikkerhetsnivaaConsumer;
	private final MeterRegistry registry;
	private final Tps tps;

	@Inject
	DokDistKanalService(DokumentTypeInfoConsumer dokumentTypeInfoConsumer,
						DigitalKontaktinformasjon digitalKontaktinformasjon,
						SikkerhetsnivaaConsumer sikkerhetsnivaaConsumer,
						MeterRegistry registry,
						Tps tps) {
		this.dokumentTypeInfoConsumer = dokumentTypeInfoConsumer;
		this.digitalKontaktinformasjon = digitalKontaktinformasjon;
		this.sikkerhetsnivaaConsumer = sikkerhetsnivaaConsumer;
		this.registry = registry;
		this.tps = tps;
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
			TpsHentPersoninfoForIdentTo personTo = tps.tpsHentPersoninfoForIdent(dokDistKanalRequest.getMottakerId());

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

			DigitalKontaktinformasjonTo dki = digitalKontaktinformasjon.hentSikkerDigitalPostadresse(dokDistKanalRequest
					.getMottakerId(), true);
			if (dki == null) {
				return logAndReturn(PRINT, "Finner ikke Digital kontaktinformasjon");
			}

			if (dki.isReservasjon()) {
				return logAndReturn(PRINT, "Bruker har reservert seg");
			}
			if (dokumentTypeInfoTo.isVarslingSdp() && isEmpty(dki.getEpostadresse()) && isEmpty(dki.getMobiltelefonnummer())) {
				return logAndReturn(PRINT, "Bruker skal varsles, men verken mobiltelefonnummer eller epostadresse har verdi");
			}
			if (dki.verifyAddress()) {
				return logAndReturn(SDP, "Sertifikat, LeverandørAddresse og BrukerAdresse har verdi.");
			}
			if (isEmpty(dki.getEpostadresse()) && isEmpty(dki.getMobiltelefonnummer())) {
				return logAndReturn(PRINT, "Epostadresse og mobiltelefon - feltene er tomme");
			}

			SikkerhetsnivaaTo sikkerhetsnivaaTo = sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(dokDistKanalRequest.getMottakerId());
			if (sikkerhetsnivaaTo == null) {
				return logAndReturn(PRINT, "Paaloggingsnivaa ikke tilgjengelig");
			}

			//DokumentTypeId brukt for aarsoppgave skal ikke gjøre sjekk på om brukerId og mottakerId er ulik
			if (!isDokumentTypeIdUsedForAarsoppgave(dokDistKanalRequest.getDokumentTypeId()) && !dokDistKanalRequest.getMottakerId()
					.equals(dokDistKanalRequest.getBrukerId())) {
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

	private boolean isDokumentTypeIdUsedForAarsoppgave(String dokumentTypeId) {
		return ((AARSOPPGAVE_DOKUMENTTYPEID_1).equals(dokumentTypeId) || (AARSOPPGAVE_DOKUMENTTYPEID_2).equals(dokumentTypeId));
	}

	private DokDistKanalResponse logAndReturn(DistribusjonKanalCode kanalKode, String reason) {
		Counter.builder("dok_request_counter")
				.tag("process", BESTEM_DISTRIBUSJON_KANAL)
				.tag("type", "velgKanal")
				.tag("consumer_name", "ukjent")
				.tag("event", kanalKode.name())
				.register(registry).increment();

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
		if (isEmpty(value)) {
			throw new UgyldingInputFunctionalException(format("Ugyldig input: Feltet %s kan ikke være null eller tomt. Fikk %s=%s", fieldName, fieldName, value));
		}
	}

	private static void assertNotNull(String fieldName, Boolean value) {
		if (value == null) {
			throw new UgyldingInputFunctionalException(format("Ugyldig input: Feltet %s kan ikke være null. Fikk %s=%s", fieldName, fieldName, value));
		}
	}

}