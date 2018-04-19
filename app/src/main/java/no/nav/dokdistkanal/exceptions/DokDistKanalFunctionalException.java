package no.nav.dokdistkanal.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class DokDistKanalFunctionalException extends Exception {
	
	public DokDistKanalFunctionalException() {
	}
	
	public DokDistKanalFunctionalException(String message) {
		super(message);
	}
	
	public DokDistKanalFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public DokDistKanalFunctionalException(Throwable cause) {
		super(cause);
	}
}
