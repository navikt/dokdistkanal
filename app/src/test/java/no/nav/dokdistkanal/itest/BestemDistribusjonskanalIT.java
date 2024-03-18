package no.nav.dokdistkanal.itest;

import com.github.tomakehurst.wiremock.client.WireMock;
import no.nav.dokdistkanal.common.DistribusjonKanalCode;
import no.nav.dokdistkanal.domain.BestemDistribusjonskanalRegel;
import no.nav.dokdistkanal.rest.bestemdistribusjonskanal.BestemDistribusjonskanalRequest;
import no.nav.dokdistkanal.rest.bestemdistribusjonskanal.BestemDistribusjonskanalResponse;
import org.junit.jupiter.api.AfterEach;
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

import java.util.function.Consumer;
import java.util.stream.Stream;

import static no.nav.dokdistkanal.common.DistribusjonKanalCode.DITT_NAV;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.DPVT;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.INGEN_DISTRIBUSJON;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.LOKAL_PRINT;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.PRINT;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.SDP;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.TRYGDERETTEN;
import static no.nav.dokdistkanal.constants.DomainConstants.DPI_MAX_FORSENDELSE_STOERRELSE_I_MEGABYTES;
import static no.nav.dokdistkanal.constants.NavHeaders.NAV_CONSUMER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/*
 * Se https://confluence.adeo.no/pages/viewpage.action?pageId=294148459 for funksjonelle behandlingsregler
 */
public class BestemDistribusjonskanalIT extends AbstractIT {

	@BeforeEach
	public void setUp() {
		stubMaskinporten();
		stubAzure();
		stubAltinn();
	}

