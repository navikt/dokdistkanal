package no.nav.dokdistkanal.itest;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import no.nav.dokdistkanal.common.DistribusjonKanalCode;
import no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel;
import no.nav.dokdistkanal.rest.bestemdistribusjonskanal.BestemDistribusjonskanalRequest;
import no.nav.dokdistkanal.rest.bestemdistribusjonskanal.BestemDistribusjonskanalResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import reactor.core.publisher.Flux;

import java.util.function.Consumer;
import java.util.stream.Stream;

import static no.nav.dokdistkanal.common.DistribusjonKanalCode.DITT_NAV;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.DPO;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.DPVT;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.INGEN_DISTRIBUSJON;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.LOKAL_PRINT;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.PRINT;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.SDP;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.TRYGDERETTEN;
import static no.nav.dokdistkanal.constants.DomainConstants.DPI_MAX_ANTALL_DOKUMENTER_FORSENDELSE;
import static no.nav.dokdistkanal.constants.DomainConstants.DPI_MAX_FORSENDELSE_STOERRELSE_I_MEGABYTES;
import static no.nav.dokdistkanal.constants.NavHeaders.NAV_CONSUMER_ID;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.MOTTAKER_ER_IKKE_PERSON_ELLER_ORGANISASJON;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.ORGANISASJON_ER_KONKURS;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.ORGANISASJON_ER_SLETTET;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.ORGANISASJON_MANGLER_NODVENDIG_ROLLER;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.ORGANISASJON_MED_ALTINN_INFO;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.ORGANISASJON_MED_INFOTRYGD_DOKUMENT;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.ORGANISASJON_MED_SERVICE_REGISTRY_INFO;
import static no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel.ORGANISASJON_UTEN_ALTINN_INFO;
import static no.nav.dokdistkanal.service.OrganisasjonDistribusjonKanalService.DPO_AVTALEMELDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/*
 * Se https://confluence.adeo.no/pages/viewpage.action?pageId=294148459 for funksjonelle behandlingsregler
 */
public class BestemDistribusjonskanalIT extends AbstractIT {

	@BeforeEach
	public void setUp() {
		clearCachene();
		stubMaskinporten();
		stubAzure();
		stubAltinn();
		resetCircuitBreakers();
	}

	@Test
	void skalSettDokumenttypeIdHvisNullOgReturnereBestemmeDistribusjonskanal() {
		stubDokmet();
		stubPdl();
		stubDigdirKrrProxy();

		BestemDistribusjonskanalResponse response = webTestClient.post()
				.uri(BESTEM_DISTRIBUSJONSKANAL_URL)
				.headers(headers())
				.bodyValue(bestemDistribusjonskanalMedNullDokumenttypeId())
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody(BestemDistribusjonskanalResponse.class)
				.returnResult().getResponseBody();

		assertThat(response).isNotNull()
				.satisfies(it -> {
					assertThat(it.distribusjonskanal()).isEqualTo(SDP);
				});

	}

	@Test
	void skalBestemmeDistribusjonskanal() {
		stubDokmet();
		stubPdl();
		stubDigdirKrrProxy();

		webTestClient.post()
				.uri(BESTEM_DISTRIBUSJONSKANAL_URL)
				.headers(headers())
				.bodyValue(gyldigBestemDistribusjonskanalRequest())
				.exchange()
				.expectStatus()
				.isOk();
	}

	/*
	 * Her testes følgende regler:
	 * 1: Skal dokumentet arkiveres? Hvis nei -> PRINT
	 * 2: Er predefinert distribusjonskanal LOKAL_PRINT? Hvis ja -> LOKAL_PRINT
	 * 3: Er predefinert distribusjonskanal INGEN_DISTRIBUSJON? Hvis ja -> INGEN_DISTRIBUSJON
	 * 4: redefinert distribusjonskanal TRYGDERETTEN? Hvis ja -> TRYGDERETTEN
	 */
	@ParameterizedTest
	@MethodSource
	void skalReturnerePredefinertDistribusjonskanal(DistribusjonKanalCode distribusjonKanal, BestemDistribusjonskanalRegel regel, String stubFile) {
		stubDokmet(stubFile);

		var response = webTestClient.post()
				.uri(BESTEM_DISTRIBUSJONSKANAL_URL)
				.headers(headers())
				.bodyValue(gyldigBestemDistribusjonskanalRequest())
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody(BestemDistribusjonskanalResponse.class)
				.returnResult()
				.getResponseBody();

		assertThat(response)
				.isNotNull()
				.satisfies(it -> {
					assertThat(it.distribusjonskanal()).isEqualTo(distribusjonKanal);
					assertThat(it.regel()).isEqualTo(regel.name());
					assertThat(it.regelBegrunnelse()).isEqualTo(regel.begrunnelse);
				});
	}

