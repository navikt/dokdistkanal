package no.nav.dokdistkanal.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.consumer.altinn.serviceowner.AltinnServiceOwnerConsumer;
import no.nav.dokdistkanal.consumer.dki.DigitalKontaktinformasjon;
import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinformasjonTo;
import no.nav.dokdistkanal.consumer.dokmet.DokumentTypeInfoConsumer;
import no.nav.dokdistkanal.consumer.dokmet.DokumentTypeInfoTo;
import no.nav.dokdistkanal.consumer.pdl.PdlGraphQLConsumer;
import no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel;
import no.nav.dokdistkanal.rest.bestemdistribusjonskanal.BestemDistribusjonskanalRequest;
import no.nav.dokdistkanal.rest.bestemdistribusjonskanal.BestemDistribusjonskanalResponse;
import org.springframework.stereotype.Service;

import java.util.Set;

import static no.nav.dokdistkanal.common.DistribusjonKanalCode.INGEN_DISTRIBUSJON;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.LOKAL_PRINT;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.TRYGDERETTEN;
import static no.nav.dokdistkanal.constants.DomainConstants.DPI_MAX_FORSENDELSE_STOERRELSE_I_MEGABYTES;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.BRUKER_ER_RESERVERT;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.BRUKER_HAR_GYLDIG_EPOST_ELLER_MOBILNUMMER;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.BRUKER_HAR_GYLDIG_SDP_ADRESSE;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.BRUKER_MANGLER_EPOST_OG_TELEFON;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.BRUKER_OG_MOTTAKER_ER_FORSKJELLIG;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.BRUKER_SDP_MANGLER_VARSELINFO;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.DOKUMENT_ER_IKKE_ARKIVERT;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.FINNER_IKKE_DIGITAL_KONTAKTINFORMASJON;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.MOTTAKER_ER_IKKE_PERSON_ELLER_ORGANISASJON;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.ORGANISASJON_MED_ALTINN_INFO;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.ORGANISASJON_MED_INFOTRYGD_DOKUMENT;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.ORGANISASJON_UTEN_ALTINN_INFO;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.PERSON_ER_DOED;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.PERSON_ER_IKKE_I_PDL;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.PERSON_ER_UNDER_18;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.PERSON_HAR_UKJENT_ALDER;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.PERSON_STANDARD_PRINT;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.PREDEFINERT_INGEN_DISTRIBUSJON;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.PREDEFINERT_LOKAL_PRINT;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.PREDEFINERT_TRYGDERETTEN;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.SKAL_IKKE_ARKIVERES;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.TEMA_HAR_BEGRENSET_INNSYN;
import static no.nav.dokdistkanal.rest.bestemkanal.DokDistKanalRestController.BESTEM_DISTRIBUSJON_KANAL;
import static no.nav.dokdistkanal.service.DokdistkanalValidator.consumerId;
import static no.nav.dokdistkanal.service.DokdistkanalValidator.erGyldigAltinnNotifikasjonMottaker;
import static no.nav.dokdistkanal.service.DokdistkanalValidator.isDokumentTypeIdUsedForAarsoppgave;
import static no.nav.dokdistkanal.service.DokdistkanalValidator.isFolkeregisterident;
import static no.nav.dokdistkanal.service.DokdistkanalValidator.isOrgNummerWithInfotrygdDokumentTypeId;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Service
public class BestemDistribusjonskanalService {

	public static final Set<String> TEMA_MED_BEGRENSET_INNSYN = Set.of("FAR", "KTR", "KTA", "ARP", "ARS");

	private final DokumentTypeInfoConsumer dokumentTypeInfoConsumer;
	private final DigitalKontaktinformasjon digitalKontaktinformasjon;
	private final MeterRegistry registry;
	private final PdlGraphQLConsumer pdlGraphQLConsumer;
	private final AltinnServiceOwnerConsumer altinnServiceOwnerConsumer;

