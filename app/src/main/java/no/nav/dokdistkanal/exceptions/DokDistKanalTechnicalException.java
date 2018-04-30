package no.nav.dokdistkanal.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
@Getter
@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class DokDistKanalTechnicalException extends RuntimeException{
	private final String shortDescription = "DokDistKanalTechnicalException";

	public DokDistKanalTechnicalException(String message) {
		super(message);
	}

	public DokDistKanalTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
	
}