	private static Stream<Arguments> skalReturnerePredefinertDistribusjonskanal() {
		return Stream.of(
				Arguments.of(PRINT, BestemDistribusjonskanalRegel.SKAL_IKKE_ARKIVERES, "dokmet/response_ingen_arkivsystem.json"),
				Arguments.of(LOKAL_PRINT, BestemDistribusjonskanalRegel.PREDEFINERT_LOKAL_PRINT, "dokmet/response_predefinert_lokal_print.json"),
				Arguments.of(INGEN_DISTRIBUSJON, BestemDistribusjonskanalRegel.PREDEFINERT_INGEN_DISTRIBUSJON, "dokmet/response_predefinert_ingen_distribusjon.json"),
				Arguments.of(TRYGDERETTEN, BestemDistribusjonskanalRegel.PREDEFINERT_TRYGDERETTEN, "dokmet/response_predefinert_trygderetten.json")
		);
	}

	/*
	 * Her testes følgende regler for mottakertype ORGANISASJON:
	 * 5: Er dokument produsert i Infotrygd? Hvis ja -> PRINT
	 * 6: Har requesten forsendelseMetadataType DPO_AVTALEMELDING,  har mottaker info fra service registry? Hvis ja -> DPO
	 * 7: Har org. varslingsinformasjon i Altinn, er ikke konkurs eller slettet, og har enhets grupperoller? Hvis ja -> DPVT
	 * 7.1: Mangler org. varslingsinformasjon for DPV? Hvis ja -> PRINT
	 * 7.2: Har org. varslingsinformasjon i Altinn, men er konkurs? Hvis ja -> PRINT
	 * 7.3: Har org. varslingsinformasjon i Altinn, er ikke konkurs, men er slettet? Hvis ja -> PRINT
	 * 7.4: Har org. varslingsinformasjon i Altinn, er ikke konkurs eller slettet, men registert person er død eller har ingen fødselsdato? Hvis ja -> PRINT
	 */
	@ParameterizedTest
	@MethodSource
	void skalReturnereForOrganisasjon(DistribusjonKanalCode distribusjonKanal, String forsendelseMetadataType, HttpStatus registryStatus, BestemDistribusjonskanalRegel regel,
									  String mottakerId, String dokumentTypeId, String hentEnhetPath, String grupperollerPath) {

		stubDokmet();
		stubDigdirKrrProxy();
		stubGetServiceRegistry(registryStatus);
		stubAltinn();
		stubEnhetsregisteret(OK, hentEnhetPath, mottakerId);
		stubUnderenhetsregisteret(NOT_FOUND, "enhetsregisteret/underenhet_response.json", mottakerId);
		stubEnhetsGruppeRoller(grupperollerPath, mottakerId);

		var request = bestemDistribusjonskanalRequestMedMetadataType(forsendelseMetadataType);
		request.setMottakerId(mottakerId);
		request.setDokumenttypeId(dokumentTypeId);

		var response = webTestClient.post()
				.uri(BESTEM_DISTRIBUSJONSKANAL_URL)
				.headers(headers())
				.bodyValue(request)
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody(BestemDistribusjonskanalResponse.class)
				.returnResult()
				.getResponseBody();

		assertThat(response)
				.isNotNull()
				.satisfies(it -> {
					assertThat(it.distribusjonskanal()).isEqualTo(distribusjonKanal);
					assertThat(it.regel()).isEqualTo(regel.name());
					assertThat(it.regelBegrunnelse()).isEqualTo(regel.begrunnelse);
				});
	}