	public BestemDistribusjonskanalService(DokumentTypeInfoConsumer dokumentTypeInfoConsumer,
										   DigitalKontaktinformasjon digitalKontaktinformasjon,
										   MeterRegistry registry,
										   PdlGraphQLConsumer pdlGraphQLConsumer,
										   AltinnServiceOwnerConsumer altinnServiceOwnerConsumer) {
		this.dokumentTypeInfoConsumer = dokumentTypeInfoConsumer;
		this.digitalKontaktinformasjon = digitalKontaktinformasjon;
		this.registry = registry;
		this.pdlGraphQLConsumer = pdlGraphQLConsumer;
		this.altinnServiceOwnerConsumer = altinnServiceOwnerConsumer;
	}


	public BestemDistribusjonskanalResponse bestemDistribusjonskanal(BestemDistribusjonskanalRequest request) {

		var dokumenttypeInfo = isBlank(request.getDokumenttypeId()) ? null : dokumentTypeInfoConsumer.hentDokumenttypeInfo(request.getDokumenttypeId());

		if (dokumenttypeInfo != null) {
			var predefinertDistribusjonskanal = predefinertDistribusjonskanal(request, dokumenttypeInfo);
			if (predefinertDistribusjonskanal != null) {
				return predefinertDistribusjonskanal;
			}
		}

		return switch (request.getMottakerId().length()) {
			case 9 -> organisasjon(request);
			case 11 -> person(request, dokumenttypeInfo);
			default -> createResponse(request, MOTTAKER_ER_IKKE_PERSON_ELLER_ORGANISASJON);
		};
	}

	private BestemDistribusjonskanalResponse predefinertDistribusjonskanal(BestemDistribusjonskanalRequest request, DokumentTypeInfoTo dokumenttypeInfo) {
		if ("INGEN".equals(dokumenttypeInfo.getArkivsystem())) {
			return createResponse(request, SKAL_IKKE_ARKIVERES);
		}
		if (LOKAL_PRINT.toString().equals(dokumenttypeInfo.getPredefinertDistKanal())) {
			return createResponse(request, PREDEFINERT_LOKAL_PRINT);
		}
		if (INGEN_DISTRIBUSJON.toString().equals(dokumenttypeInfo.getPredefinertDistKanal())) {
			return createResponse(request, PREDEFINERT_INGEN_DISTRIBUSJON);
		}
		if (TRYGDERETTEN.toString().equals(dokumenttypeInfo.getPredefinertDistKanal())) {
			return createResponse(request, PREDEFINERT_TRYGDERETTEN);
		}
		return null;
	}

	private BestemDistribusjonskanalResponse organisasjon(BestemDistribusjonskanalRequest request) {
		if (isOrgNummerWithInfotrygdDokumentTypeId(request.getDokumenttypeId())) {
			return createResponse(request, ORGANISASJON_MED_INFOTRYGD_DOKUMENT);
		}

		var serviceOwnerValidRecipient = altinnServiceOwnerConsumer.isServiceOwnerValidRecipient(request.getMottakerId());
		if (erGyldigAltinnNotifikasjonMottaker(serviceOwnerValidRecipient)) {
			return createResponse(request, ORGANISASJON_MED_ALTINN_INFO);
		} else {
			return createResponse(request, ORGANISASJON_UTEN_ALTINN_INFO);
		}
	}

	private BestemDistribusjonskanalResponse person(BestemDistribusjonskanalRequest request, DokumentTypeInfoTo dokumentTypeInfo) {

		var personinfoResultat = evaluerPersoninfo(request);

		if (personinfoResultat != null) {
			return personinfoResultat;
		}

		var digitalKontaktinfo = digitalKontaktinformasjon.hentSikkerDigitalPostadresse(request.getMottakerId(), true);
		var digitalKontaktinfoResultat = evaluerDigitalKontaktinfo(request, dokumentTypeInfo, digitalKontaktinfo);

		if (digitalKontaktinfoResultat != null) {
			return digitalKontaktinfoResultat;
		}

		var dokumentTypeErIkkeAarsoppgave = request.getDokumenttypeId() == null || !isDokumentTypeIdUsedForAarsoppgave(request.getDokumenttypeId());
		var mottarOgBrukerErForskjellig = !request.getMottakerId().equals(request.getBrukerId());

		//DokumentTypeId brukt for aarsoppgave skal ikke gjøre sjekk på om brukerId og mottakerId er ulik
		if (dokumentTypeErIkkeAarsoppgave && mottarOgBrukerErForskjellig) {
			return createResponse(request, BRUKER_OG_MOTTAKER_ER_FORSKJELLIG);
		}

		if (!request.isErArkivert()) {
			return createResponse(request, DOKUMENT_ER_IKKE_ARKIVERT);
		}

		if (TEMA_MED_BEGRENSET_INNSYN.contains(request.getTema())) {
			return createResponse(request, TEMA_HAR_BEGRENSET_INNSYN);
		}

		if (digitalKontaktinfo.harEpostEllerMobilnummer()) {
			return createResponse(request, BRUKER_HAR_GYLDIG_EPOST_ELLER_MOBILNUMMER);
		}

		return createResponse(request, PERSON_STANDARD_PRINT);
	}

