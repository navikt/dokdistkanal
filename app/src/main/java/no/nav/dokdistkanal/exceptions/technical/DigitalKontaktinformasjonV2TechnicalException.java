package no.nav.dokdistkanal.exceptions.technical;

import no.nav.dokdistkanal.exceptions.functional.DokDistKanalFunctionalException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Heidi Elisabeth Sando, Visma Consulting
 */
@ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
public class DigitalKontaktinformasjonV2TechnicalException extends DokDistKanalFunctionalException {
	public DigitalKontaktinformasjonV2TechnicalException(String message, Throwable cause) {
		super(message, cause);
	}

	public DigitalKontaktinformasjonV2TechnicalException(String message) {
		super(message);
	}
}