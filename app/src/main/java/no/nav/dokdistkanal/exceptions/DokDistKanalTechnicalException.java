package no.nav.dokdistkanal.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class DokDistKanalTechnicalException extends RuntimeException{
	public DokDistKanalTechnicalException() {
	}
	
	public DokDistKanalTechnicalException(String message) {
		super(message);
	}
	
	public DokDistKanalTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public DokDistKanalTechnicalException(Throwable cause) {
		super(cause);
	}
}