	private BestemDistribusjonskanalResponse evaluerPersoninfo(BestemDistribusjonskanalRequest request) {
		var personinfo = isFolkeregisterident(request.getMottakerId()) ? pdlGraphQLConsumer.hentPerson(request.getMottakerId()) : null;

		if (personinfo == null) {
			return createResponse(request, PERSON_ER_IKKE_I_PDL);
		}
		if (personinfo.erDoed()) {
			return createResponse(request, PERSON_ER_DOED);
		}
		if (personinfo.harUkjentAlder()) {
			return createResponse(request, PERSON_HAR_UKJENT_ALDER);
		}
		if (personinfo.erUnderAtten()) {
			return createResponse(request, PERSON_ER_UNDER_18);
		}
		return null;
	}

	private BestemDistribusjonskanalResponse evaluerDigitalKontaktinfo(BestemDistribusjonskanalRequest request,
																	   DokumentTypeInfoTo dokumentTypeInfo,
																	   DigitalKontaktinformasjonTo digitalKontaktinfo) {

		if (digitalKontaktinfo == null) {
			return createResponse(request, FINNER_IKKE_DIGITAL_KONTAKTINFORMASJON);
		}
		if (digitalKontaktinfo.isReservasjon()) {
			return createResponse(request, BRUKER_ER_RESERVERT);
		}
		if (dokumentTypeInfo != null &&
				dokumentTypeInfo.isVarslingSdp() &&
				!digitalKontaktinfo.harEpostEllerMobilnummer()) {

			return createResponse(request, BRUKER_SDP_MANGLER_VARSELINFO);
		}

		if (digitalKontaktinfo.verifyAddressAndCertificate()) {
			if (request.getForsendelseStoerrelse() == null || request.getForsendelseStoerrelse() < DPI_MAX_FORSENDELSE_STOERRELSE_I_MEGABYTES) {
				return createResponse(request, BRUKER_HAR_GYLDIG_SDP_ADRESSE);
			}
			log.info("Forsendelse er større enn {}MB og kan ikke distribueres til DPI. forsendelseStoerrelse={}MB",
					DPI_MAX_FORSENDELSE_STOERRELSE_I_MEGABYTES, request.getForsendelseStoerrelse());
		}
		if (!digitalKontaktinfo.harEpostEllerMobilnummer()) {
			return createResponse(request, BRUKER_MANGLER_EPOST_OG_TELEFON);
		}
		return null;
	}

	private BestemDistribusjonskanalResponse createResponse(BestemDistribusjonskanalRequest request, BestemDistribusjonskanalRegel regel) {
		var kanalKode = regel.distribusjonKanal.name();

		Counter.builder("dok_request_counter")
				.tag("process", BESTEM_DISTRIBUSJON_KANAL)
				.tag("type", "velgKanal")
				.tag("consumer_name", "ukjent")
				.tag("event", kanalKode)
				.register(registry).increment();

		log.info("bestemDistribusjonskanal: Sender melding fra {} (Tema={}) til {}: {}", consumerId(), request.getTema(), kanalKode, regel.begrunnelse);

		return new BestemDistribusjonskanalResponse(regel);
	}
}
