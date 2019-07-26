package no.nav.dokdistkanal.consumer.dokkat;

import no.nav.dokdistkanal.exceptions.DokDistKanalFunctionalException;
import no.nav.dokdistkanal.exceptions.DokDistKanalTechnicalException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
class DokkatFunctionalException extends DokDistKanalFunctionalException {
	DokkatFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
