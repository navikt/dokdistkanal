package no.nav.dokdistkanal.config;

import no.nav.dokdistkanal.azure.AzureTokenException;
import no.nav.dokdistkanal.exceptions.DokDistKanalSecurityException;
import no.nav.dokdistkanal.exceptions.functional.AltinnServiceOwnerFunctionalException;
import no.nav.dokdistkanal.exceptions.functional.CouldNotDecodeBasicAuthToken;
import no.nav.dokdistkanal.exceptions.functional.DigitalKontaktinformasjonV2FunctionalException;
import no.nav.dokdistkanal.exceptions.functional.DokkatFunctionalException;
import no.nav.dokdistkanal.exceptions.functional.MaskinportenFunctionalException;
import no.nav.dokdistkanal.exceptions.functional.PdlFunctionalException;
import no.nav.dokdistkanal.exceptions.functional.SikkerhetsnivaaFunctionalException;
import no.nav.dokdistkanal.exceptions.functional.UgyldigInputFunctionalException;
import no.nav.dokdistkanal.exceptions.technical.DokDistKanalTechnicalException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ControllerAdvice
public class RestInvalidExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler({AltinnServiceOwnerFunctionalException.class, CouldNotDecodeBasicAuthToken.class,
			DokkatFunctionalException.class, DigitalKontaktinformasjonV2FunctionalException.class,
			MaskinportenFunctionalException.class, PdlFunctionalException.class,
			SikkerhetsnivaaFunctionalException.class, UgyldigInputFunctionalException.class, AzureTokenException.class})
	public ResponseEntity<Object> handleBadRequestException(Exception e) {
		Map<String, Object> responseBody = new HashMap<>();
		logger.warn("Feilet funksjonell med feilmelding=" + e.getMessage(), e);
		responseBody.put("message", e.getMessage());
		responseBody.put("error", e);
		if (e.getMessage().contains(NOT_FOUND.toString())) {
			responseBody.put("status", NOT_FOUND);
			return new ResponseEntity<>(responseBody, NOT_FOUND);
		}
		if (e.getMessage().contains(UNAUTHORIZED.toString())) {
			responseBody.put("status", UNAUTHORIZED);
			return new ResponseEntity<>(responseBody, UNAUTHORIZED);
		}
		if (e.getMessage().contains(FORBIDDEN.toString())) {
			responseBody.put("status", FORBIDDEN);
			return new ResponseEntity<>(responseBody, FORBIDDEN);
		}
		responseBody.put("status", BAD_REQUEST);
		return new ResponseEntity<>(responseBody, BAD_REQUEST);
	}

	@ExceptionHandler({DokDistKanalTechnicalException.class, Exception.class,
			DokDistKanalSecurityException.class
	})
	public ResponseEntity<Object> handleTechnicalException(Exception e) {
		Map<String, Object> responseBody = new HashMap<>();
		logger.error("Feilet teknisk med feilmelding=" + e.getMessage(), e);
		responseBody.put("message", e.getMessage());
		responseBody.put("error", e);
		return new ResponseEntity<>(responseBody, INTERNAL_SERVER_ERROR);
	}
}
