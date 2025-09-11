package no.nav.dokdistkanal.service;

import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.consumer.dki.DigitalKontaktinformasjonConsumer;
import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinformasjonTo;
import no.nav.dokdistkanal.consumer.dokmet.DokmetConsumer;
import no.nav.dokdistkanal.consumer.dokmet.DokumentTypeKanalInfo;
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
import static no.nav.dokdistkanal.constants.DomainConstants.DPI_MAX_ANTALL_DOKUMENTER_FORSENDELSE;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.BRUKER_ER_RESERVERT;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.BRUKER_HAR_GYLDIG_EPOST_ELLER_MOBILNUMMER;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.BRUKER_HAR_GYLDIG_SDP_ADRESSE;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.BRUKER_MANGLER_EPOST_OG_TELEFON;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.BRUKER_OG_MOTTAKER_ER_FORSKJELLIG;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.BRUKER_SDP_MANGLER_VARSELINFO;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.DOKUMENT_ER_IKKE_ARKIVERT;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.FINNER_IKKE_DIGITAL_KONTAKTINFORMASJON;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.MOTTAKER_ER_IKKE_PERSON_ELLER_ORGANISASJON;
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
import static no.nav.dokdistkanal.service.DokdistkanalValidator.consumerId;
import static no.nav.dokdistkanal.service.DokdistkanalValidator.erDokumentFraAarsoppgave;
import static no.nav.dokdistkanal.service.DokdistkanalValidator.erIdentitetsnummer;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Service
public class BestemDistribusjonskanalService {

	public static final String DEFAULT_DOKUMENTTYPE_ID = "U000001";
	public static final Set<String> TEMA_MED_BEGRENSET_INNSYN = Set.of("FAR", "KTR", "KTA", "ARP", "ARS");

	private final DokmetConsumer dokmetConsumer;
	private final DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer;
	private final PdlGraphQLConsumer pdlGraphQLConsumer;
	private final OrganisasjonDistribusjonKanalService organisasjonDistribusjonKanalService;


	public BestemDistribusjonskanalService(DokmetConsumer dokmetConsumer,
										   DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer,
										   PdlGraphQLConsumer pdlGraphQLConsumer,
										   OrganisasjonDistribusjonKanalService organisasjonDistribusjonKanalService) {
		this.dokmetConsumer = dokmetConsumer;
		this.digitalKontaktinformasjonConsumer = digitalKontaktinformasjonConsumer;
		this.pdlGraphQLConsumer = pdlGraphQLConsumer;
		this.organisasjonDistribusjonKanalService = organisasjonDistribusjonKanalService;
	}

	public BestemDistribusjonskanalResponse bestemDistribusjonskanal(BestemDistribusjonskanalRequest request) {

		if (isBlank(request.getDokumenttypeId())) {
			request.setDokumenttypeId(DEFAULT_DOKUMENTTYPE_ID);
		}

		var dokumenttypeInfo = dokmetConsumer.hentDokumenttypeInfo(request.getDokumenttypeId());

		if (dokumenttypeInfo != null) {
			var predefinertDistribusjonskanal = predefinertDistribusjonskanal(request, dokumenttypeInfo);
			if (predefinertDistribusjonskanal != null) {
				return predefinertDistribusjonskanal;
			}
		}

		return switch (request.getMottakerId().length()) {
			case 9 -> organisasjonDistribusjonKanalService.validerOrgNrOgBestemKanal(request);
			case 11 -> validerIdNrOgBestemKanal(request, dokumenttypeInfo);
			default -> createResponse(request, MOTTAKER_ER_IKKE_PERSON_ELLER_ORGANISASJON);
		};
	}


	private BestemDistribusjonskanalResponse validerIdNrOgBestemKanal(BestemDistribusjonskanalRequest request, DokumentTypeKanalInfo dokumentTypeKanalInfo) {
		if (!erIdentitetsnummer(request.getMottakerId())) {
			return createResponse(request, MOTTAKER_ER_IKKE_PERSON_ELLER_ORGANISASJON);
		}

		return person(request, dokumentTypeKanalInfo);
	}

	private BestemDistribusjonskanalResponse predefinertDistribusjonskanal(BestemDistribusjonskanalRequest request, DokumentTypeKanalInfo dokumentTypeKanalInfo) {
		if ("INGEN".equals(dokumentTypeKanalInfo.getArkivsystem())) {
			return createResponse(request, SKAL_IKKE_ARKIVERES);
		}
		if (LOKAL_PRINT.toString().equals(dokumentTypeKanalInfo.getPredefinertDistKanal())) {
			return createResponse(request, PREDEFINERT_LOKAL_PRINT);
		}
		if (INGEN_DISTRIBUSJON.toString().equals(dokumentTypeKanalInfo.getPredefinertDistKanal())) {
			return createResponse(request, PREDEFINERT_INGEN_DISTRIBUSJON);
		}
		if (TRYGDERETTEN.toString().equals(dokumentTypeKanalInfo.getPredefinertDistKanal())) {
			return createResponse(request, PREDEFINERT_TRYGDERETTEN);
		}
		return null;
	}

