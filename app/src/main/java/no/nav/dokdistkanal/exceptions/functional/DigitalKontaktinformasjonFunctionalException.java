package no.nav.dokdistkanal.exceptions.functional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class DigitalKontaktinformasjonFunctionalException extends DokdistkanalFunctionalException {
	public DigitalKontaktinformasjonFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}

	public DigitalKontaktinformasjonFunctionalException(String message) {
		super(message);
	}
}