package no.nav.dokdistkanal.itest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static no.nav.dokdistkanal.rest.DokDistKanalRestController.BESTEM_KANAL_URI_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import no.nav.dokdistkanal.common.DistribusjonKanalCode;
import no.nav.dokdistkanal.common.DokDistKanalRequest;
import no.nav.dokdistkanal.common.DokDistKanalResponse;
import no.nav.dokdistkanal.common.MottakerTypeCode;
import org.apache.http.HttpHeaders;
import org.apache.http.entity.ContentType;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpStatusCodeException;

public class DokDistKanalIT extends AbstractIT {

	private static final String DOKUMENTTYPEID = "000009";
	private static final String MOTTAKERID = "***gammelt_fnr***";
	private static final String ORGMOTTAKERID = "123456789";
	private static final String SAMHANDLERMOTTAKERID = "987654321";
	private final static boolean ER_ARKIVERT_TRUE = true;
	private final static boolean INKLUDER_SIKKER_DIGITALPOSTKASSE = true;

	@Before
	public void runBefore() {
		stubFor(get(urlPathMatching("/DOKUMENTTYPEINFO_V4(.*)"))
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader("Content-Type", "application/json")
						.withBodyFile("treg001/dokkat/happy-response.json")));

		stubFor(get(urlPathMatching("/HENTPAALOGGINGSNIVAA_V1(.*)"))
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader("Content-Type", "application/json")
						.withBodyFile("treg001/paalogging/happy-response.json")));

		stubFor(get(urlPathMatching("/STS"))
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
						.withBodyFile("felles/sts/stsResponse_happy.json")));

		stubFor(get(urlPathMatching("/TPS/v1/innsyn/person"))
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
						.withBodyFile("treg001/tps/happy-path.json")));

		stubFor(get("/DKIF_V2/api/v1/personer/kontaktinformasjon?inkluderSikkerDigitalPost=" + INKLUDER_SIKKER_DIGITALPOSTKASSE)
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
						.withBodyFile("treg001/dki/happy-responsebody.json")));
	}

	/**
	 * Komplertterer fullt brevdatasett der mottaker er person
	 */
	@Test
	public void shouldGetDistribusjonskanal() {
		DokDistKanalRequest request = baseDokDistKanalRequestBuilder().build();

		DokDistKanalResponse actualResponse = restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		assertEquals(DistribusjonKanalCode.SDP, actualResponse.getDistribusjonsKanal());
	}

	/**
	 * Komplertterer fullt brevdatasett der mottaker er organisasjon
	 */
	@Test
	public void shouldGetDistribusjonskanalPrintForOrganisasjon() {
		DokDistKanalRequest request = baseDokDistKanalRequestBuilder().mottakerId(ORGMOTTAKERID)
				.mottakerType(MottakerTypeCode.ORGANISASJON).brukerId(ORGMOTTAKERID).build();

		DokDistKanalResponse actualResponse = restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		assertEquals(DistribusjonKanalCode.PRINT, actualResponse.getDistribusjonsKanal());
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
				.build();
		DokDistKanalResponse actualResponse = restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		assertEquals(DistribusjonKanalCode.PRINT, actualResponse.getDistribusjonsKanal());
	}

	@Test
	public void shouldThrowFunctionalExceptionFromPersonPlugin() {
		//Stub web services:
		stubFor(get(urlPathMatching("/TPS/v1/innsyn/person"))
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
						.withBodyFile("treg001/tps/tps-not-found.json")));

		DokDistKanalRequest request = baseDokDistKanalRequestBuilder().build();

		DokDistKanalResponse actualResponse = restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		assertEquals(DistribusjonKanalCode.PRINT, actualResponse.getDistribusjonsKanal());
	}

	@Test
	public void shouldThrowFunctionalExceptionFromDokkatWhenNotFound() {
		//Stub web services:
		stubFor(get(urlPathMatching("/DOKUMENTTYPEINFO_V4(.*)"))
				.willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())
						.withHeader("Content-Type", "application/json")
						.withBody("Could not find dokumenttypeId: DOKTYPENOTFOUND in repository")));
		try {
			DokDistKanalRequest request = baseDokDistKanalRequestBuilder().dokumentTypeId("DOKTYPENOTFOUND").build();

			restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
			assertFalse(Boolean.TRUE);
		} catch (HttpStatusCodeException e) {
			assertEquals(HttpStatus.BAD_REQUEST, e.getStatusCode());
			assertThat(e.getResponseBodyAsString(), CoreMatchers.containsString("DokumentTypeInfoConsumer feilet. (HttpStatus=404 NOT_FOUND) for dokumenttypeId:DOKTYPENOTFOUND"));
		}
	}

	@Test
	public void shouldThrowFunctionalExceptionFromDKIWhenKontaktinformasjonNotFound() {
		//Stub web services:
		stubFor(get("/DKIF_V2/api/v1/personer/kontaktinformasjon?inkluderSikkerDigitalPost=true")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
						.withBodyFile("treg001/dki/person-ikke-funnet.json")));

		DokDistKanalRequest request = baseDokDistKanalRequestBuilder().build();

		DokDistKanalResponse actualResponse = restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		assertEquals(DistribusjonKanalCode.PRINT, actualResponse.getDistribusjonsKanal());
	}

	@Test
	public void shouldThrowFunctionalExceptionFromDKIWhenSikkerhetsbegrensning() {
		//Stub web services:
		stubFor(get("/DKIF_V2/api/v1/personer/kontaktinformasjon?inkluderSikkerDigitalPost=true")
				.willReturn(aResponse().withStatus(HttpStatus.FORBIDDEN.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
				));
		try {
			DokDistKanalRequest request = baseDokDistKanalRequestBuilder().build();

			restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
			assertFalse(Boolean.TRUE);
		} catch (HttpStatusCodeException e) {
			assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, e.getStatusCode());
			assertThat(e.getResponseBodyAsString(), CoreMatchers.containsString("Funksjonell feil ved kall mot DigitalKontaktinformasjonV2.digitalKontaktinformasjon feilmelding=403 Forbidden"));
		}
	}

	@Test
	public void shouldThrowFunctionalExceptionFromPaaloggingsnivaaUgyldigIdent() {
		//Stub web services:
		stubFor(get("/DKIF_V2/api/v1/personer/kontaktinformasjon?inkluderSikkerDigitalPost=true")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType())
						.withBodyFile("treg001/dki/ditt-nav-responsebody.json")));

		stubFor(get(urlPathMatching("/HENTPAALOGGINGSNIVAA_V1(.*)"))
				.willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())
						.withHeader("Content-Type", "application/json")
						.withBody("Personident ikke gydig")));
		DokDistKanalRequest request = baseDokDistKanalRequestBuilder().build();

		DokDistKanalResponse actualResponse = restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		assertEquals(DistribusjonKanalCode.PRINT, actualResponse.getDistribusjonsKanal());
	}

	private DokDistKanalRequest.DokDistKanalRequestBuilder baseDokDistKanalRequestBuilder() {
		return DokDistKanalRequest.builder()
				.dokumentTypeId(DOKUMENTTYPEID)
				.mottakerId(MOTTAKERID)
				.mottakerType(MottakerTypeCode.PERSON)
				.brukerId(MOTTAKERID)
				.erArkivert(ER_ARKIVERT_TRUE);
	}
}