	@AfterEach
	public void tearDown() {
		WireMock.removeAllMappings();
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
				.bodyValue(bestemDistribusjonskanalRequest())
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
	 * Her testes følgende regler:
	 * 5: Er mottakerType ORGANISASJON og dokument produsert i infotrygd? Hvis ja -> PRINT
	 * 6: Er mottakerType ORGANISASJON og har varslingsinformasjon i Altinn, ikke konkurs og har enhets grupperoller? Hvis ja -> DPVT
	 * 6.1: Er mottakerType ORGANISASJON og mangler varslingsinformasjon for DPV? Hvis ja -> PRINT
	 * 6.2: Er mottakerType ORGANISASJON, har varslingsinformasjon i Altinn og konkurs? Hvis ja -> PRINT
	 * 6.3: Er mottakerType ORGANISASJON, har varslingsinformasjon i Altinn, ikke konkurs og registert person er døde eller har ingen fødselsdato? Hvis ja -> PRINT
	 */
	@ParameterizedTest
	@MethodSource
	void skalReturnereForOrganisasjon(DistribusjonKanalCode distribusjonKanal, BestemDistribusjonskanalRegel regel,
									  String mottakerId, String dokumentTypeId, String hentEnhetPath, String grupperollerPath) {
		stubDokmet();
		stubDigdirKrrProxy();
		stubAltinn();
		stubEnhetsregisteret(OK, hentEnhetPath, mottakerId);
		stubEnhetsGruppeRoller(OK, grupperollerPath, mottakerId);

		var request = bestemDistribusjonskanalRequest();
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
				Arguments.of(PRINT, BestemDistribusjonskanalRegel.ORGANISASJON_MED_INFOTRYGD_DOKUMENT, "974761076", "000044", null, null),
				Arguments.of(DPVT, BestemDistribusjonskanalRegel.ORGANISASJON_MED_ALTINN_INFO, "974761076", "000000", HENT_ENHET_OK_PATH, GRUPPEROLLER_OK_PATH),
				Arguments.of(PRINT, BestemDistribusjonskanalRegel.ORGANISASJON_UTEN_ALTINN_INFO, "889640782", "000000", null, null),
				Arguments.of(PRINT, BestemDistribusjonskanalRegel.ORGANISASJON_ER_KONKURS, "974761076", "000000", KONKURS_ENHET_PATH, GRUPPEROLLER_OK_PATH),
				Arguments.of(PRINT, BestemDistribusjonskanalRegel.ORGANISASJON_MANGLER_NODVENDIG_ROLLER, "974761076", "000000", HENT_ENHET_OK_PATH, GRUPPEROLLER_PERSON_ER_DOED_PATH)
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
													 String stubFile, Integer forsendelseStoerrelse) {
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
				.bodyValue(bestemDistribusjonskanalRequestMedFilstoerrelse(forsendelseStoerrelse))
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
				Arguments.of(PRINT, BestemDistribusjonskanalRegel.FINNER_IKKE_DIGITAL_KONTAKTINFORMASJON, "dki/response_person_ikke_funnet.json", 10),
				Arguments.of(PRINT, BestemDistribusjonskanalRegel.BRUKER_ER_RESERVERT, "dki/response_bruker_er_reservert.json", 10),
				Arguments.of(PRINT, BestemDistribusjonskanalRegel.BRUKER_SDP_MANGLER_VARSELINFO, "dki/response_bruker_mangler_kontaktinfo.json", 5),
				Arguments.of(SDP, BestemDistribusjonskanalRegel.BRUKER_HAR_GYLDIG_SDP_ADRESSE, "dki/happy-responsebody.json", DPI_MAX_FORSENDELSE_STOERRELSE_I_MEGABYTES - 1),
				Arguments.of(SDP, BestemDistribusjonskanalRegel.BRUKER_HAR_GYLDIG_SDP_ADRESSE, "dki/happy-responsebody.json", null),
				Arguments.of(PRINT, BestemDistribusjonskanalRegel.BRUKER_OG_MOTTAKER_ER_FORSKJELLIG, "dki/happy-responsebody.json", DPI_MAX_FORSENDELSE_STOERRELSE_I_MEGABYTES),
				Arguments.of(PRINT, BestemDistribusjonskanalRegel.BRUKER_MANGLER_EPOST_OG_TELEFON, "dki/response_bruker_mangler_kontaktinfo.json", 10)
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

		var request = bestemDistribusjonskanalRequest();
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
	@ValueSource(strings = {"FAR", "KTR", "KTA", "ARP", "ARS"})
	void skalReturnerePrintForTemaMedBegrensetInnsyn(String tema) {
		stubDokmet();
		stubPdl();
		stubDigdirKrrProxy("dki/ugyldig-sertifikat-responsebody.json");

		var request = bestemDistribusjonskanalRequest();
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

		var request = bestemDistribusjonskanalRequest();
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

		var request = bestemDistribusjonskanalRequest();
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
				.bodyValue(bestemDistribusjonskanalRequest())
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
				.bodyValue(bestemDistribusjonskanalRequest())
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
	void skalReturnereNotFoundFunksjonellExceptionVedKallTilEnhetsregistreretTjeneste() {

		stubDokmet();
		stubDigdirKrrProxy();
		stubAltinn();
		stubEnhetsregisteret(NOT_FOUND, "", MOTTAKER_ID);
		stubEnhetsGruppeRoller(OK, GRUPPEROLLER_OK_PATH, MOTTAKER_ID);

		var request = bestemDistribusjonskanalRequest();
		request.setMottakerId(MOTTAKER_ID);
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
					assertThat(it.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR.value());
					assertThat(it.getTitle()).isEqualTo("Funksjonell feil ved kall mot ekstern tjeneste");
				});
	}

	@Test
	void skalReturnereUnauthorizedVedManglendeOIDCToken() {

		var response = webTestClient.post()
				.uri(BESTEM_DISTRIBUSJONSKANAL_URL)
				.bodyValue(bestemDistribusjonskanalRequest())
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
				.bodyValue(bestemDistribusjonskanalRequest())
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

	private BestemDistribusjonskanalRequest bestemDistribusjonskanalRequest() {
		return new BestemDistribusjonskanalRequest(
				"12345678901",
				"12345678902",
				"PEN",
				"dokumentType",
				true,
				10
		);
	}

	private BestemDistribusjonskanalRequest bestemDistribusjonskanalMedNullDokumenttypeId() {
		return new BestemDistribusjonskanalRequest(
				"12345678901",
				"12345678902",
				"PEN",
				null,
				true,
				10
		);
	}

	private BestemDistribusjonskanalRequest bestemDistribusjonskanalRequestMedFilstoerrelse(Integer filstoerrelse) {
		return new BestemDistribusjonskanalRequest(
				"12345678901",
				"12345678902",
				"PEN",
				"dokumentType",
				true,
				filstoerrelse
		);
	}

	private Consumer<HttpHeaders> headers() {
		return headers -> {
			headers.setBearerAuth(jwt());
			headers.add(NAV_CONSUMER_ID, "testConsumer");
		};
	}
}