	private static Stream<Arguments> skalReturnereForOrganisasjon() {
		return Stream.of(
				Arguments.of(PRINT, null, OK, ORGANISASJON_MED_INFOTRYGD_DOKUMENT, "974761076", "000044", null, null),
				Arguments.of(DPO, DPO_AVTALEMELDING, OK, ORGANISASJON_MED_SERVICE_REGISTRY_INFO, "974761076", "000000", HENT_ENHET_OK_PATH, null),
				Arguments.of(DPVT, DPO_AVTALEMELDING, BAD_REQUEST, ORGANISASJON_MED_ALTINN_INFO, "974761076", "000000", HENT_ENHET_OK_PATH, GRUPPEROLLER_OK_PATH),
				Arguments.of(DPVT, null, OK, ORGANISASJON_MED_ALTINN_INFO, "974761076", "000000", HENT_ENHET_OK_PATH, GRUPPEROLLER_OK_PATH),
				Arguments.of(PRINT, null, OK, ORGANISASJON_UTEN_ALTINN_INFO, "889640782", "000000", null, null),
				Arguments.of(PRINT, null, OK, ORGANISASJON_ER_KONKURS, "974761076", "000000", KONKURS_ENHET_PATH, null),
				Arguments.of(PRINT, null, OK, ORGANISASJON_ER_SLETTET, "974761076", "000000", SLETTET_ENHET_PATH, null),
				Arguments.of(PRINT, null, OK, ORGANISASJON_MANGLER_NODVENDIG_ROLLER, "974761076", "000000", HENT_ENHET_OK_PATH, GRUPPEROLLER_PERSON_ER_DOED_PATH)
		);
	}

	/*
	 * Her testes følgende regler:
	 * 8: Finnes mottaker i PDL? Hvis nei -> PRINT
	 * 9: Er mottakers fødselsdato ikke satt, eller er under 18 år gammel? Hvis ja -> PRINT
	 * 10: Er personen død? Hvis ja -> PRINT
	 */
	@ParameterizedTest
	@MethodSource
	void skalReturnerePrintForPersonMedPersoninfo(BestemDistribusjonskanalRegel regel, String stubFile) {
		stubDokmet();
		stubPdl(stubFile);

		var response = webTestClient.post()
				.uri(BESTEM_DISTRIBUSJONSKANAL_URL)
				.headers(headers())
				.bodyValue(bestemDistribusjonskanalRequest())
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody(BestemDistribusjonskanalResponse.class)
				.returnResult()
				.getResponseBody();

		assertThat(response)
				.isNotNull()
				.satisfies(it -> {
					assertThat(it.distribusjonskanal()).isEqualTo(PRINT);
					assertThat(it.regel()).isEqualTo(regel.name());
					assertThat(it.regelBegrunnelse()).isEqualTo(regel.begrunnelse);
				});
	}

	private static Stream<Arguments> skalReturnerePrintForPersonMedPersoninfo() {
		return Stream.of(
				Arguments.of(BestemDistribusjonskanalRegel.PERSON_ER_IKKE_I_PDL, "pdl/pdl_feil_response.json"),
				Arguments.of(BestemDistribusjonskanalRegel.PERSON_ER_DOED, "pdl/pdl_doedperson_response.json"),
				Arguments.of(BestemDistribusjonskanalRegel.PERSON_HAR_UKJENT_ALDER, "pdl/pdl_ok_ingen_foedselsdato.json"),
				Arguments.of(BestemDistribusjonskanalRegel.PERSON_ER_UNDER_18, "pdl/pdl_under_18_aar.json")
		);
	}

