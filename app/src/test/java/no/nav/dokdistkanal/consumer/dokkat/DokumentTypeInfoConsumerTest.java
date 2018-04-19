package no.nav.dokdistkanal.consumer.dokkat;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import no.nav.dokdistkanal.consumer.dokkat.to.DokumentTypeInfoTo;
import no.nav.dokdistkanal.exceptions.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.DokDistKanalTechnicalException;
import no.nav.dokkat.api.tkat020.v3.DokumentMottakInfoToV3;
import no.nav.dokkat.api.tkat020.v3.DokumentTypeInfoToV3;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class DokumentTypeInfoConsumerTest {
	private static final String DOKTYPE = "***gammelt_fnr***";
	private static final String ARKIVBEHANDLING = "Behandling";
	private static final String ARKIVSYSTEM = "System";

	private RestTemplate restTemplate;
	private DokumentTypeInfoConsumer dokumentTypeInfoConsumer;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void setUp() throws Exception {
		restTemplate = mock(RestTemplate.class);
		dokumentTypeInfoConsumer = new DokumentTypeInfoConsumer(restTemplate);
	}

	@Test
	public void shouldRunOK() throws DokDistKanalFunctionalException {
		when(restTemplate.getForObject(any(String.class), eq(DokumentTypeInfoToV3.class), any(Map.class)))
				.thenReturn(createResponse());

		DokumentTypeInfoTo dokumentTypeInfoTo = dokumentTypeInfoConsumer.hentDokumenttypeInfo(DOKTYPE);
		assertThat(dokumentTypeInfoTo.getArkivbehandling(), equalTo(ARKIVBEHANDLING));
		assertThat(dokumentTypeInfoTo.getArkivsystem(), equalTo(ARKIVSYSTEM));
	}

	@Test
	public void shouldThrowFunctionalExceptionWhenBadRequest() throws Exception {
		when(restTemplate.getForObject(any(String.class), eq(DokumentTypeInfoToV3.class), any(Map.class)))
				.thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

		expectedException.expectMessage("Dokkat.TKAT020 failed with bad request for dokumenttypeId:");
		expectedException.expect(DokDistKanalFunctionalException.class);

		dokumentTypeInfoConsumer.hentDokumenttypeInfo(DOKTYPE);
	}

	@Test
	public void shouldThrowTechnicalExceptionWhenServiceUnavaliable() throws Exception {
		when(restTemplate.getForObject(any(String.class), eq(DokumentTypeInfoToV3.class), any(Map.class)))
				.thenThrow(new HttpClientErrorException(HttpStatus.SERVICE_UNAVAILABLE));

		expectedException.expectMessage("Dokkat.TKAT020 failed. (HttpStatus=503) for dokumenttypeId:");
		expectedException.expect(DokDistKanalTechnicalException.class);

		dokumentTypeInfoConsumer.hentDokumenttypeInfo(DOKTYPE);
	}

	@Test
	public void shouldThrowTechnicalExceptionWhenServerException() throws Exception {
		when(restTemplate.getForObject(any(String.class), eq(DokumentTypeInfoToV3.class), any(Map.class)))
				.thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));

		expectedException.expectMessage("Dokkat.TKAT020 failed with statusCode=503");
		expectedException.expect(DokDistKanalTechnicalException.class);

		dokumentTypeInfoConsumer.hentDokumenttypeInfo(DOKTYPE);
	}

	@Test
	public void shouldThrowTechnicalExceptionWhenRuntimeException() throws Exception {
		when(restTemplate.getForObject(any(String.class), eq(DokumentTypeInfoToV3.class), any(Map.class)))
				.thenThrow(new RuntimeException());

		expectedException.expectMessage("Dokkat.TKAT020 failed with message");
		expectedException.expect(DokDistKanalTechnicalException.class);

		dokumentTypeInfoConsumer.hentDokumenttypeInfo(DOKTYPE);
	}

	private DokumentTypeInfoToV3 createResponse() {
		DokumentMottakInfoToV3 dokumentMottakInfoToV3 = new DokumentMottakInfoToV3();
		dokumentMottakInfoToV3.setArkivBehandling(ARKIVBEHANDLING);
		dokumentMottakInfoToV3.setArkivSystem(ARKIVSYSTEM);
		DokumentTypeInfoToV3 response = new DokumentTypeInfoToV3();
		response.setDokumentType(DOKTYPE);
		response.setDokumentMottakInfo(dokumentMottakInfoToV3);
		return response;
	}
}
