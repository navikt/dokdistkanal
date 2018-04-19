package no.nav.dokdistkanal.consumer.sikkerhetsnivaa;

import no.nav.dokdistkanal.exceptions.DokKanalvalgFunctionalException;

public class SikkerhetsnivaaFunctionalException extends DokKanalvalgFunctionalException {
	public SikkerhetsnivaaFunctionalException(String message, Throwable cause) {
		super(message, cause);
	}
}