	/*
	 * Her testes følgende regler:
	 * 11: Har personen gyldig digital kontaktinformasjon? Hvis nei -> PRINT
	 * 12: Er personen reservert mot digital kommunikasjon? Hvis ja -> PRINT
	 * 13: Skal bruker varsles, men mangler digital kontaktinfo? Hvis ja -> PRINT
	 * 14: Har mottaker gyldig epostadresse eller mobilnummer? Hvis nei -> PRINT
	 * 15: Har bruker gyldig digitalt postkassesertifikat, leverandøradresse og brukeradresse? Hvis ja -> SDP
	 * 15: Har bruker gyldig digitalt postkassesertifikat, leverandøradresse og brukeradresse med filstørrelse over 45 megabytes? Hvis ja -> PRINT
	 */
	@ParameterizedTest
	@MethodSource
	void skalReturnereForPersonMedDigitalKontaktinfo(DistribusjonKanalCode distribusjonKanal, BestemDistribusjonskanalRegel regel,
													 String stubFile, Integer forsendelseStoerrelse, Integer antallDokumenter) {
		stubPdl();
		stubDigdirKrrProxy(stubFile);

		if (regel == BestemDistribusjonskanalRegel.BRUKER_MANGLER_EPOST_OG_TELEFON) {
			stubDokmet("dokmet/response_ikke_sdp_varsling.json");
		} else {
			stubDokmet();
		}

		var response = webTestClient.post()
				.uri(BESTEM_DISTRIBUSJONSKANAL_URL)
				.headers(headers())
				.bodyValue(bestemDistribusjonskanalRequest(forsendelseStoerrelse, antallDokumenter))
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody(BestemDistribusjonskanalResponse.class)
				.returnResult()
				.getResponseBody();

		assertThat(response)
				.isNotNull()
				.satisfies(it -> {
					assertThat(it.distribusjonskanal()).isEqualTo(distribusjonKanal);
					assertThat(it.regel()).isEqualTo(regel.name());
					assertThat(it.regelBegrunnelse()).isEqualTo(regel.begrunnelse);
				});
	}

	private static Stream<Arguments> skalReturnereForPersonMedDigitalKontaktinfo() {
		return Stream.of(
				Arguments.of(PRINT, BestemDistribusjonskanalRegel.FINNER_IKKE_DIGITAL_KONTAKTINFORMASJON, "dki/response_person_ikke_funnet.json", 10, null),
				Arguments.of(PRINT, BestemDistribusjonskanalRegel.BRUKER_ER_RESERVERT, "dki/response_bruker_er_reservert.json", 10, null),
				Arguments.of(PRINT, BestemDistribusjonskanalRegel.BRUKER_SDP_MANGLER_VARSELINFO, "dki/response_bruker_mangler_kontaktinfo.json", 5, null),
				Arguments.of(SDP, BestemDistribusjonskanalRegel.BRUKER_HAR_GYLDIG_SDP_ADRESSE, "dki/happy-responsebody.json", DPI_MAX_FORSENDELSE_STOERRELSE_I_MEGABYTES - 1, null),
				Arguments.of(SDP, BestemDistribusjonskanalRegel.BRUKER_HAR_GYLDIG_SDP_ADRESSE, "dki/happy-responsebody.json", null, null),
				Arguments.of(SDP, BestemDistribusjonskanalRegel.BRUKER_HAR_GYLDIG_SDP_ADRESSE, "dki/happy-responsebody.json", null, DPI_MAX_ANTALL_DOKUMENTER_FORSENDELSE),
				Arguments.of(PRINT, BestemDistribusjonskanalRegel.BRUKER_OG_MOTTAKER_ER_FORSKJELLIG, "dki/happy-responsebody.json", null, DPI_MAX_ANTALL_DOKUMENTER_FORSENDELSE + 1),
				Arguments.of(PRINT, BestemDistribusjonskanalRegel.BRUKER_OG_MOTTAKER_ER_FORSKJELLIG, "dki/happy-responsebody.json", DPI_MAX_FORSENDELSE_STOERRELSE_I_MEGABYTES, null),
				Arguments.of(PRINT, BestemDistribusjonskanalRegel.BRUKER_MANGLER_EPOST_OG_TELEFON, "dki/response_bruker_mangler_kontaktinfo.json", 10, null)
		);
	}

	/*
	 * Her testes følgende regler:
	 * 16: Er bruker og mottaker forskjellig (og dokumentTypeId er ikke årsoppgave)? Hvis ja -> PRINT
	 * 17: Er dokumentet arkivert? Hvis nei -> PRINT
	 */
	@ParameterizedTest
	@ValueSource(strings = {"000011"})
	@NullSource
	void skalReturnerePrintDersomBrukerOgMottakerErUlikOgDokumentIkkeErAarsoppgave(String dokumentTypeId) {
		stubDokmet();
		stubPdl();
		stubDigdirKrrProxy("dki/ugyldig-sertifikat-responsebody.json");

		var request = gyldigBestemDistribusjonskanalRequest();
		request.setDokumenttypeId(dokumentTypeId);

		var response = webTestClient.post()
				.uri(BESTEM_DISTRIBUSJONSKANAL_URL)
				.headers(headers())
				.bodyValue(request)
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody(BestemDistribusjonskanalResponse.class)
				.returnResult()
				.getResponseBody();

		assertThat(response)
				.isNotNull()
				.satisfies(it -> {
					assertThat(it.distribusjonskanal()).isEqualTo(PRINT);
					assertThat(it.regel()).isEqualTo(BestemDistribusjonskanalRegel.BRUKER_OG_MOTTAKER_ER_FORSKJELLIG.name());
					assertThat(it.regelBegrunnelse()).isEqualTo(BestemDistribusjonskanalRegel.BRUKER_OG_MOTTAKER_ER_FORSKJELLIG.begrunnelse);
				});
	}

