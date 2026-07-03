package no.nav.dokdistkanal.exceptions.functional;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.GONE)
public class EnhetSlettetException extends DokdistkanalFunctionalException {
	public EnhetSlettetException(String message) {
		super(message);
	}
}
