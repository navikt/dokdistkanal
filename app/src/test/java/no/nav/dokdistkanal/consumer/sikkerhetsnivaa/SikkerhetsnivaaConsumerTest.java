package no.nav.dokdistkanal.consumer.sikkerhetsnivaa;

import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.schema.SikkerhetsnivaaRequest;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.schema.SikkerhetsnivaaResponse;
import no.nav.dokdistkanal.consumer.sikkerhetsnivaa.to.SikkerhetsnivaaTo;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import no.nav.dokdistkanal.exceptions.functional.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.functional.SikkerhetsnivaaFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.SikkerhetsnivaaTechnicalException;
import no.nav.dokdistkanal.metrics.MicrometerMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class SikkerhetsnivaaConsumerTest {

    private static final String FNR = "12345678910";

    private RestTemplate restTemplate;
    private SikkerhetsnivaaConsumer sikkerhetsnivaaConsumer;
    private MicrometerMetrics metrics;

    @BeforeEach
    public void setUp() {
        restTemplate = mock(RestTemplate.class);
        metrics = mock(MicrometerMetrics.class);
        sikkerhetsnivaaConsumer = new SikkerhetsnivaaConsumer(restTemplate, metrics);
    }

    @Test
    public void shouldRunOK() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
        SikkerhetsnivaaResponse response = new SikkerhetsnivaaResponse();
        response.setPersonidentifikator(FNR);
        response.setHarbruktnivaa4(Boolean.FALSE);
        when(restTemplate.postForObject(any(String.class), any(SikkerhetsnivaaRequest.class), eq(SikkerhetsnivaaResponse.class)))
                .thenReturn(response);
        SikkerhetsnivaaTo sikkerhetsnivaaTo = sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(FNR);
        assertThat(sikkerhetsnivaaTo.isHarLoggetPaaNivaa4(), equalTo(Boolean.FALSE));
        assertThat(sikkerhetsnivaaTo.getPersonIdent(), equalTo(FNR));
    }

    @Test
    public void shouldReturnFalseWhenSikkerhetsnivaaNotFound() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
        when(restTemplate.postForObject(any(String.class), any(SikkerhetsnivaaRequest.class), eq(SikkerhetsnivaaResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));
        SikkerhetsnivaaTo sikkerhetsnivaaTo = sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(FNR);
        assertThat(sikkerhetsnivaaTo.isHarLoggetPaaNivaa4(), equalTo(Boolean.FALSE));
        assertThat(sikkerhetsnivaaTo.getPersonIdent(), equalTo(FNR));
    }

    @Test
    public void shouldThrowFunctionalExceptionWhenHttpStatusBadRequest() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
        when(restTemplate.postForObject(any(String.class), any(SikkerhetsnivaaRequest.class), eq(SikkerhetsnivaaResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
        assertThrows(SikkerhetsnivaaFunctionalException.class, () -> sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(FNR), "Sikkerhetsnivaa.hentPaaloggingsnivaa feilet (HttpStatus=400 BAD_REQUEST)");

    }


    @Test
    public void shouldThrowTechnicalExceptionWhenRuntimeException() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
        when(restTemplate.postForObject(any(String.class), any(SikkerhetsnivaaRequest.class), eq(SikkerhetsnivaaResponse.class)))
                .thenThrow(new RuntimeException());
        assertThrows(SikkerhetsnivaaTechnicalException.class, () -> sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(FNR),
                "Sikkerhetsnivaa.hentPaaloggingsnivaa feilet");


    }

    @Test
    public void shouldThrowSecurityExceptionWhenHttpStatusForbidden() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
		when(restTemplate.postForObject(any(String.class), any(SikkerhetsnivaaRequest.class), eq(SikkerhetsnivaaResponse.class)))
				.thenThrow(new HttpClientErrorException(HttpStatus.FORBIDDEN));
		assertThrows(DokDistKanalSecurityException.class, () -> sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(FNR),
				"Sikkerhetsnivaa.hentPaaloggingsnivaa feilet (HttpStatus=403 FORBIDDEN)");


    }

    @Test
    public void shouldThrowTechnicalExceptionWhenHttpStatusInternalServerError() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
        when(restTemplate.postForObject(any(String.class), any(SikkerhetsnivaaRequest.class), eq(SikkerhetsnivaaResponse.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

		assertThrows(SikkerhetsnivaaTechnicalException.class, () -> sikkerhetsnivaaConsumer.hentPaaloggingsnivaa(FNR),
				"Sikkerhetsnivaa.hentPaaloggingsnivaa feilet (HttpStatus=500 INTERNAL_SERVER_ERROR)");

    }
}
