package no.nav.dokdistkanal.consumer.dokkat;

import no.nav.dokdistkanal.exceptions.DokDistKanalTechnicalException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
class DokkatTechnicalException extends DokDistKanalTechnicalException {
	DokkatTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}
}
