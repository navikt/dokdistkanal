package no.nav.dokdistkanal.config;

import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import no.nav.dokdistkanal.exceptions.functional.AltinnServiceOwnerFunctionalException;
import no.nav.dokdistkanal.exceptions.functional.CouldNotDecodeBasicAuthToken;
import no.nav.dokdistkanal.exceptions.functional.DigitalKontaktinformasjonV2FunctionalException;
import no.nav.dokdistkanal.exceptions.functional.DokmetFunctionalException;
import no.nav.dokdistkanal.exceptions.functional.MaskinportenFunctionalException;
import no.nav.dokdistkanal.exceptions.functional.PdlFunctionalException;
import no.nav.dokdistkanal.exceptions.functional.UgyldigInputFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.DokDistKanalTechnicalException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ControllerAdvice(basePackages = "no.nav.dokdistkanal.rest.bestemkanal")
public class DokdistkanalRestExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler({AltinnServiceOwnerFunctionalException.class,
			DokmetFunctionalException.class, DigitalKontaktinformasjonV2FunctionalException.class,
			MaskinportenFunctionalException.class, PdlFunctionalException.class,
			UgyldigInputFunctionalException.class})
	public ResponseEntity<Object> handleBadRequestException(Exception err) {
		Map<String, Object> responseBody = new HashMap<>();
		logger.warn("Feilet funksjonell med feilmelding=" + err.getMessage(), err);
		responseBody.put("message", err.getMessage());

		if (err instanceof HttpClientErrorException) {
			HttpClientErrorException restErr = (HttpClientErrorException) err;
			responseBody.put("status", restErr.getStatusCode());
			return new ResponseEntity<>(responseBody, restErr.getStatusCode());
		}
		responseBody.put("status", BAD_REQUEST);
		return new ResponseEntity<>(responseBody, BAD_REQUEST);
	}

	@ExceptionHandler({DokDistKanalSecurityException.class, CouldNotDecodeBasicAuthToken.class})
	public ResponseEntity<Object> handleUnauthorizedException(Exception e) {
		Map<String, Object> responseBody = new HashMap<>();
		logger.error("Feilet teknisk med feilmelding=" + e.getMessage(), e);
		responseBody.put("message", e.getMessage());
		responseBody.put("status", UNAUTHORIZED);
		return new ResponseEntity<>(responseBody, UNAUTHORIZED);
	}

	@ExceptionHandler({DokDistKanalTechnicalException.class, Exception.class})
	public ResponseEntity<Object> handleTechnicalException(Exception e) {
		Map<String, Object> responseBody = new HashMap<>();
		logger.error("Feilet teknisk med feilmelding=" + e.getMessage(), e);
		responseBody.put("message", e.getMessage());
		return new ResponseEntity<>(responseBody, INTERNAL_SERVER_ERROR);
	}
}
