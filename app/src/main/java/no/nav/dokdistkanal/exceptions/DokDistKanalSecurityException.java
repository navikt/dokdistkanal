package no.nav.dokdistkanal.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
@Getter
@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
public class DokDistKanalSecurityException extends Exception {

	private String shortDescription = "DokDistKanalSecurityException";

	public DokDistKanalSecurityException() {
		super();
	}

	public DokDistKanalSecurityException(Throwable cause) {
		super(cause);
	}

	public DokDistKanalSecurityException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
