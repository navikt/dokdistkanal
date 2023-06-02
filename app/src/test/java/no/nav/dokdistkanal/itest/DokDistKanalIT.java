package no.nav.dokdistkanal.itest;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import no.nav.dokdistkanal.common.DistribusjonKanalCode;
import no.nav.dokdistkanal.common.DokDistKanalRequest;
import no.nav.dokdistkanal.common.DokDistKanalResponse;
import no.nav.dokdistkanal.common.MottakerTypeCode;
import no.nav.dokdistkanal.constants.MDCConstants;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import no.nav.dokdistkanal.exceptions.functional.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.service.DokDistKanalService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpStatusCodeException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.DPVT;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.PRINT;
import static no.nav.dokdistkanal.common.MottakerTypeCode.PERSON;
import static no.nav.dokdistkanal.rest.DokDistKanalRestController.BESTEM_KANAL_URI_PATH;
import static no.nav.dokdistkanal.service.DokDistKanalServiceTest.TEMA;
import static no.nav.dokdistkanal.util.TestUtils.classpathToString;
import static no.nav.dokdistkanal.util.TestUtils.getLogMessage;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.hateoas.MediaTypes.HAL_JSON_VALUE;
import static org.springframework.http.HttpHeaders.ACCEPT_ENCODING;
import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

public class DokDistKanalIT extends AbstractIT {

	private final static String CONSUMER_ID = "srvdokdistfordeling";
	private static final String DOKUMENTTYPEID = "000009";
	private static final String MOTTAKERID = "12345678901";
	private final static String BOST_MOTTAKERID = "80000123456";
	private final static String ONLY_ONE_MOTTAKERID = "11111111111";
	private static final String ORGMOTTAKERID = "123456789";
	private static final String SAMHANDLERMOTTAKERID = "987654321";
	private final static boolean ER_ARKIVERT_TRUE = true;
	private final static boolean INKLUDER_SIKKER_DIGITALPOSTKASSE = true;
	private static final String ALTINN_HAPPY_FILE_PATH = "altinn/serviceowner_happy_response.json";
	private static final String PDL_HAPPY_FILE_PATH = "pdl/pdl_ok_response.json";
	private static final String SKATTEETATEN_ORGNUMMER = "974761076";
	private static final String INFOTRYGD_DOKUMENTTYPE_ID = "000044";

	private ListAppender<ILoggingEvent> logWatcher;

	@BeforeEach
	public void runBefore() {
		logWatcher = new ListAppender<>();
		logWatcher.start();
		((Logger) LoggerFactory.getLogger(DokDistKanalService.class)).addAppender(logWatcher);
		MDC.put(MDCConstants.CONSUMER_ID, CONSUMER_ID);
		stubAllApi();
	}

	@AfterEach
	public void tearDown() {
		((Logger) LoggerFactory.getLogger(DokDistKanalService.class)).detachAndStopAllAppenders();
	}


	/**
	 * Komplertterer fullt brevdatasett der mottaker er person
	 */
	@Test
	public void shouldGetDistribusjonskanal() {
		stubGetAltinn(ALTINN_HAPPY_FILE_PATH);
		stubPostPDL(PDL_HAPPY_FILE_PATH);

		DokDistKanalRequest request = baseDokDistKanalRequestBuilder().tema("PEN").build();

		DokDistKanalResponse actualResponse = restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		assertEquals(DistribusjonKanalCode.SDP, actualResponse.getDistribusjonsKanal());
	}

	@Test
	public void shouldReturnPrintForBOSTIdenter() {
		stubPostPDL(PDL_HAPPY_FILE_PATH);

		DokDistKanalRequest request = dokDistKanalRequestBuilder(BOST_MOTTAKERID).build();
		DokDistKanalResponse actualResponse = restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		assertEquals(PRINT, actualResponse.getDistribusjonsKanal());
	}

	/**
	 * Komplettert fullt brevdatasett der mottaker er person
	 */
	@Test
	public void shouldReturnPrintForOnlyOneIdenter() {
		stubPostPDL(PDL_HAPPY_FILE_PATH);

		DokDistKanalRequest request = dokDistKanalRequestBuilder(ONLY_ONE_MOTTAKERID).build();

		DokDistKanalResponse actualResponse = restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		assertEquals(PRINT, actualResponse.getDistribusjonsKanal());
	}

