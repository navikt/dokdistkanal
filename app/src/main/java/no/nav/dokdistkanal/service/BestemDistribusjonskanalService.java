package no.nav.dokdistkanal.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import no.nav.dokdistkanal.consumer.altinn.serviceowner.AltinnServiceOwnerConsumer;
import no.nav.dokdistkanal.consumer.altinn.serviceowner.ValidateRecipientResponse;
import no.nav.dokdistkanal.consumer.brreg.BrregEnhetsregisterConsumer;
import no.nav.dokdistkanal.consumer.brreg.EnhetsRolleResponse;
import no.nav.dokdistkanal.consumer.brreg.HentEnhetResponse;
import no.nav.dokdistkanal.consumer.dki.DigitalKontaktinformasjonConsumer;
import no.nav.dokdistkanal.consumer.dki.to.DigitalKontaktinformasjonTo;
import no.nav.dokdistkanal.consumer.dokmet.DokumentTypeInfoConsumer;
import no.nav.dokdistkanal.consumer.dokmet.DokumentTypeInfoTo;
import no.nav.dokdistkanal.consumer.pdl.PdlGraphQLConsumer;
import no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel;
import no.nav.dokdistkanal.rest.bestemdistribusjonskanal.BestemDistribusjonskanalRequest;
import no.nav.dokdistkanal.rest.bestemdistribusjonskanal.BestemDistribusjonskanalResponse;
import org.springframework.stereotype.Service;

import java.util.Objects;
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
import static no.nav.dokdistkanal.service.DokdistkanalValidator.consumerId;
import static no.nav.dokdistkanal.service.DokdistkanalValidator.erDokumentFraAarsoppgave;
import static no.nav.dokdistkanal.service.DokdistkanalValidator.erDokumentFraInfotrygd;
import static no.nav.dokdistkanal.service.DokdistkanalValidator.erGyldigAltinnNotifikasjonMottaker;
import static no.nav.dokdistkanal.service.DokdistkanalValidator.erIdentitetsnummer;
import static no.nav.dokdistkanal.service.DokdistkanalValidator.erOrganisasjonsnummer;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.util.CollectionUtils.isEmpty;

@Slf4j
@Service
public class BestemDistribusjonskanalService {

	public static final String DEFAULT_DOKUMENTTYPE_ID = "U000001";
	public static final Set<String> TEMA_MED_BEGRENSET_INNSYN = Set.of("FAR", "KTR", "KTA", "ARP", "ARS");
	private static final Set<String> ROLLER_TYPE = Set.of("DAGL", "INNH", "LEDE", "BEST", "DTPR", "DTSO");

	public static final String BESTEM_DISTRIBUSJONSKANAL = "bestemDistribusjonKanal";

	private final DokumentTypeInfoConsumer dokumentTypeInfoConsumer;
	private final DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer;
	private final MeterRegistry registry;
	private final PdlGraphQLConsumer pdlGraphQLConsumer;
	private final AltinnServiceOwnerConsumer altinnServiceOwnerConsumer;

	private final BrregEnhetsregisterConsumer brregEnhetsRegisterConsumer;

	public BestemDistribusjonskanalService(DokumentTypeInfoConsumer dokumentTypeInfoConsumer,
										   DigitalKontaktinformasjonConsumer digitalKontaktinformasjonConsumer,
										   BrregEnhetsregisterConsumer brregEnhetsRegisterConsumer,
										   PdlGraphQLConsumer pdlGraphQLConsumer,
										   AltinnServiceOwnerConsumer altinnServiceOwnerConsumer,
										   MeterRegistry registry) {
		this.dokumentTypeInfoConsumer = dokumentTypeInfoConsumer;
		this.digitalKontaktinformasjonConsumer = digitalKontaktinformasjonConsumer;
		this.pdlGraphQLConsumer = pdlGraphQLConsumer;
		this.altinnServiceOwnerConsumer = altinnServiceOwnerConsumer;
		this.brregEnhetsRegisterConsumer = brregEnhetsRegisterConsumer;
		this.registry = registry;
	}

	public BestemDistribusjonskanalResponse bestemDistribusjonskanal(BestemDistribusjonskanalRequest request) {

		if (isBlank(request.getDokumenttypeId())) {
			request.setDokumenttypeId(DEFAULT_DOKUMENTTYPE_ID);
		}

		var dokumenttypeInfo = dokumentTypeInfoConsumer.hentDokumenttypeInfo(request.getDokumenttypeId());

		if (dokumenttypeInfo != null) {
			var predefinertDistribusjonskanal = predefinertDistribusjonskanal(request, dokumenttypeInfo);
			if (predefinertDistribusjonskanal != null) {
				return predefinertDistribusjonskanal;
			}
		}

		return switch (request.getMottakerId().length()) {
			case 9 -> validerOrgNrOgBestemKanal(request);
			case 11 -> validerIdNrOgBestemKanal(request, dokumenttypeInfo);
			default -> createResponse(request, MOTTAKER_ER_IKKE_PERSON_ELLER_ORGANISASJON);
		};
	}

