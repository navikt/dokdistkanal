package no.nav.dokdistkanal.exceptions.technical;

import no.nav.dokdistkanal.exceptions.functional.DokdistkanalFunctionalException;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE;

@ResponseStatus(value = SERVICE_UNAVAILABLE)
public class DigitalKontaktinformasjonTechnicalException extends DokdistkanalFunctionalException {

	public DigitalKontaktinformasjonTechnicalException(String message, Throwable cause) {
		super(message, cause);
	}

	public DigitalKontaktinformasjonTechnicalException(String message) {
		super(message);
	}
}