	/*
	 * Her testes følgende regler:
	 * 18: Har dokmentet tema med begrenset innsyn? Hvis ja -> PRINT
	 */
	@ParameterizedTest
	@ValueSource(strings = {"FAR", "KTR", "KTA", "ARP", "ARS", "BBF"})
	void skalReturnerePrintForTemaMedBegrensetInnsyn(String tema) {
		stubDokmet();
		stubPdl();
		stubDigdirKrrProxy("dki/ugyldig-sertifikat-responsebody.json");

		var request = gyldigBestemDistribusjonskanalRequest();
		request.setBrukerId(request.getMottakerId());
		request.setTema(tema);

		var response = webTestClient.post()
				.uri(BESTEM_DISTRIBUSJONSKANAL_URL)
				.headers(headers())
				.bodyValue(request)
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody(BestemDistribusjonskanalResponse.class)
				.returnResult()
				.getResponseBody();

		assertThat(response)
				.isNotNull()
				.satisfies(it -> {
					assertThat(it.distribusjonskanal()).isEqualTo(PRINT);
					assertThat(it.regel()).isEqualTo(BestemDistribusjonskanalRegel.TEMA_HAR_BEGRENSET_INNSYN.name());
					assertThat(it.regelBegrunnelse()).isEqualTo(BestemDistribusjonskanalRegel.TEMA_HAR_BEGRENSET_INNSYN.begrunnelse);
				});
	}

	/*
	 * Her testes følgende regler:
	 * 19: Har bruker gyldig epostadresse eller mobilnummer? Hvis ja -> DITT_NAV
	 */
	@Test
	void skalReturnereDittNavForBrukerMedGyldigEpostEllerMobilnummer() {
		stubDokmet();
		stubPdl();
		stubDigdirKrrProxy("dki/ugyldig-sertifikat-responsebody.json");

		var request = gyldigBestemDistribusjonskanalRequest();
		request.setBrukerId(request.getMottakerId());

		var response = webTestClient.post()
				.uri(BESTEM_DISTRIBUSJONSKANAL_URL)
				.headers(headers())
				.bodyValue(request)
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody(BestemDistribusjonskanalResponse.class)
				.returnResult()
				.getResponseBody();

		assertThat(response)
				.isNotNull()
				.satisfies(it -> {
					assertThat(it.distribusjonskanal()).isEqualTo(DITT_NAV);
					assertThat(it.regel()).isEqualTo(BestemDistribusjonskanalRegel.BRUKER_HAR_GYLDIG_EPOST_ELLER_MOBILNUMMER.name());
					assertThat(it.regelBegrunnelse()).isEqualTo(BestemDistribusjonskanalRegel.BRUKER_HAR_GYLDIG_EPOST_ELLER_MOBILNUMMER.begrunnelse);
				});
	}

	/*
	 * Her testes følgende regler:
	 * 7: Mottaker er hverken PERSON eller ORGANISASJON -> PRINT
	 */
	@ParameterizedTest
	@ValueSource(strings = {"12345", "123456789", "82345678902", "11111111111", "GB:UK010"})
	void skalReturnerePrintDersomMottakerHverkenErPersonEllerOrganisasjon(String mottakerId) {
		stubDokmet();

		var request = gyldigBestemDistribusjonskanalRequest();
		request.setMottakerId(mottakerId);

		var response = webTestClient.post()
				.uri(BESTEM_DISTRIBUSJONSKANAL_URL)
				.headers(headers())
				.bodyValue(request)
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody(BestemDistribusjonskanalResponse.class)
				.returnResult()
				.getResponseBody();

		assertThat(response)
				.isNotNull()
				.satisfies(it -> {
					assertThat(it.distribusjonskanal()).isEqualTo(PRINT);
					assertThat(it.regel()).isEqualTo(BestemDistribusjonskanalRegel.MOTTAKER_ER_IKKE_PERSON_ELLER_ORGANISASJON.name());
					assertThat(it.regelBegrunnelse()).isEqualTo(BestemDistribusjonskanalRegel.MOTTAKER_ER_IKKE_PERSON_ELLER_ORGANISASJON.begrunnelse);
				});
	}

