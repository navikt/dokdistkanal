package no.nav.dokdistkanal.itest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static no.nav.dokdistkanal.rest.DokDistKanalRestController.BESTEM_KANAL_URI_PATH;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;

import no.nav.dokdistkanal.common.DistribusjonKanalCode;
import no.nav.dokdistkanal.common.DokDistKanalRequest;
import no.nav.dokdistkanal.common.DokDistKanalResponse;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;

public class DokDistKanalIT extends AbstractIT {

	private static final String DOKUMENTTYPEID = "000009";
	private static final String MOTTAKERID = "***gammelt_fnr***";
	private static final String ORGMOTTAKERID = "123456789";

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


		stubFor(post("/STS")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("felles/sts/sts_signature-responsebody.xml")));

		stubFor(post("/VIRKSOMHET_PERSON_V3")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("treg001/personV3/happypath-responsebody.xml")));

		stubFor(post("/VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("treg001/dki/happy-responsebody.xml")));

	}

	/**
	 * Komplertterer fullt brevdatasett der mottaker er person
	 */
	@Test
	public void shouldGetDistribusjonskanal() throws Exception {
		DokDistKanalRequest request = DokDistKanalRequest.builder().dokumentTypeId(DOKUMENTTYPEID).mottakerId(MOTTAKERID).build();
		DokDistKanalResponse actualResponse = restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		assertEquals(DistribusjonKanalCode.SDP, actualResponse.getDistribusjonsKanal());
	}

	/**
	 * Komplertterer fullt brevdatasett der mottaker er person
	 */
	@Test
	public void shouldGetDistribusjonskanalOrg() throws Exception {
		DokDistKanalRequest request = DokDistKanalRequest.builder().dokumentTypeId(DOKUMENTTYPEID).mottakerId(ORGMOTTAKERID).build();
		DokDistKanalResponse actualResponse = restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		assertEquals(DistribusjonKanalCode.PRINT, actualResponse.getDistribusjonsKanal());
	}

	@Test
	public void shouldThrowWhenPersonV3FailsSecurityErrorNoAccess() throws Exception {
		stubFor(post("/VIRKSOMHET_PERSON_V3")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("treg001/personV3/hentPerson-FunksjonellFeil-SikkerhetsBegrensning-responsebody.xml")));

		DokDistKanalRequest request = DokDistKanalRequest.builder().dokumentTypeId(DOKUMENTTYPEID).mottakerId(MOTTAKERID).build();
		try {
			restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
			assertFalse("Test did not throw exception", Boolean.TRUE);
		} catch (HttpStatusCodeException e) {
			assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
			assertThat(e.getResponseBodyAsString(), CoreMatchers.containsString("PersonV3.hentPerson feiler på grunn av sikkerhetsbegresning"));
			assertThat(e.getResponseBodyAsString(), CoreMatchers.containsString("DokDistKanalSecurityException"));
		}
	}

	@Test
	public void shouldThrowFunctionalExceptionFromPersonPlugin() throws Exception {
		//Stub web services:
		stubFor(post("/VIRKSOMHET_PERSON_V3")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("treg001/personV3/hentPerson-FunksjonellFeil-PersonIkkeFunnet-responsebody.xml")));
		DokDistKanalRequest request = DokDistKanalRequest.builder().dokumentTypeId(DOKUMENTTYPEID).mottakerId(MOTTAKERID).build();
		DokDistKanalResponse actualResponse = restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		assertEquals(DistribusjonKanalCode.PRINT, actualResponse.getDistribusjonsKanal());
	}

	@Test
	public void shouldThrowFunctionalExceptionFromDokkatWhenNotFound() throws Exception {
		//Stub web services:
		stubFor(get(urlPathMatching("/DOKUMENTTYPEINFO_V4(.*)"))
				.willReturn(aResponse().withStatus(HttpStatus.NOT_FOUND.value())
						.withHeader("Content-Type", "application/json")
						.withBody("Could not find dokumenttypeId: DOKTYPENOTFOUND in repository")));
		try {
			DokDistKanalRequest request = DokDistKanalRequest.builder().dokumentTypeId("DOKTYPENOTFOUND").mottakerId(MOTTAKERID).build();
			restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
			assertFalse(Boolean.TRUE);
		} catch (HttpStatusCodeException e) {
			assertEquals(e.getStatusCode(), HttpStatus.BAD_REQUEST);
			assertThat(e.getResponseBodyAsString(), CoreMatchers.containsString("DokumentTypeInfoConsumer feilet. (HttpStatus=404) for dokumenttypeId:DOKTYPENOTFOUND"));
			assertThat(e.getResponseBodyAsString(), CoreMatchers.containsString("DokDistKanalFunctionalException"));
		}
	}

	@Test
	public void shouldThrowFunctionalExceptionFromDKIWhenKontaktinformasjonNotFound() throws Exception {
		//Stub web services:
		stubFor(post("/VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("treg001/dki/ikke-funnet.xml")));
		DokDistKanalRequest request = DokDistKanalRequest.builder().dokumentTypeId(DOKUMENTTYPEID).mottakerId(MOTTAKERID).build();
		DokDistKanalResponse actualResponse = restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		assertEquals(DistribusjonKanalCode.PRINT, actualResponse.getDistribusjonsKanal());
	}

	@Test
	public void shouldThrowFunctionalExceptionFromDKIWhenPersonNotFound() throws Exception {
		//Stub web services:
		stubFor(post("/VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("treg001/dki/person-ikke-funnet.xml")));
		DokDistKanalRequest request = DokDistKanalRequest.builder().dokumentTypeId(DOKUMENTTYPEID).mottakerId(MOTTAKERID).build();
		restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		DokDistKanalResponse actualResponse = restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		assertEquals(DistribusjonKanalCode.PRINT, actualResponse.getDistribusjonsKanal());
	}

	@Test
	public void shouldThrowFunctionalExceptionFromDKIWhenSikkerhetsbegrensning() throws Exception {
		//Stub web services:
		stubFor(post("/VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("treg001/dki/sikkerhet.xml")));
		try {
			DokDistKanalRequest request = DokDistKanalRequest.builder().dokumentTypeId(DOKUMENTTYPEID).mottakerId(MOTTAKERID).build();
			restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
			assertFalse(Boolean.TRUE);
		} catch (HttpStatusCodeException e) {
			assertEquals(e.getStatusCode(), HttpStatus.UNAUTHORIZED);
			assertThat(e.getResponseBodyAsString(), CoreMatchers.containsString("DigitalKontaktinformasjonV1.hentDigitakKontaktinformasjon feiler på grunn av sikkerhetsbegresning. message=Sikkerhetsbegrensning ved kall til DIFI"));
			assertThat(e.getResponseBodyAsString(), CoreMatchers.containsString("DokDistKanalSecurityException"));
		}
	}

	@Test
	public void shouldThrowFunctionalExceptionFromPaaloggingsnivaaUgyldigIdent() throws Exception {
		//Stub web services:
		stubFor(post("/VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("treg001/dki/ditt-nav-responsebody.xml")));
		stubFor(get(urlPathMatching("/HENTPAALOGGINGSNIVAA_V1(.*)"))
				.willReturn(aResponse().withStatus(HttpStatus.BAD_REQUEST.value())
						.withHeader("Content-Type", "application/json")
						.withBody("Personident ikke gydig")));
		try {
			DokDistKanalRequest request = DokDistKanalRequest.builder().dokumentTypeId(DOKUMENTTYPEID).mottakerId(MOTTAKERID).build();
			restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
			assertFalse(Boolean.TRUE);
		} catch (HttpStatusCodeException e) {
			assertEquals(e.getStatusCode(), HttpStatus.BAD_REQUEST);
			assertThat(e.getResponseBodyAsString(), CoreMatchers.containsString("Sikkerhetsnivaa.hentPaaloggingsnivaa feilet (HttpStatus=404)"));
			assertThat(e.getResponseBodyAsString(), CoreMatchers.containsString("DokDistKanalFunctionalException"));
		}

	}
}
