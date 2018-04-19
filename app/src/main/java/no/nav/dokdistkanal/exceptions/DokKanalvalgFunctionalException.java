package no.nav.dokdistkanal.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class DokKanalvalgFunctionalException extends Exception {
	
	public DokKanalvalgFunctionalException() {
	}
	
	public DokKanalvalgFunctionalException(String message) {
		super(message);
	}
	
	public DokKanalvalgFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
	
	public DokKanalvalgFunctionalException(Throwable cause) {
		super(cause);
	}
}