	@ParameterizedTest
	@MethodSource
	void skalReturnereBadRequestVedUgyldigInput(String mottakerId, String brukerId, String tema) {

		String jsonString = """
				{
				  "mottakerId": "%s",
				  "brukerId": "%s",
				  "tema": "%s",
				  "dokumenttypeId": "dokumentType",
				  "erArkivert": true
				}
				""";

		String jsonRequest = String.format(jsonString, mottakerId, brukerId, tema);

		webTestClient.post()
				.uri(BESTEM_DISTRIBUSJONSKANAL_URL)
				.headers(headers -> {
					headers.setBearerAuth(jwt());
					headers.setContentType(APPLICATION_JSON);
				})
				.bodyValue(jsonRequest)
				.exchange()
				.expectStatus()
				.isBadRequest();
	}

	private static Stream<Arguments> skalReturnereBadRequestVedUgyldigInput() {
		return Stream.of(
				Arguments.of("", "12345678902", "PEN"),
				Arguments.of("12345678901", null, "PEN"),
				Arguments.of("12345678901", "abc", "PEN"),
				Arguments.of("12345678901", "123456789012", "PEN"),
				Arguments.of("12345678901", "12345678902", null),
				Arguments.of("12345678901", "123456789012", "TO"),
				Arguments.of("12345678901", "123456789012", "FIRE"),
				Arguments.of("111111111111111111111", "12345678902", "PEN")
		);
	}

	@ParameterizedTest
	@ValueSource(ints = {500, 400})
	void skalReturnereInternalServerErrorVedFeilFraEksternTjeneste(int httpStatusCode) {
		HttpStatus httpStatus = HttpStatus.valueOf(httpStatusCode);
		stubDokmet(httpStatus);

		var response = webTestClient.post()
				.uri(BESTEM_DISTRIBUSJONSKANAL_URL)
				.headers(headers())
				.bodyValue(gyldigBestemDistribusjonskanalRequest())
				.exchange()
				.expectStatus()
				.is5xxServerError()
				.expectBody(ProblemDetail.class)
				.returnResult()
				.getResponseBody();

		var feilmelding = String.format("%s feil ved kall mot ekstern tjeneste", httpStatus.is5xxServerError() ? "Teknisk" : "Funksjonell");

		assertThat(response)
				.isNotNull()
				.satisfies(it -> {
					assertThat(it.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR.value());
					assertThat(it.getTitle()).isEqualTo(feilmelding);
				});
	}

	@Test
	void skalReturnereInternalServerErrorVedFunksjonellEksternTjeneste() {
		stubDokmet(BAD_REQUEST);

		var response = webTestClient.post()
				.uri(BESTEM_DISTRIBUSJONSKANAL_URL)
				.headers(headers())
				.bodyValue(gyldigBestemDistribusjonskanalRequest())
				.exchange()
				.expectStatus()
				.is5xxServerError()
				.expectBody(ProblemDetail.class)
				.returnResult()
				.getResponseBody();

		assertThat(response)
				.isNotNull()
				.satisfies(it -> {
					assertThat(it.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR.value());
					assertThat(it.getTitle()).isEqualTo("Funksjonell feil ved kall mot ekstern tjeneste");
				});
	}

