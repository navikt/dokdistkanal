package no.nav.dokdistkanal.exceptions.technical;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@ResponseStatus(value = SERVICE_UNAVAILABLE)
public class DokkatTechnicalException extends DokDistKanalTechnicalException {
	public DokkatTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
}