	private BestemDistribusjonskanalResponse validerOrgNrOgBestemKanal(BestemDistribusjonskanalRequest request) {
		if (!erOrganisasjonsnummer(request)) {
			return createResponse(request, MOTTAKER_ER_IKKE_PERSON_ELLER_ORGANISASJON);
		}

		return organisasjon(request);
	}

	private BestemDistribusjonskanalResponse validerIdNrOgBestemKanal(BestemDistribusjonskanalRequest request, DokumentTypeInfoTo dokumenttypeInfo) {
		if (!erIdentitetsnummer(request.getMottakerId())) {
			return createResponse(request, MOTTAKER_ER_IKKE_PERSON_ELLER_ORGANISASJON);
		}

		return person(request, dokumenttypeInfo);
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
		if (erDokumentFraInfotrygd(request.getDokumenttypeId())) {
			return createResponse(request, ORGANISASJON_MED_INFOTRYGD_DOKUMENT);
		}

		var serviceOwnerValidRecipient = altinnServiceOwnerConsumer.isServiceOwnerValidRecipient(request.getMottakerId());
		return erEnhetenGyldigNotifikasjonMottakerOgIkkeKonkursOgHarRolleGruppe(serviceOwnerValidRecipient, request.getMottakerId()) ?
				createResponse(request, ORGANISASJON_MED_ALTINN_INFO) : createResponse(request, ORGANISASJON_UTEN_ALTINN_INFO);
	}

	private BestemDistribusjonskanalResponse person(BestemDistribusjonskanalRequest request, DokumentTypeInfoTo dokumentTypeInfo) {

		var personinfoResultat = evaluerPersoninfo(request);

		if (personinfoResultat != null) {
			return personinfoResultat;
		}

		var digitalKontaktinfo = digitalKontaktinformasjonConsumer.hentSikkerDigitalPostadresse(request.getMottakerId(), true);
		var digitalKontaktinfoResultat = evaluerDigitalKontaktinfo(request, dokumentTypeInfo, digitalKontaktinfo);

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

	private boolean erEnhetenGyldigNotifikasjonMottakerOgIkkeKonkursOgHarRolleGruppe(ValidateRecipientResponse validateRecipientResponse, String orgNummer) {
		if (erGyldigAltinnNotifikasjonMottaker(validateRecipientResponse)) {
			boolean erKonkurs = erEnhetenKonkurs(orgNummer);
			if (!erKonkurs) {
				return isContainsValidRolleType(orgNummer);
			}
		}
		return false;
	}

	private BestemDistribusjonskanalResponse createResponse(BestemDistribusjonskanalRequest request, BestemDistribusjonskanalRegel regel) {
		var kanalKode = regel.distribusjonKanal.name();

		Counter.builder("dok_request_counter")
				.tag("process", BESTEM_DISTRIBUSJONSKANAL)
				.tag("type", "velgKanal")
				.tag("consumer_name", "ukjent")
				.tag("event", kanalKode)
				.register(registry).increment();

		log.info("bestemDistribusjonskanal: Sender melding fra {} (Tema={}) til {}: {}", consumerId(), request.getTema(), kanalKode, regel.begrunnelse);

		return new BestemDistribusjonskanalResponse(regel);
	}

	private boolean erEnhetenKonkurs(String orgNummer) {
		HentEnhetResponse hentEnhetResponse = brregEnhetsRegisterConsumer.hentEnhet(orgNummer);
		return hentEnhetResponse == null || hentEnhetResponse.konkurs();
	}

	private boolean isContainsValidRolleType(String orgNummer) {

		EnhetsRolleResponse response = brregEnhetsRegisterConsumer.hentEnhetsRollegrupper(orgNummer);

		if (response == null || isEmpty(response.rollegrupper())) {
			return false;
		}

		return response.rollegrupper().stream()
				.flatMap(roller -> roller.roller().stream())
				.filter(Objects::nonNull)
				.filter(rolle -> !erPersonDoedOrIkkeFodselsdato(rolle.person()))
				.anyMatch(r -> ROLLER_TYPE.contains(r.type().kode()));
	}

	private boolean erPersonDoedOrIkkeFodselsdato(EnhetsRolleResponse.Person person) {
		if (person == null) {
			return false;
		}
		return person.erDoed() || person.fodselsdato() == null;
	}
}