	@Test
	void skalReturnerePrintWhenOrgnrIsNeitherHovedOrUnderenheter() {

		stubDokmet();
		stubDigdirKrrProxy();
		stubAltinn();
		stubEnhetsregisteret(NOT_FOUND, null, UNDERENHET_ORGNR);
		stubEnhetsGruppeRoller(GRUPPEROLLER_OK_PATH, UNDERENHET_ORGNR);
		stubUnderenhetsregisteret(NOT_FOUND, "", UNDERENHET_ORGNR);

		var request = gyldigBestemDistribusjonskanalRequest();
		request.setMottakerId(UNDERENHET_ORGNR);
		request.setDokumenttypeId("1234");

		var response = webTestClient.post()
				.uri(BESTEM_DISTRIBUSJONSKANAL_URL)
				.headers(headers())
				.bodyValue(request)
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody(BestemDistribusjonskanalResponse.class)
				.returnResult()
				.getResponseBody();

		assertThat(response)
				.isNotNull()
				.satisfies(it -> {
					assertThat(it.distribusjonskanal()).isEqualTo(PRINT);
					assertThat(it.regel()).isEqualTo(MOTTAKER_ER_IKKE_PERSON_ELLER_ORGANISASJON.name());
					assertThat(it.regelBegrunnelse()).isEqualTo(MOTTAKER_ER_IKKE_PERSON_ELLER_ORGANISASJON.begrunnelse);
				});
	}

	@Test
	void skalReturnereInternalServerErrorWhenOrgnrThrowsBadRequestException() {

		stubDokmet();
		stubDigdirKrrProxy();
		stubAltinn();
		stubUnderenhetsregisteret(BAD_REQUEST, "", UNDERENHET_ORGNR);

		var request = gyldigBestemDistribusjonskanalRequest();
		request.setMottakerId(UNDERENHET_ORGNR);
		request.setDokumenttypeId("1234");

		var response = webTestClient.post()
				.uri(BESTEM_DISTRIBUSJONSKANAL_URL)
				.headers(headers())
				.bodyValue(request)
				.exchange()
				.expectStatus()
				.is5xxServerError()
				.expectBody(ProblemDetail.class)
				.returnResult()
				.getResponseBody();

		assertThat(response)
				.isNotNull()
				.satisfies(it -> {
					assertThat(response.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR.value());
					assertThat(response.getDetail()).contains("Kall mot Brønnøysundregistrene feilet funksjonelt med feilmelding");

				});
	}

	@Test
	void skalReturnereDPVTWhenOrgnrIsUnderenheterOgHarHovedenhetMedRolletype() {

		stubDokmet();
		stubDigdirKrrProxy();
		stubAltinn();
		stubEnhetsregisteret(NOT_FOUND, null, UNDERENHET_ORGNR);
		stubEnhetsGruppeRoller(GRUPPEROLLER_OK_PATH, HOVEDENHET_ORGNR);
		stubUnderenhetsregisteret(OK, "enhetsregisteret/underenhet_response.json", UNDERENHET_ORGNR);
		stubSecondEnhetsregisteret("enhetsregisteret/ikke_konkurs_enhetsregisteret.json", HOVEDENHET_ORGNR);

		var request = gyldigBestemDistribusjonskanalRequest();
		request.setMottakerId(UNDERENHET_ORGNR);
		request.setDokumenttypeId("1234");

		var response = webTestClient.post()
				.uri(BESTEM_DISTRIBUSJONSKANAL_URL)
				.headers(headers())
				.bodyValue(request)
				.exchange()
				.expectStatus()
				.isOk()
				.expectBody(BestemDistribusjonskanalResponse.class)
				.returnResult()
				.getResponseBody();

		assertThat(response)
				.isNotNull()
				.satisfies(it -> {
					assertThat(it.distribusjonskanal()).isEqualTo(DPVT);
					assertThat(it.regel()).isEqualTo(ORGANISASJON_MED_ALTINN_INFO.name());
					assertThat(it.regelBegrunnelse()).isEqualTo(ORGANISASJON_MED_ALTINN_INFO.begrunnelse);
				});
	}


	@Test
	void skalReturnereUnauthorizedVedManglendeOIDCToken() {

		var response = webTestClient.post()
				.uri(BESTEM_DISTRIBUSJONSKANAL_URL)
				.bodyValue(gyldigBestemDistribusjonskanalRequest())
				.exchange()
				.expectStatus()
				.isUnauthorized()
				.expectBody(ProblemDetail.class)
				.returnResult()
				.getResponseBody();

		assertThat(response)
				.isNotNull()
				.satisfies(it -> {
					assertThat(it.getStatus()).isEqualTo(UNAUTHORIZED.value());
					assertThat(it.getTitle()).isEqualTo("OIDC token mangler eller er ugyldig");
				});
	}

