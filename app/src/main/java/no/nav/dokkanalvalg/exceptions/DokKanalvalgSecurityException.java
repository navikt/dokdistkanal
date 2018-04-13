package no.nav.dokkanalvalg.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
@ResponseStatus(value = HttpStatus.UNAUTHORIZED)
public class DokKanalvalgSecurityException extends Exception {
	
	public DokKanalvalgSecurityException(String message) {
		super(message);
	}
	
	public DokKanalvalgSecurityException(Throwable cause) {
		super(cause);
	}
	
	public DokKanalvalgSecurityException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
