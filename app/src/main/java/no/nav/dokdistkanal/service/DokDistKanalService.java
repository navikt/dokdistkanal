package no.nav.dokdistkanal.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.common.DistribusjonKanalCode;
import no.nav.dokdistkanal.common.DokDistKanalRequest;
import no.nav.dokdistkanal.common.DokDistKanalResponse;
import no.nav.dokdistkanal.consumer.altinn.serviceowner.AltinnServiceOwnerConsumer;
import no.nav.dokdistkanal.consumer.altinn.serviceowner.ValidateRecipientResponse;
import no.nav.dokdistkanal.consumer.dki.DigitalKontaktinformasjon;
import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinformasjonTo;
import no.nav.dokdistkanal.consumer.dokmet.DokumentTypeInfoConsumer;
import no.nav.dokdistkanal.consumer.dokmet.DokumentTypeInfoTo;
import no.nav.dokdistkanal.consumer.pdl.HentPersoninfo;
import no.nav.dokdistkanal.consumer.pdl.PdlGraphQLConsumer;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.SikkerhetsnivaaConsumer;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.to.SikkerhetsnivaaTo;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

import static java.lang.String.format;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.DITT_NAV;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.DPVT;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.INGEN_DISTRIBUSJON;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.LOKAL_PRINT;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.PRINT;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.SDP;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.TRYGDERETTEN;
import static no.nav.dokdistkanal.common.MottakerTypeCode.PERSON;
import static no.nav.dokdistkanal.rest.bestemkanal.DokDistKanalRestController.BESTEM_DISTRIBUSJON_KANAL;
import static no.nav.dokdistkanal.service.DokdistkanalValidator.consumerId;
import static no.nav.dokdistkanal.service.DokdistkanalValidator.erGyldigAltinnNotifikasjonMottaker;
import static no.nav.dokdistkanal.service.DokdistkanalValidator.isDokumentTypeIdUsedForAarsoppgave;
import static no.nav.dokdistkanal.service.DokdistkanalValidator.isFolkeregisterident;
import static no.nav.dokdistkanal.service.DokdistkanalValidator.isOrgNummerWithInfotrygdDokumentTypeId;
import static no.nav.dokdistkanal.service.DokdistkanalValidator.isValidDPVTOrgNummer;
import static no.nav.dokdistkanal.service.DokdistkanalValidator.validateInput;
import static org.apache.commons.lang3.StringUtils.isEmpty;

@Slf4j
@Component
public class DokDistKanalService {

	private static final String LOGGET_PAA = "logget på med nivaa4 de siste 18 mnd";
	public static final Set<String> BEGRENSET_INNSYN_TEMA = Set.of("FAR", "KTR", "KTA", "ARP", "ARS");

	private final DokumentTypeInfoConsumer dokumentTypeInfoConsumer;
	private final DigitalKontaktinformasjon digitalKontaktinformasjon;
	private final SikkerhetsnivaaConsumer sikkerhetsnivaaConsumer;
	private final MeterRegistry registry;
	private final PdlGraphQLConsumer pdlGraphQLConsumer;
	private final AltinnServiceOwnerConsumer altinnServiceOwnerConsumer;

	DokDistKanalService(DokumentTypeInfoConsumer dokumentTypeInfoConsumer,
						DigitalKontaktinformasjon digitalKontaktinformasjon,
						SikkerhetsnivaaConsumer sikkerhetsnivaaConsumer,
						MeterRegistry registry,
						PdlGraphQLConsumer pdlGraphQLConsumer,
						AltinnServiceOwnerConsumer altinnServiceOwnerConsumer) {
		this.dokumentTypeInfoConsumer = dokumentTypeInfoConsumer;
		this.digitalKontaktinformasjon = digitalKontaktinformasjon;
		this.sikkerhetsnivaaConsumer = sikkerhetsnivaaConsumer;
		this.registry = registry;
		this.pdlGraphQLConsumer = pdlGraphQLConsumer;
		this.altinnServiceOwnerConsumer = altinnServiceOwnerConsumer;
	}

	public DokDistKanalResponse velgKanal(DokDistKanalRequest dokDistKanalRequest) {
		validateInput(dokDistKanalRequest);
		final String tema = dokDistKanalRequest.getTema();

		DokumentTypeInfoTo dokumentTypeInfoTo = dokumentTypeInfoConsumer.hentDokumenttypeInfo(dokDistKanalRequest.getDokumentTypeId());

		if ("INGEN".equals(dokumentTypeInfoTo.getArkivsystem())) {
			return logAndReturn(PRINT, "Skal ikke arkiveres", tema);
		}
		if (LOKAL_PRINT.toString().equals(dokumentTypeInfoTo.getPredefinertDistKanal())) {
			return logAndReturn(LOKAL_PRINT, "Predefinert distribusjonskanal er Lokal Print", tema);
		}
		if (INGEN_DISTRIBUSJON.toString().equals(dokumentTypeInfoTo.getPredefinertDistKanal())) {
			return logAndReturn(INGEN_DISTRIBUSJON, "Predefinert distribusjonskanal er Ingen Distribusjon", tema);
		}
		if (TRYGDERETTEN.toString().equals(dokumentTypeInfoTo.getPredefinertDistKanal())) {
			return logAndReturn(TRYGDERETTEN, "Predefinert distribusjonskanal er Trygderetten", tema);
		}

		if (!PERSON.equals(dokDistKanalRequest.getMottakerType())) {
			return validerOrganisasjonAndVelgKanal(dokDistKanalRequest, tema);
		}

		return validerPersonAndVelgKanal(dokDistKanalRequest, tema, dokumentTypeInfoTo);
	}