	@Test
	void skalReturnereUnauthorizedVedUgyldigOIDCToken() {
		var ugyldigToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQss123";

		var response = webTestClient.post()
				.uri(BESTEM_DISTRIBUSJONSKANAL_URL)
				.header(AUTHORIZATION, "Bearer " + ugyldigToken)
				.bodyValue(gyldigBestemDistribusjonskanalRequest())
				.exchange()
				.expectStatus()
				.isUnauthorized()
				.expectBody(ProblemDetail.class)
				.returnResult()
				.getResponseBody();

		assertThat(response)
				.isNotNull()
				.satisfies(it -> {
					assertThat(it.getStatus()).isEqualTo(UNAUTHORIZED.value());
					assertThat(it.getTitle()).isEqualTo("OIDC token mangler eller er ugyldig");
				});
	}

	@Test
	void skalReturnereServiceUnavailableVedGjentagendeTekniskFeilFraEksternTjeneste() {
		stubDokmet();
		stubPdl();
		stubDigdirKrrProxy(INTERNAL_SERVER_ERROR);

		var request = gyldigBestemDistribusjonskanalRequest();
		request.setBrukerId(request.getMottakerId());

		CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("digdir-krr-proxy");
		Retry retry = retryRegistry.retry("digdir-krr-proxy");

		var retries = retry.getRetryConfig().getMaxAttempts();
		var slidingWindowSize = circuitBreaker.getCircuitBreakerConfig().getSlidingWindowSize();

		// CircuitBreaker trigger etter sliding-window-size / retry-max-attempts feilede requests
		Flux.range(1, slidingWindowSize / retries)
				.flatMap(i -> webTestClient.post()
						.uri(BESTEM_DISTRIBUSJONSKANAL_URL)
						.headers(headers())
						.bodyValue(request)
						.exchange()
						.expectStatus()
						.isEqualTo(INTERNAL_SERVER_ERROR)
						.returnResult(ProblemDetail.class)
						.getResponseBody()
				)
				.blockLast();

		assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

		var response = webTestClient.post()
				.uri(BESTEM_DISTRIBUSJONSKANAL_URL)
				.headers(headers())
				.bodyValue(request)
				.exchange()
				.expectStatus().isEqualTo(SERVICE_UNAVAILABLE)
				.expectBody(ProblemDetail.class)
				.returnResult()
				.getResponseBody();

		assertThat(response)
				.isNotNull()
				.extracting(ProblemDetail::getDetail)
				.isEqualTo("CircuitBreaker 'digdir-krr-proxy' is OPEN and does not permit further calls");
	}

	private BestemDistribusjonskanalRequest gyldigBestemDistribusjonskanalRequest() {
		return bestemDistribusjonskanalRequest(10, 3);
	}

	private BestemDistribusjonskanalRequest bestemDistribusjonskanalRequest() {
		return new BestemDistribusjonskanalRequest(
				"12345678901",
				"12345678902",
				"PEN",
				"dokumentType",
				true,
				10,
				3,
				null
		);
	}

	private BestemDistribusjonskanalRequest bestemDistribusjonskanalRequestMedMetadataType(String forsendelseMetadataType) {
		return new BestemDistribusjonskanalRequest(
				"12345678901",
				"12345678902",
				"PEN",
				"dokumentType",
				true,
				10,
				3,
				forsendelseMetadataType
		);
	}

	private BestemDistribusjonskanalRequest bestemDistribusjonskanalMedNullDokumenttypeId() {
		return new BestemDistribusjonskanalRequest(
				"12345678901",
				"12345678902",
				"PEN",
				null,
				true,
				10,
				3,
				null
		);
	}

	private BestemDistribusjonskanalRequest bestemDistribusjonskanalRequest(Integer filstoerrelse, Integer antallDokumenter) {
		return new BestemDistribusjonskanalRequest(
				"12345678901",
				"12345678902",
				"PEN",
				"dokumentType",
				true,
				filstoerrelse,
				antallDokumenter,
				null
		);
	}

	private Consumer<HttpHeaders> headers() {
		return headers -> {
			headers.setBearerAuth(jwt());
			headers.add(NAV_CONSUMER_ID, "testConsumer");
		};
	}
}
