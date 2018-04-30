package no.nav.dokdistkanal.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
@Getter
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class DokDistKanalFunctionalException extends Exception {

	private final String shortDescription = "DokDistKanalFunctionalException";

	public DokDistKanalFunctionalException() {}
	
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
