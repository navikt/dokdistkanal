package no.nav.dokdistkanal.exceptions.functional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * @author Heidi Elisabeth Sando, Visma Consulting
 */
@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class TpsHentNavnFunctionalException extends DokDistKanalFunctionalException {
	public TpsHentNavnFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}