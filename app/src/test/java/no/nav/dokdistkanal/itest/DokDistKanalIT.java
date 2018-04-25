package no.nav.dokdistkanal.itest;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static no.nav.dokdistkanal.rest.DokDistKanalRestController.BESTEM_KANAL_URI_PATH;
import static org.junit.Assert.assertEquals;

import no.nav.dokdistkanal.common.DistribusjonKanalCode;
import no.nav.dokdistkanal.common.DokDistKanalRequest;
import no.nav.dokdistkanal.common.DokDistKanalResponse;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;

public class DokDistKanalIT extends AbstractIT {

	private static final String DOKUMENTTYPEID = "000009";
	private static final String MOTTAKERID = "***gammelt_fnr***";

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
		assertEquals(DistribusjonKanalCode.SDP,actualResponse.getDistribusjonsKanal());
	}
}
