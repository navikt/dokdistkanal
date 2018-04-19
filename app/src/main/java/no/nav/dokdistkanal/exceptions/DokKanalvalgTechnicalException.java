package no.nav.dokdistkanal.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class DokKanalvalgTechnicalException extends RuntimeException{
	public DokKanalvalgTechnicalException() {
	}
	
	public DokKanalvalgTechnicalException(String message) {
		super(message);
	}
	
	public DokKanalvalgTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public DokKanalvalgTechnicalException(Throwable cause) {
		super(cause);
	}
}
