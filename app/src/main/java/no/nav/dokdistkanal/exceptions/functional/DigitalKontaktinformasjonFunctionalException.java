package no.nav.dokdistkanal.exceptions.functional;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResponseStatus(value = BAD_REQUEST)
public class DigitalKontaktinformasjonFunctionalException extends DokdistkanalFunctionalException {
	public DigitalKontaktinformasjonFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}

	public DigitalKontaktinformasjonFunctionalException(String message) {
		super(message);
	}
}