	private BestemDistribusjonskanalResponse person(BestemDistribusjonskanalRequest request, DokumentTypeKanalInfo dokumentTypeKanalInfo) {

		var personinfoResultat = evaluerPersoninfo(request);

		if (personinfoResultat != null) {
			return personinfoResultat;
		}

		var digitalKontaktinfo = digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(request.getMottakerId(), true);
		var digitalKontaktinfoResultat = evaluerDigitalKontaktinfo(request, dokumentTypeKanalInfo, digitalKontaktinfo);

		if (digitalKontaktinfoResultat != null) {
			return digitalKontaktinfoResultat;
		}

		var dokumentTypeErIkkeAarsoppgave = request.getDokumenttypeId() == null || !erDokumentFraAarsoppgave(request.getDokumenttypeId());
		var mottakerOgBrukerErForskjellig = !request.getMottakerId().equals(request.getBrukerId());

		//DokumentTypeId brukt for aarsoppgave skal ikke gjøre sjekk på om brukerId og mottakerId er ulik
		if (dokumentTypeErIkkeAarsoppgave && mottakerOgBrukerErForskjellig) {
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
		var personinfo = pdlGraphQLConsumer.hentPerson(request.getMottakerId());

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
																	   DokumentTypeKanalInfo dokumentTypeKanalInfo,
																	   DigitalKontaktinformasjonTo digitalKontaktinfo) {

		if (digitalKontaktinfo == null) {
			return createResponse(request, FINNER_IKKE_DIGITAL_KONTAKTINFORMASJON);
		}
		if (digitalKontaktinfo.isReservasjon()) {
			return createResponse(request, BRUKER_ER_RESERVERT);
		}
		if (dokumentTypeKanalInfo != null &&
				dokumentTypeKanalInfo.isVarslingSdp() &&
				!digitalKontaktinfo.harEpostEllerMobilnummer()) {

			return createResponse(request, BRUKER_SDP_MANGLER_VARSELINFO);
		}

		if (digitalKontaktinfo.verifyAddressAndCertificate()) {
			if (requestStoerrelseGyldigForSDP(request) && requestAntallDokumenterGyldigForSDP(request)) {
				return createResponse(request, BRUKER_HAR_GYLDIG_SDP_ADRESSE);
			} else if (!requestStoerrelseGyldigForSDP(request)) {
				log.info("Forsendelse er større enn {}MB og kan ikke distribueres til DPI. forsendelseStoerrelse={}MB",
						DPI_MAX_FORSENDELSE_STOERRELSE_I_MEGABYTES, request.getForsendelseStoerrelse());
			} else {
				log.info("Forsendelse består av flere dokumenter enn {} og kan ikke distribueres til DPI. antallDokumenter={}",
						DPI_MAX_ANTALL_DOKUMENTER_FORSENDELSE, request.getAntallDokumenter());
			}
		}
		if (!digitalKontaktinfo.harEpostEllerMobilnummer()) {
			return createResponse(request, BRUKER_MANGLER_EPOST_OG_TELEFON);
		}
		return null;
	}

	public static BestemDistribusjonskanalResponse createResponse(BestemDistribusjonskanalRequest request, BestemDistribusjonskanalRegel regel) {
		var kanalKode = regel.distribusjonKanal.name();

		log.info("bestemDistribusjonskanal: Sender melding fra {} (Tema={}) til {}: {}", consumerId(), request.getTema(), kanalKode, regel.begrunnelse);

		return new BestemDistribusjonskanalResponse(regel);
	}

	private static boolean requestStoerrelseGyldigForSDP(BestemDistribusjonskanalRequest request) {
		return request.getForsendelseStoerrelse() == null || request.getForsendelseStoerrelse() < DPI_MAX_FORSENDELSE_STOERRELSE_I_MEGABYTES;
	}

	private static boolean requestAntallDokumenterGyldigForSDP(BestemDistribusjonskanalRequest request) {
		return request.getAntallDokumenter() == null || request.getAntallDokumenter() <= DPI_MAX_ANTALL_DOKUMENTER_FORSENDELSE;
	}
}
