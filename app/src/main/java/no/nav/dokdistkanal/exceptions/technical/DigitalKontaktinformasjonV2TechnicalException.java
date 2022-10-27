package no.nav.dokdistkanal.exceptions.technical;

import no.nav.dokdistkanal.exceptions.functional.DokDistKanalFunctionalException;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;


@ResponseStatus(value = SERVICE_UNAVAILABLE)
public class DigitalKontaktinformasjonV2TechnicalException extends DokDistKanalFunctionalException {
	public DigitalKontaktinformasjonV2TechnicalException(String message, Throwable cause) {
		super(message, cause);
	}

	public DigitalKontaktinformasjonV2TechnicalException(String message) {
		super(message);
	}
}