	@Test
	public void shouldGetDistribusjonskanalPrintForOrganisasjon() {
		DokDistKanalRequest request = baseDokDistKanalRequestBuilder().mottakerId(ORGMOTTAKERID).tema("PEN")
				.mottakerType(MottakerTypeCode.ORGANISASJON).brukerId(ORGMOTTAKERID).build();

		stubGetAltinn(ALTINN_HAPPY_FILE_PATH);
		stubPostPDL(PDL_HAPPY_FILE_PATH);

		DokDistKanalResponse actualResponse = restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		assertEquals(PRINT, actualResponse.getDistribusjonsKanal());
	}

	/**
	 * Komplertterer fullt brevdatasett der mottaker er samhandler
	 */
	@Test
	public void shouldGetDistribusjonskanalPrintForSamhandler() {
		DokDistKanalRequest request = baseDokDistKanalRequestBuilder()
				.mottakerId(SAMHANDLERMOTTAKERID)
				.mottakerType(MottakerTypeCode.SAMHANDLER_HPR)
				.brukerId(SAMHANDLERMOTTAKERID)
				.tema("PEN")
				.build();
		stubPostPDL(PDL_HAPPY_FILE_PATH);
		DokDistKanalResponse actualResponse = restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		assertEquals(PRINT, actualResponse.getDistribusjonsKanal());
	}

	/**
	 * Kompletterer fullt brevdatasett der mottaker er samhandler utenlandsk organisasjon
	 */
	@Test
	public void shouldGetDistribusjonskanalPrintForSamhandlerUtenlandskOrganisasjon() {
		DokDistKanalRequest request = baseDokDistKanalRequestBuilder()
				.mottakerId(SAMHANDLERMOTTAKERID)
				.mottakerType(MottakerTypeCode.SAMHANDLER_UTL_ORG)
				.brukerId(SAMHANDLERMOTTAKERID)
				.tema("PEN")
				.build();

		DokDistKanalResponse serviceResponse = restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());

