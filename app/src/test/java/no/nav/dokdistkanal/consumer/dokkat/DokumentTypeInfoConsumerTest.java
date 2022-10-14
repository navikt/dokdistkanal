package no.nav.dokdistkanal.consumer.dokkat;

import no.nav.dokdistkanal.azure.TokenConsumer;
import no.nav.dokdistkanal.azure.TokenResponse;
import no.nav.dokdistkanal.consumer.dokkat.to.DistribusjonInfoTo;
import no.nav.dokdistkanal.consumer.dokkat.to.DokumentProduksjonsInfoToV4;
import no.nav.dokdistkanal.consumer.dokkat.to.DokumentTypeInfoToV4;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import no.nav.dokdistkanal.exceptions.functional.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.functional.DokkatFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.DokkatTechnicalException;
import no.nav.dokdistkanal.metrics.MicrometerMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static no.nav.dokdistkanal.common.DistribusjonKanalCode.LOKAL_PRINT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DokumentTypeInfoConsumerTest {
	private static final String DOKTYPE = "12345678910";
	private static final String ARKIVSYSTEM = "JOARK";

	private RestTemplate restTemplate;
	private DokumentTypeInfoConsumer dokumentTypeInfoConsumer;
	private MicrometerMetrics metrics;
	private TokenConsumer tokenConsumer;

	@BeforeEach
	public void setUp() {
		restTemplate = mock(RestTemplate.class);
		metrics = mock(MicrometerMetrics.class);
		tokenConsumer = mock(TokenConsumer.class);
		dokumentTypeInfoConsumer = new DokumentTypeInfoConsumer(restTemplate, metrics, tokenConsumer);

		when(tokenConsumer.getClientCredentialToken(any(String.class)))
				.thenReturn(getTokenResponse());
	}

	@Test
	public void shouldRunOK() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
		DokumentTypeInfoToV4 response = createResponse();
		response.getDokumentProduksjonsInfo().setDistribusjonInfo(null);
		ResponseEntity<DokumentTypeInfoToV4> responseEntity = new ResponseEntity<>(response,HttpStatus.ACCEPTED);

		when(restTemplate.exchange(any(String.class),any(HttpMethod.class),any(HttpEntity.class), eq(DokumentTypeInfoToV4.class)))
				.thenReturn(responseEntity);

		DokumentTypeInfoTo dokumentTypeInfoTo = dokumentTypeInfoConsumer.hentDokumenttypeInfo(DOKTYPE);
		assertThat(dokumentTypeInfoTo.getArkivsystem(), equalTo(ARKIVSYSTEM));
		assertThat(dokumentTypeInfoTo.isVarslingSdp(), equalTo(Boolean.FALSE));
	}


	@Test
	public void shouldRunOKDistKanalLokalPrint() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
		when(restTemplate.exchange(any(String.class),any(HttpMethod.class),any(HttpEntity.class), eq(DokumentTypeInfoToV4.class)))
				.thenReturn(createResponseEntity());

		DokumentTypeInfoTo dokumentTypeInfoTo = dokumentTypeInfoConsumer.hentDokumenttypeInfo(DOKTYPE);
		assertThat(dokumentTypeInfoTo.getArkivsystem(), equalTo(ARKIVSYSTEM));
		assertThat(dokumentTypeInfoTo.isVarslingSdp(), equalTo(Boolean.FALSE));
		assertThat(dokumentTypeInfoTo.getPredefinertDistKanal(), equalTo(LOKAL_PRINT.name()));
	}

	@Test
	public void shouldThrowFunctionalExceptionWhenBadRequest() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
		when(restTemplate.exchange(any(String.class),any(HttpMethod.class),any(HttpEntity.class), eq(DokumentTypeInfoToV4.class)))
				.thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));
		assertThrows(DokkatFunctionalException.class, ()-> dokumentTypeInfoConsumer.hentDokumenttypeInfo(DOKTYPE),
				"DokumentTypeInfoConsumer feilet. (HttpStatus=400 BAD_REQUEST) for dokumenttypeId");


	}

	@Test
	public void shouldThrowTechnicalExceptionWhenServerException() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
		when(restTemplate.exchange(any(String.class),any(HttpMethod.class),any(HttpEntity.class), eq(DokumentTypeInfoToV4.class)))
				.thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));
		assertThrows(DokkatTechnicalException.class, ()-> dokumentTypeInfoConsumer.hentDokumenttypeInfo(DOKTYPE),
				"DokumentTypeInfoConsumer feilet med statusCode=503");

	}

	@Test
	public void shouldThrowSecurityExceptionWhenUnauthorized() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
		when(restTemplate.exchange(any(String.class),any(HttpMethod.class),any(HttpEntity.class), eq(DokumentTypeInfoToV4.class)))
				.thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));
		assertThrows(DokDistKanalSecurityException.class, ()-> dokumentTypeInfoConsumer.hentDokumenttypeInfo(DOKTYPE),
				"DokumentTypeInfoConsumer feilet (HttpStatus=401 UNAUTHORIZED) for dokumenttypeId:" + DOKTYPE);

	}


	@Test
	public void shouldThrowTechnicalExceptionWhenRuntimeException() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
		when(restTemplate.exchange(any(String.class),any(HttpMethod.class),any(HttpEntity.class), eq(DokumentTypeInfoToV4.class)))
				.thenThrow(new RuntimeException());

		assertThrows(DokkatTechnicalException.class,
				()-> dokumentTypeInfoConsumer.hentDokumenttypeInfo(DOKTYPE),"DokumentTypeInfoConsumer feilet med message");

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

	private ResponseEntity<DokumentTypeInfoToV4> createResponseEntity(){
		return new ResponseEntity<>(createResponse(),HttpStatus.ACCEPTED);
	}

	private TokenResponse getTokenResponse() {
		return TokenResponse.builder()
				.access_token("abc")
				.build();
	}
}
