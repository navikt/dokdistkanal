package no.nav.dokdistkanal.consumer.sikkerhetsnivaa;

import no.nav.dokdistkanal.exceptions.DokDistKanalFunctionalException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
class SikkerhetsnivaaFunctionalException extends DokDistKanalFunctionalException {
	SikkerhetsnivaaFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