		assertThat(getLogMessage(logWatcher)).contains("Mottaker er av typen SAMHANDLER_UTL_ORG");
	}

	@Test
	public void shouldGetDPVTWhenOrgNummerIsFromDPVTListAndDokumentTypeIdIsNotFromInfotrygd() {
		DokDistKanalRequest request = dokDistKanalRequestBuilder(DOKUMENTTYPEID)
				.mottakerId(SKATTEETATEN_ORGNUMMER)
				.mottakerType(MottakerTypeCode.ORGANISASJON)
				.brukerId(SKATTEETATEN_ORGNUMMER)
				.tema("PEN")
				.build();

		stubGetAltinn(ALTINN_HAPPY_FILE_PATH);
		stubPostPDL(PDL_HAPPY_FILE_PATH);

		DokDistKanalResponse actualResponse = restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		assertEquals(DPVT, actualResponse.getDistribusjonsKanal());
		assertThat(getLogMessage(logWatcher)).contains("er en gyldig altinn-serviceowner notifikasjonsmottaker");
	}


	@Test
	public void shouldSetKanalPrintNaarSamhandlerUkjent() throws DokDistKanalFunctionalException, DokDistKanalSecurityException {
		DokDistKanalRequest request = baseDokDistKanalRequestBuilder()
				.mottakerId(SAMHANDLERMOTTAKERID)
				.mottakerType(MottakerTypeCode.SAMHANDLER_UKJENT)
				.brukerId(SAMHANDLERMOTTAKERID)
				.tema("PEN")
				.build();

		DokDistKanalResponse serviceResponse = restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		assertEquals(PRINT, serviceResponse.getDistribusjonsKanal());
		assertThat(getLogMessage(logWatcher)).contains("Mottaker er av typen SAMHANDLER_UKJENT");
	}

	@Test
	public void shouldReturnPrintWhenPersonErDoed() {
		//Stub web services:
		stubPostPDL("pdl/pdl_doedperson_response.json");
		;

		DokDistKanalRequest request = baseDokDistKanalRequestBuilder().tema("PEN").build();

		DokDistKanalResponse actualResponse = restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		assertEquals(PRINT, actualResponse.getDistribusjonsKanal());
	}

	@Test
	public void shouldReturnPrintWhenPersonNotFound() {
		//Stub web services:
		stubGetAltinn(ALTINN_HAPPY_FILE_PATH);
		stubPostPDL("pdl/pdl_feil_response.json");

		DokDistKanalRequest request = baseDokDistKanalRequestBuilder().tema("PEN").build();

		DokDistKanalResponse actualResponse = restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		assertEquals(DistribusjonKanalCode.PRINT, actualResponse.getDistribusjonsKanal());
	}

	@Test
	public void shouldReturnPrintWhenSertifikatNotValid() {
		//Stub web services:
		stubFor(post("/DIGDIR_KRR_PROXY/rest/v1/personer?inkluderSikkerDigitalPost=true")
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("treg001/dki/ugyldig-sertifikat-responsebody.json")));
		stubGetAltinn(ALTINN_HAPPY_FILE_PATH);
		stubPostPDL(PDL_HAPPY_FILE_PATH);

		DokDistKanalRequest request = baseDokDistKanalRequestBuilder().tema("PEN").build();

		DokDistKanalResponse actualResponse = restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		assertEquals(PRINT, actualResponse.getDistribusjonsKanal());
	}

	@Test
	public void shouldThrowFunctionalExceptionWhenTemaIsNull() {
		//Stub web services:
		stubFor(get(urlPathMatching("/DOKUMENTTYPEINFO_V4(.*)"))
				.willReturn(aResponse().withStatus(NOT_FOUND.value())
						.withHeader("Content-Type", "application/json")
						.withBody("Could not find dokumenttypeId: DOKTYPENOTFOUND in repository")));
		stubGetAltinn(ALTINN_HAPPY_FILE_PATH);
		stubPostPDL(PDL_HAPPY_FILE_PATH);

		DokDistKanalRequest request = baseDokDistKanalRequestBuilder().dokumentTypeId("DOKTYPENOTFOUND").build();

		HttpStatusCodeException e = Assertions.assertThrows(HttpStatusCodeException.class, () ->
				restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class));

		assertEquals(BAD_REQUEST, e.getStatusCode());
		assertThat(e.getResponseBodyAsString()).contains("Ugyldig input: Feltet tema kan ikke være null eller tomt. Fikk tema=null\",\"path\":\"/rest/bestemKanal");
	}

	@Test
	public void shouldThrowFunctionalExceptionFromDokkatWhenNotFound() {
		//Stub web services:
		stubFor(get(urlPathMatching("/DOKUMENTTYPEINFO_V4(.*)"))
				.willReturn(aResponse().withStatus(NOT_FOUND.value())
						.withHeader("Content-Type", "application/json")
						.withBody("Could not find dokumenttypeId: DOKTYPENOTFOUND in repository")));
		stubGetAltinn(ALTINN_HAPPY_FILE_PATH);
		stubPostPDL(PDL_HAPPY_FILE_PATH);

		DokDistKanalRequest request = baseDokDistKanalRequestBuilder().dokumentTypeId("DOKTYPENOTFOUND").tema("PEN").build();

		HttpStatusCodeException e = Assertions.assertThrows(HttpStatusCodeException.class, () ->
				restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class));
		assertEquals(BAD_REQUEST, e.getStatusCode());
		assertThat(e.getResponseBodyAsString()).contains("DokumentTypeInfoConsumer feilet. (HttpStatus=404 NOT_FOUND) for dokumenttypeId:DOKTYPENOTFOUND");

	}

	@Test
	public void shouldReturnPrintFromDKIWhenKontaktinformasjonNotFound() {
		//Stub web services:
		stubFor(post("/DIGDIR_KRR_PROXY/rest/v1/personer?inkluderSikkerDigitalPost=true")
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("treg001/dki/feilmelding-responsebody.json")));
		stubGetAltinn(ALTINN_HAPPY_FILE_PATH);
		stubPostPDL(PDL_HAPPY_FILE_PATH);

		DokDistKanalRequest request = baseDokDistKanalRequestBuilder().tema("PEN").build();

		DokDistKanalResponse actualResponse = restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		assertEquals(PRINT, actualResponse.getDistribusjonsKanal());
	}

	@Test
	public void shouldReturnPrintWhenBrukerPdlFoedelsdatoNull() {
		DokDistKanalRequest request = baseDokDistKanalRequestBuilder().tema("PEN").build();

		stubGetAltinn(ALTINN_HAPPY_FILE_PATH);
		stubPostPDL("pdl/pdl_ok_ingen_foedselsdato.json");

		DokDistKanalResponse actualResponse = restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		assertEquals(PRINT, actualResponse.getDistribusjonsKanal());
	}

	@Test
	public void shouldThrowFunctionalExceptionFromDKIWhenSikkerhetsbegrensning() {
		//Stub web services:
		stubFor(post("/DIGDIR_KRR_PROXY/rest/v1/personer?inkluderSikkerDigitalPost=true")
				.willReturn(aResponse().withStatus(BAD_REQUEST.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)));

		DokDistKanalRequest request = baseDokDistKanalRequestBuilder().build();

		HttpStatusCodeException e = Assertions.assertThrows(HttpStatusCodeException.class, () ->
				restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class));
		assertEquals(BAD_REQUEST, e.getStatusCode());
		assertThat(e.getResponseBodyAsString()).contains("Ugyldig input: Feltet tema kan ikke være null eller tomt. Fikk tema=null\",\"path\":\"/rest/bestemKanal");
	}

	@Test
	public void shouldThrowFunctionalExceptionFromPaaloggingsnivaaUgyldigIdent() {
		//Stub web services:
		stubFor(post("/DIGDIR_KRR_PROXY/rest/v1/personer?inkluderSikkerDigitalPost=true")
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("treg001/dki/ditt-nav-responsebody.json")));
		stubGetAltinn(ALTINN_HAPPY_FILE_PATH);
		stubPostPDL(PDL_HAPPY_FILE_PATH);
		stubFor(get(urlPathMatching("/HENTPAALOGGINGSNIVAA_V1(.*)"))
				.willReturn(aResponse().withStatus(NOT_FOUND.value())
						.withHeader("Content-Type", "application/json")
						.withBody("Personident ikke gydig")));
		DokDistKanalRequest request = baseDokDistKanalRequestBuilder().tema("PEN").build();

		DokDistKanalResponse actualResponse = restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		assertEquals(PRINT, actualResponse.getDistribusjonsKanal());
	}

	private void stubAllApi() {
		stubFor(get(urlPathMatching("/DOKUMENTTYPEINFO_V4(.*)"))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("treg001/dokkat/happy-response.json")));

		stubFor(get(urlPathMatching("/HENTPAALOGGINGSNIVAA_V1(.*)"))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("treg001/paalogging/happy-response.json")));

		stubFor(get(urlPathMatching("/STS"))
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("felles/sts/stsResponse_happy.json")));

		//leverandoerSertifikat som ligger under mappene treg001/dokkat/... er utsendt av DigDir og har utløpsdato februar 2023.
		//Det må byttes ut innen den tid hvis ikke vil testene feile. Mer info i README.
		stubFor(post("/DIGDIR_KRR_PROXY/rest/v1/personer?inkluderSikkerDigitalPost=" + INKLUDER_SIKKER_DIGITALPOSTKASSE)
				.willReturn(aResponse().withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBodyFile("treg001/dki/happy-responsebody.json")));

		stubFor(post(urlMatching("/maskinporten"))
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
						.withBody(classpathToString("__files/altinn/maskinporten_happy_response.json"))));

		stubFor(post("/azure_token")
				.willReturn(aResponse()
						.withStatus(OK.value())
						.withHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
						.withBodyFile("azure/token_response_dummy.json")));
	}

	private void stubGetAltinn(String path) {
		stubFor(get("/altinn/serviceowner/notifications/validaterecipient?organizationNumber=974761076&serviceCode=123456&serviceEditionCode=1")
				.willReturn(aResponse()
						.withHeader(CONTENT_TYPE, HAL_JSON_VALUE)
						.withHeader(ACCEPT_ENCODING, "gzip")
						.withBodyFile(path)));
	}

	private void stubPostPDL(String path) {
		stubFor(post("/graphql").willReturn(aResponse().withStatus(OK.value())
				.withHeader(CONTENT_TYPE, APPLICATION_JSON_VALUE)
				.withBodyFile(path)));
	}

	public static DokDistKanalRequest.DokDistKanalRequestBuilder baseDokDistKanalRequestBuilder() {
		return DokDistKanalRequest.builder()
				.dokumentTypeId(DOKUMENTTYPEID)
				.mottakerId(MOTTAKERID)
				.mottakerType(PERSON)
				.brukerId(MOTTAKERID)
				.erArkivert(ER_ARKIVERT_TRUE);
	}

	private static DokDistKanalRequest.DokDistKanalRequestBuilder dokDistKanalRequestBuilder(String mottakerId) {
		return DokDistKanalRequest.builder()
				.dokumentTypeId(DOKUMENTTYPEID)
				.mottakerId(mottakerId)
				.mottakerType(PERSON)
				.brukerId(mottakerId)
				.erArkivert(ER_ARKIVERT_TRUE)
				.tema(TEMA);
	}
}