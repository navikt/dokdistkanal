package no.nav.dokdistkanal.consumer.sikkerhetsnivaa;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.schema.SikkerhetsnivaaRequest;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.schema.SikkerhetsnivaaResponse;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.to.SikkerhetsnivaaTo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@RunWith(MockitoJUnitRunner.class)
public class SikkerhetsnivaaConsumerTest {

	private static final String FNR = "***gammelt_fnr***";

	private RestTemplate restTemplate;
	private SikkerhetsnivaaRestComsumer sikkerhetsnivaaConsumer;


	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void setUp() {
		restTemplate = mock(RestTemplate.class);
		sikkerhetsnivaaConsumer= new SikkerhetsnivaaRestComsumer(restTemplate);
	}

	@Test
	public void shouldRunOK() throws SikkerhetsnivaaFunctionalException {
		SikkerhetsnivaaResponse response = new SikkerhetsnivaaResponse();
		response.setPersonidentifikator(FNR);
		response.setHarbruktnivaa4(Boolean.FALSE);
		when(restTemplate.postForObject(any(String.class), any(SikkerhetsnivaaRequest.class), eq(SikkerhetsnivaaResponse.class))).thenReturn(response);
		SikkerhetsnivaaTo sikkerhetsnivaaTo = sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(FNR);
		assertThat(sikkerhetsnivaaTo.getHarLoggetPaaNivaa4(), equalTo(Boolean.FALSE));
		assertThat(sikkerhetsnivaaTo.getPersonIdent(), equalTo(FNR));
	}

	@Test
	public void shouldReturnNullWhenSikkerhetsnivaaNotFound() throws SikkerhetsnivaaFunctionalException {
		when(restTemplate.postForObject(any(String.class), any(SikkerhetsnivaaRequest.class), eq(SikkerhetsnivaaResponse.class))).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
		SikkerhetsnivaaTo sikkerhetsnivaaTo = sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(FNR);
		assertThat(sikkerhetsnivaaTo, nullValue());
	}

	@Test
	public void shouldThrowFunctionalExceptionWhenHttpStatusBadRequest() throws SikkerhetsnivaaFunctionalException {
		expectedException.expect(SikkerhetsnivaaFunctionalException.class);
		expectedException.expectMessage("Sikkerhetsnivaa.hentPaaloggingsnivaa feilet med BAD REQUEST for fnr=" + FNR);
		when(restTemplate.postForObject(any(String.class), any(SikkerhetsnivaaRequest.class), eq(SikkerhetsnivaaResponse.class))).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
		sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(FNR);
	}


	@Test
	public void shouldThrowTechnicalExceptionWhenRuntimeException() throws SikkerhetsnivaaFunctionalException {
		expectedException.expect(SikkerhetsnivaaTechnicalException.class);
		expectedException.expectMessage("Sikkerhetsnivaa.hentPaaloggingsnivaa feilet for fnr=" + FNR);
		when(restTemplate.postForObject(any(String.class), any(SikkerhetsnivaaRequest.class), eq(SikkerhetsnivaaResponse.class))).thenThrow(new RuntimeException());
		sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(FNR);
	}

	@Test
	public void shouldThrowTechnicalExceptionWhenHttpStatusForbidden() throws SikkerhetsnivaaFunctionalException {
		expectedException.expect(SikkerhetsnivaaTechnicalException.class);
		expectedException.expectMessage("Sikkerhetsnivaa.hentPaaloggingsnivaa feilet for fnr=" + FNR);
		when(restTemplate.postForObject(any(String.class), any(SikkerhetsnivaaRequest.class), eq(SikkerhetsnivaaResponse.class))).thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));
		sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(FNR);
	}

	@Test
	public void shouldPing() throws Exception {
		when(restTemplate.getForObject("isReady", String.class)).thenReturn("\"ok\"");
		sikkerhetsnivaaConsumer.ping();
		verify(restTemplate).getForObject("isReady", String.class);
	}
}
