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
import no.nav.dokdistkanal.common.MottakerTypeCode;
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
	private final static Boolean ER_ARKIVERT_TRUE = Boolean.TRUE;

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
	public void shouldThrowWhenPersonV3FailsSecurityErrorNoAccess() {
		stubFor(post("/VIRKSOMHET_PERSON_V3")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("treg001/personV3/hentPerson-FunksjonellFeil-SikkerhetsBegrensning-responsebody.xml")));

		DokDistKanalRequest request = baseDokDistKanalRequestBuilder().build();

		try {
			restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
			assertFalse("Test did not throw exception", Boolean.TRUE);
		} catch (HttpStatusCodeException e) {
			assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
			assertThat(e.getResponseBodyAsString(), CoreMatchers.containsString("PersonV3.hentPerson feiler på grunn av sikkerhetsbegresning"));
		}
	}

	@Test
	public void shouldThrowFunctionalExceptionFromPersonPlugin() {
		//Stub web services:
		stubFor(post("/VIRKSOMHET_PERSON_V3")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("treg001/personV3/hentPerson-FunksjonellFeil-PersonIkkeFunnet-responsebody.xml")));
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
		stubFor(post("/VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("treg001/dki/ikke-funnet.xml")));
		DokDistKanalRequest request = baseDokDistKanalRequestBuilder().build();

		DokDistKanalResponse actualResponse = restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		assertEquals(DistribusjonKanalCode.PRINT, actualResponse.getDistribusjonsKanal());
	}

	@Test
	public void shouldThrowFunctionalExceptionFromDKIWhenPersonNotFound() {
		//Stub web services:
		stubFor(post("/VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("treg001/dki/person-ikke-funnet.xml")));
		DokDistKanalRequest request = baseDokDistKanalRequestBuilder().build();

		restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		DokDistKanalResponse actualResponse = restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
		assertEquals(DistribusjonKanalCode.PRINT, actualResponse.getDistribusjonsKanal());
	}

	@Test
	public void shouldThrowFunctionalExceptionFromDKIWhenSikkerhetsbegrensning() {
		//Stub web services:
		stubFor(post("/VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("treg001/dki/sikkerhet.xml")));
		try {
			DokDistKanalRequest request = baseDokDistKanalRequestBuilder().build();

			restTemplate.postForObject(LOCAL_ENDPOINT_URL + BESTEM_KANAL_URI_PATH, request, DokDistKanalResponse.class);
			assertFalse(Boolean.TRUE);
		} catch (HttpStatusCodeException e) {
			assertEquals(HttpStatus.UNAUTHORIZED, e.getStatusCode());
			assertThat(e.getResponseBodyAsString(), CoreMatchers.containsString("DigitalKontaktinformasjonV1.hentDigitakKontaktinformasjon feiler på grunn av sikkerhetsbegresning. message=Sikkerhetsbegrensning ved kall til DIFI"));
		}
	}

	@Test
	public void shouldThrowFunctionalExceptionFromPaaloggingsnivaaUgyldigIdent() {
		//Stub web services:
		stubFor(post("/VIRKSOMHET_DIGITALKONTAKINFORMASJON_V1")
				.willReturn(aResponse().withStatus(HttpStatus.OK.value())
						.withBodyFile("treg001/dki/ditt-nav-responsebody.xml")));
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
