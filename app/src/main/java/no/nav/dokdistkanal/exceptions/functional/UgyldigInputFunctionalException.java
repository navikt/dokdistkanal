package no.nav.dokdistkanal.exceptions.functional;

import org.springframework.web.bind.annotation.ResponseStatus;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@ResponseStatus(value = BAD_REQUEST)
public class UgyldigInputFunctionalException extends DokDistKanalFunctionalException {
	public UgyldigInputFunctionalException(String message) {
		super(message);
	}
}
