package no.nav.dokdistkanal.consumer.sikkerhetsnivaa;

import no.nav.dokdistkanal.exceptions.DokDistKanalFunctionalException;

public class SikkerhetsnivaaFunctionalException extends DokDistKanalFunctionalException {
	public SikkerhetsnivaaFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
