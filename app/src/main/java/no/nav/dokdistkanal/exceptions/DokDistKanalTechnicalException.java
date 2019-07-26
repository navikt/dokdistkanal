package no.nav.dokdistkanal.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Jarl Øystein Samseth, Visma Consulting
 */
public abstract class DokDistKanalTechnicalException extends RuntimeException {
	public DokDistKanalTechnicalException(String message) {
		super(message);
	}

	public DokDistKanalTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
}
