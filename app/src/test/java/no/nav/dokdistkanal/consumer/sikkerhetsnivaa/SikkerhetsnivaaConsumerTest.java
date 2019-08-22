package no.nav.dokdistkanal.consumer.sikkerhetsnivaa;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.schema.SikkerhetsnivaaRequest;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.schema.SikkerhetsnivaaResponse;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.to.SikkerhetsnivaaTo;
import no.nav.dokdistkanal.exceptions.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
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

@RunWith(MockitoJUnitRunner.class)
public class SikkerhetsnivaaConsumerTest {

	private static final String FNR = "***gammelt_fnr***";

	private RestTemplate restTemplate;
	private SikkerhetsnivaaConsumer sikkerhetsnivaaConsumer;


	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void setUp() {
		restTemplate = mock(RestTemplate.class);
		sikkerhetsnivaaConsumer= new SikkerhetsnivaaConsumer(restTemplate);
	}

	@Test
	public void shouldRunOK() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
		SikkerhetsnivaaResponse response = new SikkerhetsnivaaResponse();
		response.setPersonidentifikator(FNR);
		response.setHarbruktnivaa4(Boolean.FALSE);
		when(restTemplate.postForObject(any(String.class), any(SikkerhetsnivaaRequest.class), eq(SikkerhetsnivaaResponse.class))).thenReturn(response);
		SikkerhetsnivaaTo sikkerhetsnivaaTo = sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(FNR);
		assertThat(sikkerhetsnivaaTo.isHarLoggetPaaNivaa4(), equalTo(Boolean.FALSE));
		assertThat(sikkerhetsnivaaTo.getPersonIdent(), equalTo(FNR));
	}

	@Test
	public void shouldReturnFalseWhenSikkerhetsnivaaNotFound() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
		when(restTemplate.postForObject(any(String.class), any(SikkerhetsnivaaRequest.class), eq(SikkerhetsnivaaResponse.class))).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
		SikkerhetsnivaaTo sikkerhetsnivaaTo = sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(FNR);
		assertThat(sikkerhetsnivaaTo.isHarLoggetPaaNivaa4(), equalTo(Boolean.FALSE));
		assertThat(sikkerhetsnivaaTo.getPersonIdent(), equalTo(FNR));
	}

	@Test
	public void shouldThrowFunctionalExceptionWhenHttpStatusBadRequest() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
		expectedException.expect(SikkerhetsnivaaFunctionalException.class);
		expectedException.expectMessage("Sikkerhetsnivaa.hentPaaloggingsnivaa feilet (HttpStatus=400 BAD_REQUEST)");
		when(restTemplate.postForObject(any(String.class), any(SikkerhetsnivaaRequest.class), eq(SikkerhetsnivaaResponse.class))).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
		sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(FNR);
	}


	@Test
	public void shouldThrowTechnicalExceptionWhenRuntimeException() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
		expectedException.expect(SikkerhetsnivaaTechnicalException.class);
		expectedException.expectMessage("Sikkerhetsnivaa.hentPaaloggingsnivaa feilet");
		when(restTemplate.postForObject(any(String.class), any(SikkerhetsnivaaRequest.class), eq(SikkerhetsnivaaResponse.class))).thenThrow(new RuntimeException());
		sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(FNR);
	}

	@Test
	public void shouldThrowSecurityExceptionWhenHttpStatusForbidden() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
		expectedException.expect(DokDistKanalSecurityException.class);
		expectedException.expectMessage("Sikkerhetsnivaa.hentPaaloggingsnivaa feilet (HttpStatus=403 FORBIDDEN)");
		when(restTemplate.postForObject(any(String.class), any(SikkerhetsnivaaRequest.class), eq(SikkerhetsnivaaResponse.class))).thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));
		sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(FNR);
	}

	@Test
	public void shouldThrowTechnicalExceptionWhenHttpStatusInternalServerError() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
		expectedException.expect(SikkerhetsnivaaTechnicalException.class);
		expectedException.expectMessage("Sikkerhetsnivaa.hentPaaloggingsnivaa feilet (HttpStatus=500 INTERNAL_SERVER_ERROR)");
		when(restTemplate.postForObject(any(String.class), any(SikkerhetsnivaaRequest.class), eq(SikkerhetsnivaaResponse.class))).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));
		sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(FNR);
	}

	@Test
	public void shouldPing() throws Exception {
		when(restTemplate.getForObject("isReady", String.class)).thenReturn("\"ok\"");
		sikkerhetsnivaaConsumer.ping();
		verify(restTemplate).getForObject("isReady", String.class);
	}
}
