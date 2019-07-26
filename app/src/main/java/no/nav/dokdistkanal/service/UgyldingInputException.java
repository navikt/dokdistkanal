package no.nav.dokdistkanal.service;

import no.nav.dokdistkanal.exceptions.DokDistKanalFunctionalException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
class UgyldingInputException extends DokDistKanalFunctionalException {
	UgyldingInputException(String message) {
		super(message);
	}
}