	private DokDistKanalResponse validerOrganisasjonAndVelgKanal(DokDistKanalRequest dokDistKanalRequest, String tema) {
		if (isValidDPVTOrgNummer(dokDistKanalRequest)) {
			if (isOrgNummerWithInfotrygdDokumentTypeId(dokDistKanalRequest)) {
				return logAndReturn(PRINT, format("Mottaker er av typen %s med infotrygd dokumentTypeId=%s", dokDistKanalRequest.getMottakerType().name(), dokDistKanalRequest.getDokumentTypeId()), tema);
			}
			ValidateRecipientResponse serviceOwnerValidReciepient = altinnServiceOwnerConsumer.isServiceOwnerValidRecipient(dokDistKanalRequest.getMottakerId());

			if (erGyldigAltinnNotifikasjonMottaker(serviceOwnerValidReciepient)) {
				return logAndReturn(DPVT, format("Mottaker er av typen %s og er en gyldig altinn-serviceowner notifikasjonsmottaker", dokDistKanalRequest.getMottakerType().name()), tema);
			}
		}
		return logAndReturn(PRINT, format("Mottaker er av typen %s", dokDistKanalRequest.getMottakerType().name()), tema);
	}

	public DokDistKanalResponse validerPersonAndVelgKanal(DokDistKanalRequest dokDistKanalRequest, String tema, DokumentTypeInfoTo dokumentTypeInfoTo) {
		HentPersoninfo hentPersoninfo = isFolkeregisterident(dokDistKanalRequest) ? pdlGraphQLConsumer.hentPerson(dokDistKanalRequest.getMottakerId(), tema) : null;

		if (hentPersoninfo == null) {
			return logAndReturn(PRINT, "Finner ikke personen i PDL", tema);
		}

		if (hentPersoninfo.getDoedsdato() != null) {
			return logAndReturn(PRINT, "Personen er død", tema);
		}

		if (hentPersoninfo.getFoedselsdato() == null) {
			return logAndReturn(PRINT, "Personens alder er ukjent", tema);
		}

		if (LocalDate.now().minusYears(18).isBefore(hentPersoninfo.getFoedselsdato())) {
			return logAndReturn(PRINT, "Personen må være minst 18 år gammel for digital kommunikasjon", tema);
		}

		DigitalKontaktinformasjonTo dki = digitalKontaktinformasjon.hentSikkerDigitalPostadresse(dokDistKanalRequest
				.getMottakerId(), true);
		if (dki == null) {
			return logAndReturn(PRINT, "Finner ikke Digital kontaktinformasjon", tema);
		}

		if (dki.isReservasjon()) {
			return logAndReturn(PRINT, "Bruker har reservert seg", tema);
		}
		if (dokumentTypeInfoTo.isVarslingSdp() && isEmpty(dki.getEpostadresse()) && isEmpty(dki.getMobiltelefonnummer())) {
			return logAndReturn(PRINT, "Bruker skal varsles, men verken mobiltelefonnummer eller epostadresse har verdi", tema);
		}

		if (dki.verifyAddressAndCertificate()) {
			if (dokDistKanalRequest.getForsendelseStoerrelse() == null || dokDistKanalRequest.getForsendelseStoerrelse() < 27) {
				return logAndReturn(SDP, "Sertifikat, LeverandørAddresse og BrukerAdresse har verdi.", tema);
			}
			log.info("Forsendelse har forstørre filstørrelse og kan ikke distribuere til DPI");
		}
		if (isEmpty(dki.getEpostadresse()) && isEmpty(dki.getMobiltelefonnummer())) {
			return logAndReturn(PRINT, "Epostadresse og mobiltelefon - feltene er tomme", tema);
		}

		//DokumentTypeId brukt for aarsoppgave skal ikke gjøre sjekk på om brukerId og mottakerId er ulik
		if (!isDokumentTypeIdUsedForAarsoppgave(dokDistKanalRequest.getDokumentTypeId()) && !dokDistKanalRequest.getMottakerId()
				.equals(dokDistKanalRequest.getBrukerId())) {
			return logAndReturn(PRINT, "Bruker og mottaker er forskjellige", tema);
		}

		SikkerhetsnivaaTo sikkerhetsnivaaTo = sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(dokDistKanalRequest.getMottakerId());
		if (sikkerhetsnivaaTo == null) {
			return logAndReturn(PRINT, "Paaloggingsnivaa ikke tilgjengelig", tema);
		}

		if (!dokDistKanalRequest.getErArkivert()) {
			return logAndReturn(PRINT, "Dokumentet er ikke arkivert", tema);
		}

		if (BEGRENSET_INNSYN_TEMA.contains(tema)) {
			return logAndReturn(PRINT, "Tema har begrenset innsyn", tema);
		}

		if (sikkerhetsnivaaTo.isHarLoggetPaaNivaa4()) {
			return logAndReturn(DITT_NAV, "Bruker har " + LOGGET_PAA, tema);
		}

		return logAndReturn(PRINT, "Bruker har ikke " + LOGGET_PAA, tema);
	}

	private DokDistKanalResponse logAndReturn(DistribusjonKanalCode kanalKode, String reason, String tema) {
		Counter.builder("dok_request_counter")
				.tag("process", BESTEM_DISTRIBUSJON_KANAL)
				.tag("type", "velgKanal")
				.tag("consumer_name", "ukjent")
				.tag("event", kanalKode.name())
				.register(registry).increment();

		log.info(format("BestemKanal: Sender melding fra %s (Tema=%s) til %s: %s", consumerId(), tema, kanalKode.name(), reason));
		return DokDistKanalResponse.builder().distribusjonsKanal(kanalKode).build();
	}
}