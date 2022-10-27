package no.nav.dokdistkanal.exceptions;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@ResponseStatus(value = UNAUTHORIZED)
public class DokDistKanalSecurityException extends RuntimeException {

	public DokDistKanalSecurityException(String message, Throwable cause) {
		super(message, cause);
	}
}
