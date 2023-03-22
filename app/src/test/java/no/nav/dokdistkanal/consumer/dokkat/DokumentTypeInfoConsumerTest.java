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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static java.lang.Boolean.FALSE;
import static no.nav.dokdistkanal.common.DistribusjonKanalCode.LOKAL_PRINT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ExtendWith(MockitoExtension.class)
public class DokumentTypeInfoConsumerTest {
	private static final String DOKTYPE = "12345678910";
	private static final String ARKIVSYSTEM = "JOARK";

	private RestTemplate restTemplate;
	private DokumentTypeInfoConsumer dokumentTypeInfoConsumer;

	@BeforeEach
	public void setUp() {
		restTemplate = mock(RestTemplate.class);
		TokenConsumer tokenConsumer = mock(TokenConsumer.class);
		dokumentTypeInfoConsumer = new DokumentTypeInfoConsumer(restTemplate, tokenConsumer);

		when(tokenConsumer.getClientCredentialToken(any(String.class)))
				.thenReturn(getTokenResponse());
	}

	@Test
	public void shouldRunOK() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
		DokumentTypeInfoToV4 response = createResponse();
		response.getDokumentProduksjonsInfo().setDistribusjonInfo(null);
		ResponseEntity<DokumentTypeInfoToV4> responseEntity = new ResponseEntity<>(response, ACCEPTED);

		when(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class), eq(DokumentTypeInfoToV4.class)))
				.thenReturn(responseEntity);

		DokumentTypeInfoTo dokumentTypeInfoTo = dokumentTypeInfoConsumer.hentDokumenttypeInfo(DOKTYPE);
		assertThat(dokumentTypeInfoTo.getArkivsystem(), equalTo(ARKIVSYSTEM));
		assertThat(dokumentTypeInfoTo.isVarslingSdp(), equalTo(FALSE));
	}

	@Test
	public void shouldRunOKDistKanalLokalPrint() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
		when(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class), eq(DokumentTypeInfoToV4.class)))
				.thenReturn(createResponseEntity());

		DokumentTypeInfoTo dokumentTypeInfoTo = dokumentTypeInfoConsumer.hentDokumenttypeInfo(DOKTYPE);
		assertThat(dokumentTypeInfoTo.getArkivsystem(), equalTo(ARKIVSYSTEM));
		assertThat(dokumentTypeInfoTo.isVarslingSdp(), equalTo(FALSE));
		assertThat(dokumentTypeInfoTo.getPredefinertDistKanal(), equalTo(LOKAL_PRINT.name()));
	}

	@Test
	public void shouldThrowFunctionalExceptionWhenBadRequest() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
		when(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class), eq(DokumentTypeInfoToV4.class)))
				.thenThrow(new HttpClientErrorException(BAD_REQUEST));
		assertThrows(DokkatFunctionalException.class, () -> dokumentTypeInfoConsumer.hentDokumenttypeInfo(DOKTYPE),
				"DokumentTypeInfoConsumer feilet. (HttpStatus=400 BAD_REQUEST) for dokumenttypeId");
	}

	@Test
	public void shouldThrowTechnicalExceptionWhenServerException() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
		when(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class), eq(DokumentTypeInfoToV4.class)))
				.thenThrow(new HttpServerErrorException(SERVICE_UNAVAILABLE));
		assertThrows(DokkatTechnicalException.class, () -> dokumentTypeInfoConsumer.hentDokumenttypeInfo(DOKTYPE),
				"DokumentTypeInfoConsumer feilet med statusCode=503");

	}

	@Test
	public void shouldThrowSecurityExceptionWhenUnauthorized() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
		when(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class), eq(DokumentTypeInfoToV4.class)))
				.thenThrow(new HttpClientErrorException(UNAUTHORIZED));
		assertThrows(DokDistKanalSecurityException.class, () -> dokumentTypeInfoConsumer.hentDokumenttypeInfo(DOKTYPE),
				"DokumentTypeInfoConsumer feilet (HttpStatus=401 UNAUTHORIZED) for dokumenttypeId:" + DOKTYPE);

	}


	@Test
	public void shouldThrowTechnicalExceptionWhenRuntimeException() throws DokDistKanalSecurityException, DokDistKanalFunctionalException {
		when(restTemplate.exchange(any(String.class), any(HttpMethod.class), any(HttpEntity.class), eq(DokumentTypeInfoToV4.class)))
				.thenThrow(new RuntimeException());

		assertThrows(DokkatTechnicalException.class,
				() -> dokumentTypeInfoConsumer.hentDokumenttypeInfo(DOKTYPE), "DokumentTypeInfoConsumer feilet med message");

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

	private ResponseEntity<DokumentTypeInfoToV4> createResponseEntity() {
		return new ResponseEntity<>(createResponse(), ACCEPTED);
	}

	private TokenResponse getTokenResponse() {
		return TokenResponse.builder()
				.access_token("abc")
				.build();
	}
}
