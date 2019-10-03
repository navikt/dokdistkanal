package no.nav.dokdistkanal.exceptions.technical;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
public class SikkerhetsnivaaTechnicalException extends DokDistKanalTechnicalException {
	public SikkerhetsnivaaTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
}
