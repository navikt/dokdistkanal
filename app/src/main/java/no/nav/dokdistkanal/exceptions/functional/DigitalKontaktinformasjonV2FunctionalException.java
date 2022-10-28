package no.nav.dokdistkanal.exceptions.functional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class DigitalKontaktinformasjonV2FunctionalException extends DokDistKanalFunctionalException {
	public DigitalKontaktinformasjonV2FunctionalException(String message, Throwable cause) {
		super(message, cause);
	}

	public DigitalKontaktinformasjonV2FunctionalException(String message) {
		super(message);
	}
}