package no.nav.dokdistkanal.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
public class DokDistKanalSecurityException extends RuntimeException {
	public DokDistKanalSecurityException(Throwable cause) {
		super(cause);
	}

	public DokDistKanalSecurityException(String message, Throwable cause) {
		super(message, cause);
	}
}
