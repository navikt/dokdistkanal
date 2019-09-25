package no.nav.dokdistkanal.exceptions.functional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Joakim Bjørnstad, Jbit AS
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class UgyldingInputFunctionalException extends DokDistKanalFunctionalException {
	public UgyldingInputFunctionalException(String message) {
		super(message);
	}
}
