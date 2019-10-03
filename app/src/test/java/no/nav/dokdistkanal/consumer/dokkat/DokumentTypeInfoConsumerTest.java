package no.nav.dokdistkanal.consumer.dokkat;

import static no.nav.dokdistkanal.common.DistribusjonKanalCode.LOKAL_PRINT;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import no.nav.dokdistkanal.consumer.dokkat.to.DokumentTypeInfoTo;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import no.nav.dokdistkanal.exceptions.functional.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.functional.DokkatFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.DokkatTechnicalException;
import no.nav.dokdistkanal.metrics.MicrometerMetrics;
import no.nav.dokkat.api.tkat020.DistribusjonInfoTo;
import no.nav.dokkat.api.tkat020.v4.DokumentProduksjonsInfoToV4;
import no.nav.dokkat.api.tkat020.v4.DokumentTypeInfoToV4;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class DokumentTypeInfoConsumerTest {
	private static final String DOKTYPE = "***gammelt_fnr***";
	private static final String ARKIVSYSTEM = "JOARK";

	private RestTemplate restTemplate;
	private DokumentTypeInfoConsumer dokumentTypeInfoConsumer;
	private MicrometerMetrics metrics;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void setUp() {
		restTemplate = mock(RestTemplate.class);
		metrics = mock(MicrometerMetrics.class);
		dokumentTypeInfoConsumer = new DokumentTypeInfoConsumer(restTemplate, metrics);
	}

	@Test
	public void shouldRunOK() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
		DokumentTypeInfoToV4 response = createResponse();
		response.getDokumentProduksjonsInfo().setDistribusjonInfo(null);
		when(restTemplate.getForObject(any(String.class), eq(DokumentTypeInfoToV4.class), any(Map.class)))
				.thenReturn(response);

		DokumentTypeInfoTo dokumentTypeInfoTo = dokumentTypeInfoConsumer.hentDokumenttypeInfo(DOKTYPE);
		assertThat(dokumentTypeInfoTo.getArkivsystem(), equalTo(ARKIVSYSTEM));
		assertThat(dokumentTypeInfoTo.isVarslingSdp(), equalTo(Boolean.FALSE));
	}

	@Test
	public void shouldRunOKDistKanalLokalPrint() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
		when(restTemplate.getForObject(any(String.class), eq(DokumentTypeInfoToV4.class), any(Map.class)))
				.thenReturn(createResponse());

		DokumentTypeInfoTo dokumentTypeInfoTo = dokumentTypeInfoConsumer.hentDokumenttypeInfo(DOKTYPE);
		assertThat(dokumentTypeInfoTo.getArkivsystem(), equalTo(ARKIVSYSTEM));
		assertThat(dokumentTypeInfoTo.isVarslingSdp(), equalTo(Boolean.FALSE));
		assertThat(dokumentTypeInfoTo.getPredefinertDistKanal(), equalTo(LOKAL_PRINT.name()));
	}

	@Test
	public void shouldThrowFunctionalExceptionWhenBadRequest() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
		when(restTemplate.getForObject(any(String.class), eq(DokumentTypeInfoToV4.class), any(Map.class)))
				.thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

		expectedException.expectMessage("DokumentTypeInfoConsumer feilet. (HttpStatus=400 BAD_REQUEST) for dokumenttypeId");
		expectedException.expect(DokkatFunctionalException.class);

		dokumentTypeInfoConsumer.hentDokumenttypeInfo(DOKTYPE);
	}

	@Test
	public void shouldThrowTechnicalExceptionWhenServerException() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
		when(restTemplate.getForObject(any(String.class), eq(DokumentTypeInfoToV4.class), any(Map.class)))
				.thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));

		expectedException.expectMessage("DokumentTypeInfoConsumer feilet med statusCode=503");
		expectedException.expect(DokkatTechnicalException.class);

		dokumentTypeInfoConsumer.hentDokumenttypeInfo(DOKTYPE);
	}

	@Test
	public void shouldThrowSecurityExceptionWhenUnauthorized() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
		when(restTemplate.getForObject(any(String.class), eq(DokumentTypeInfoToV4.class), any(Map.class)))
				.thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

		expectedException.expectMessage("DokumentTypeInfoConsumer feilet (HttpStatus=401 UNAUTHORIZED) for dokumenttypeId:" + DOKTYPE);
		expectedException.expect(DokDistKanalSecurityException.class);

		dokumentTypeInfoConsumer.hentDokumenttypeInfo(DOKTYPE);
	}


	@Test
	public void shouldThrowTechnicalExceptionWhenRuntimeException() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
		when(restTemplate.getForObject(any(String.class), eq(DokumentTypeInfoToV4.class), any(Map.class)))
				.thenThrow(new RuntimeException());

		expectedException.expectMessage("DokumentTypeInfoConsumer feilet med message");
		expectedException.expect(DokkatTechnicalException.class);

		dokumentTypeInfoConsumer.hentDokumenttypeInfo(DOKTYPE);
	}

	private DokumentTypeInfoToV4 createResponse() {
		DokumentTypeInfoToV4 response = new DokumentTypeInfoToV4();
		response.setDokumentType(DOKTYPE);
		response.setArkivSystem(ARKIVSYSTEM);
		DistribusjonInfoTo distribusjonInfoTo = new DistribusjonInfoTo();
		distribusjonInfoTo.setPredefinertDistKanal(LOKAL_PRINT.name());
		DokumentProduksjonsInfoToV4 dokumentProduksjonsInfoToV4 = new DokumentProduksjonsInfoToV4();
		dokumentProduksjonsInfoToV4.setDistribusjonInfo(distribusjonInfoTo);
		response.setDokumentProduksjonsInfo(dokumentProduksjonsInfoToV4);
		return response;
	}
}
